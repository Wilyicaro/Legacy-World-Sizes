package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.util.LegacyBiomeScale;

@Mixin(NormalNoise.NoiseParameters.class)
public class NoiseParametersMixin implements LegacyBiomeScale.OctaveFunctionHolder {
    @Unique
    LegacyBiomeScale.OctaveFunction lws$octaveFunction = i -> i;

    @Override
    public LegacyBiomeScale.OctaveFunction getOctaveFunction() {
        return lws$octaveFunction;
    }

    @Override
    public void setOctaveFunction(LegacyBiomeScale.OctaveFunction octaveFunction) {
        this.lws$octaveFunction = octaveFunction;
    }

    @ModifyReturnValue(method = "firstOctave", at = @At("RETURN"))
    private int applyOctaveFunction(int original) {
        return getOctaveFunction().apply(original);
    }
}
