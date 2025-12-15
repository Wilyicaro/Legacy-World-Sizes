package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.Util;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTask;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.level.FakeLevelChunk;
import wily.legacy_world_sizes.level.FakeProtoChunk;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStep.class)
public class ChunkStepMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatusTask;doWork(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/world/level/chunk/status/ChunkStep;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<ChunkAccess> apply(ChunkStatusTask instance, WorldGenContext context, ChunkStep chunkStep, StaticCache2D<GenerationChunkHolder> generationChunkHolderStaticCache2D, ChunkAccess chunkAccess, Operation<CompletableFuture<ChunkAccess>> original) {
        boolean validStep = false;

        if (chunkAccess instanceof FakeProtoChunk fakeChunk) {
            LegacyLevelLimit limit;
            if (chunkStep.targetStatus() == ChunkStatus.BIOMES && (limit = LWSWorldOptions.legacyLevelLimits.get().get(context.level().dimension())) != null && limit.fixedBiome().isEmpty()) {
                return CompletableFuture.supplyAsync(() -> {
                    fakeChunk.fillBiomesFromNoise(context.generator().getBiomeSource(), context.level().getChunkSource().randomState().sampler());
                    return fakeChunk;
                }, Util.backgroundExecutor().forName("init_biomes"));
            } else if (chunkStep.targetStatus() == ChunkStatus.INITIALIZE_LIGHT) {
                validStep = true;
            } else if (chunkStep.targetStatus() == ChunkStatus.LIGHT) {
                validStep = true;
            } else if (chunkStep.targetStatus() == ChunkStatus.FULL) {
                validStep = true;
            } else {
                return CompletableFuture.completedFuture(fakeChunk);
            }
        }

        if (!validStep && !LWSWorldOptions.isValidPos(context.level().dimension(), chunkAccess.getPos())) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(context.level().dimension());
            ProtoChunk protoChunk = FakeLevelChunk.ContentType.CACHE.computeIfAbsent(limit.content(), content -> content.fill(context.level(), new ProtoChunk(ChunkPos.ZERO, UpgradeData.EMPTY, context.level(), context.level().palettedContainerFactory(), null)));
            FakeLevelChunk fakeChunk = new FakeLevelChunk(context.level(), chunkAccess.getPos(), Arrays.stream(protoChunk.getSections()).map(LevelChunkSection::copy).toArray(LevelChunkSection[]::new), limit.heightFallOff(), limit.fixedBiome().orElse(null));

            for (Map.Entry<Heightmap.Types, Heightmap> entry : protoChunk.getHeightmaps()) {
                if (ChunkStatus.SURFACE.heightmapsAfter().contains(entry.getKey())) {
                    fakeChunk.setHeightmap(entry.getKey(), entry.getValue().getRawData().clone());
                }
            }

            return CompletableFuture.completedFuture(new FakeProtoChunk(fakeChunk));
        }

        return original.call(instance, context, chunkStep, generationChunkHolderStaticCache2D, chunkAccess);
    }
}
