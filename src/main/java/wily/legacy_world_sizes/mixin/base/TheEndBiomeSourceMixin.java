package wily.legacy_world_sizes.mixin.base;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(TheEndBiomeSource.class)
public class TheEndBiomeSourceMixin {
    @Shadow @Final private Holder<Biome> end;

    @Shadow @Final private Holder<Biome> highlands;

    @Shadow @Final private Holder<Biome> midlands;

    @Shadow @Final private Holder<Biome> islands;

    @Shadow @Final private Holder<Biome> barrens;

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    public void getNoiseBiome(int i, int j, int k, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);

        if (limit != null) {
            int x = QuartPos.toBlock(i);
            int m = QuartPos.toBlock(j);
            int n = QuartPos.toBlock(k);
            int o = SectionPos.blockToSectionCoord(x);
            int p = SectionPos.blockToSectionCoord(n);
            if ((long) o * o + (long) p * p <= Mth.square(LWSWorldOptions.endOuterIslandsRay.get())) {
                cir.setReturnValue(this.end);
            } else {
                int q = (SectionPos.blockToSectionCoord(x) * 2 + 1) * 8;
                int r = (SectionPos.blockToSectionCoord(n) * 2 + 1) * 8;
                double d = sampler.erosion().compute(new DensityFunction.SinglePointContext(q, m, r));
                if (d > 0.25) {
                    cir.setReturnValue(highlands);
                } else if (d >= -0.0625) {
                    cir.setReturnValue(midlands);
                } else {
                    cir.setReturnValue(d < -0.21875 ? this.islands : this.barrens);
                }
            }
        }
    }
}
