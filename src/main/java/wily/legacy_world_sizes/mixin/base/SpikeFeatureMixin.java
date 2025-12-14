package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.util.List;

@Mixin(SpikeFeature.class)
public class SpikeFeatureMixin {

    @Inject(method = "getSpikesForLevel", at = @At("HEAD"), cancellable = true)
    private static void getSpikesForLevel(WorldGenLevel worldGenLevel, CallbackInfoReturnable<List<SpikeFeature.EndSpike>> cir) {
        if (LWSWorldOptions.legacyEndSpikes.get()) {
            cir.setReturnValue(LWSWorldOptions.LEGACY_END_SPIKES);
        }
    }

    @ModifyExpressionValue(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getMinY()I"))
    private int placeSpike(int original) {
        return LWSWorldOptions.legacyEndSpikes.get() ? original + 1 : original;
    }
}
