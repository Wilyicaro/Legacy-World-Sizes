package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @ModifyExpressionValue(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;getTeleportationScale(Lnet/minecraft/world/level/dimension/DimensionType;Lnet/minecraft/world/level/dimension/DimensionType;)D"))
    private double getPortalDestination(double original, ServerLevel from, @Local(ordinal = 1) ServerLevel to) {
        return LWSWorldOptions.getTeleportationScale(from.dimension(), to.dimension(), original);
    }
}
