package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @ModifyReturnValue(method = "getHitResult", at = @At("RETURN"))
    private static HitResult getHitResult(HitResult original, @Local(argsOnly = true) Level level) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
        if (limit != null) {
            Vec3 vec3 = original.getLocation();
            for (LegacyChunkBounds bound : limit.bounds()) {
                if (bound.isInside(vec3.x, vec3.z, 0)) return original;
            }
            return BlockHitResult.miss(original.getLocation(), Direction.UP, BlockPos.ZERO);
        }
        return original;
    }
}
