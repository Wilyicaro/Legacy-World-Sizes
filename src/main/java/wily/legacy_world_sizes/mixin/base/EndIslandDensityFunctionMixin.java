package wily.legacy_world_sizes.mixin.base;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(targets = {"net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction"})
public class EndIslandDensityFunctionMixin {

    @Inject(method = "getHeightValue", at = @At("HEAD"), cancellable = true)
    private static void getHeightValue(SimplexNoise simplexNoise, int i, int j, CallbackInfoReturnable<Float> cir) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);

        if (limit != null) {
            for (LegacyChunkBounds bound : limit.bounds()) {
                if (bound.isInside(i / 2, j / 2)) {

                    int lx = i - (bound.min().x + bound.max().x);
                    int lz = j - (bound.min().z + bound.max().z);

                    float height = 100 - Mth.sqrt(lx * lx + lz * lz) * 8.0F;
                    cir.setReturnValue(Mth.clamp(height, -100.0f, 80.0f));
                    return;
                }
            }
        }
    }
}
