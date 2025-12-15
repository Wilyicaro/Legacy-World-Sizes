package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {
    @ModifyReturnValue(method = "generateFeatures", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> generateFeatures(CompletableFuture<ChunkAccess> original, WorldGenContext worldGenContext, ChunkStep chunkStep, StaticCache2D<GenerationChunkHolder> staticCache2D, ChunkAccess chunkAccess) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(worldGenContext.level().dimension());
        if (limit != null && limit.bedrockBarrier()) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                if (bounds.isInsideBorder(chunkAccess.getPos().x, chunkAccess.getPos().z)) {
                    return original.thenApply(access -> {
                        bounds.generateBedrockWalls(chunkAccess, worldGenContext.generator(), worldGenContext.level().getChunkSource().randomState());
                        return chunkAccess;
                    });
                }
            }
        }
        return original;
    }
}
