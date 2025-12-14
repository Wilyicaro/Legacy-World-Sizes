package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {
    @ModifyExpressionValue(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;origin()Lnet/minecraft/core/BlockPos;"))
    private BlockPos modifyEndSpawnPoint(BlockPos original, FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        return original.equals(ServerLevel.END_SPAWN_POINT) ? LWSWorldOptions.endSpawnPoint.get() : original;
    }
}
