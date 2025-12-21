package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    @ModifyVariable(method = "findNearestMapStructure", at = @At("HEAD"), argsOnly = true)
    private int findNearestMapStructure(int original, @Local(argsOnly = true) ServerLevel level) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
        if (limit != null && limit.bounds().size() == 1) {
            LegacyChunkBounds bounds = limit.bounds().get(0);
            return Math.min(original, Math.max(bounds.max().x - bounds.min().x, bounds.max().z - bounds.min().z) / 6);
        }
        return original;
    }
}
