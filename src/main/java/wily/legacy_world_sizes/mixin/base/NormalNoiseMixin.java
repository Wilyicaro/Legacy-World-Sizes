package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/synth/NormalNoise$NoiseParameters;firstOctave:I"))
    private int changeFistOctaveToGetter(int original, @Local(argsOnly = true) NormalNoise.NoiseParameters noiseParameters) {
        return noiseParameters.firstOctave();
    }
}
