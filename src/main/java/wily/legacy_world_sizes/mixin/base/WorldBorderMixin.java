package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyLevelLimit;
import wily.legacy_world_sizes.util.LevelWorldBorder;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements LevelWorldBorder {
    @Unique
    private Level level;

    @Override
    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @ModifyReturnValue(method = "isWithinBounds(DDD)Z", at = @At("RETURN"))
    private boolean isWithinBounds(boolean original, double d, double e, double f) {
        if (getLevel() != null && original) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(getLevel().dimension());
            if (limit != null) {
                for (LegacyLevelLimit.ChunkBounds bound : limit.bounds()) {
                    if (bound.isInside(d, e, f)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return original;
    }

    @ModifyReturnValue(method = "clampVec3ToBound(DDD)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private Vec3 clampVec3ToBound(Vec3 original) {
        if (getLevel() != null) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(getLevel().dimension());
            if (limit != null) {
                for (LegacyLevelLimit.ChunkBounds bound : limit.bounds()) {
                    if (!bound.isInside(original.x, original.z, 0))
                        original = new Vec3(Mth.clamp(original.x, bound.min().getMinBlockX(), bound.max().getMinBlockX() - 1.0E-5F), original.y, Mth.clamp(original.z, bound.min().getMinBlockZ(), bound.max().getMinBlockZ() - 1.0E-5F));
                }
            }
        }
        return original;
    }
}
