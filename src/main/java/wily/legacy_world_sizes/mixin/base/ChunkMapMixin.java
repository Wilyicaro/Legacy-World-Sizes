package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.Util;
import net.minecraft.server.level.*;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.level.FakeLevelChunk;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Unique
    private final Map<FakeLevelChunk.ContentType, ProtoChunk> contentChunks = new HashMap<>();

    @Shadow @Final
    ServerLevel level;

    @Shadow @Final private ThreadedLevelLightEngine lightEngine;

    @Shadow protected abstract RandomState randomState();

    @Shadow protected abstract ChunkGenerator generator();

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Inject(method = "applyStep", at = @At("HEAD"), cancellable = true)
    private void applyStep(GenerationChunkHolder generationChunkHolder, ChunkStep chunkStep, StaticCache2D<GenerationChunkHolder> staticCache2D, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (generationChunkHolder.getLatestChunk() instanceof FakeLevelChunk fakeChunk) {
            LegacyLevelLimit limit;
            if (chunkStep.targetStatus() == ChunkStatus.BIOMES && (limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension())) != null && limit.fixedBiome().isEmpty()) {
                cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                    fakeChunk.fillBiomesFromNoise(generator().getBiomeSource(), randomState().sampler());
                    return fakeChunk;
                }, Util.backgroundExecutor().forName("init_biomes")));
            } else if (chunkStep.targetStatus() == ChunkStatus.INITIALIZE_LIGHT) {
                fakeChunk.initializeLightSources();
                cir.setReturnValue(lightEngine.initializeLight(fakeChunk, false));
            } else if (chunkStep.targetStatus() == ChunkStatus.LIGHT) {
                cir.setReturnValue(lightEngine.lightChunk(fakeChunk, false));
            } else if (chunkStep.targetStatus() == ChunkStatus.FULL) {
                cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                    fakeChunk.registerTickContainerInLevel(level);
                    return fakeChunk;
                }, mainThreadExecutor));
            } else {
                cir.setReturnValue(CompletableFuture.completedFuture(fakeChunk));
            }
            return;
        }

        if (!LWSWorldOptions.isValidPos(level.dimension(), generationChunkHolder.getPos())) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
            ProtoChunk protoChunk = contentChunks.computeIfAbsent(limit.content(), content -> FakeLevelChunk.fillChunk(level, new ProtoChunk(ChunkPos.ZERO, UpgradeData.EMPTY, level, level.palettedContainerFactory(), null), content));
            FakeLevelChunk fakeChunk = new FakeLevelChunk(level, generationChunkHolder.getPos(), Arrays.stream(protoChunk.getSections()).map(LevelChunkSection::copy).toArray(LevelChunkSection[]::new), limit.heightFallOff(), limit.fixedBiome().orElse(null));

            for (Map.Entry<Heightmap.Types, Heightmap> entry : protoChunk.getHeightmaps()) {
                if (ChunkStatus.SURFACE.heightmapsAfter().contains(entry.getKey())) {
                    fakeChunk.setHeightmap(entry.getKey(), entry.getValue().getRawData().clone());
                }
            }

            cir.setReturnValue(CompletableFuture.completedFuture(fakeChunk));
        }
    }

    @WrapOperation(method = "applyStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStep;apply(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<ChunkAccess> applyBedrockBarrierStep(ChunkStep instance, WorldGenContext worldGenContext, StaticCache2D<GenerationChunkHolder> staticCache2D, ChunkAccess chunkAccess, Operation<CompletableFuture<ChunkAccess>> original) {
        if (instance.targetStatus() == ChunkStatus.FEATURES) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
            if (limit != null && limit.bedrockBarrier()) {
                for (LegacyLevelLimit.ChunkBounds bounds : limit.bounds()) {
                    if (bounds.isInsideBorder(chunkAccess.getPos().x, chunkAccess.getPos().z)) {
                        return original.call(instance, worldGenContext, staticCache2D, chunkAccess).thenApply(access -> {
                            bounds.generateBedrockWalls(chunkAccess, worldGenContext.generator(), randomState());
                            return chunkAccess;
                        });
                    }
                }
            }
        }
        return original.call(instance, worldGenContext, staticCache2D, chunkAccess);
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void save(ChunkAccess chunkAccess, CallbackInfoReturnable<Boolean> cir) {
        if (!LWSWorldOptions.isValidPos(level.dimension(), chunkAccess.getPos())) cir.setReturnValue(false);
    }
}
