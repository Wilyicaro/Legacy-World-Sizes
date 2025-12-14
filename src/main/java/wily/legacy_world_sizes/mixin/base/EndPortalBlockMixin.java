package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EndPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @ModifyExpressionValue(method = "getPortalDestination", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;END_SPAWN_POINT:Lnet/minecraft/core/BlockPos;"))
    public BlockPos getPortalDestination(BlockPos original) {
        return LWSWorldOptions.endSpawnPoint.get();
    }
}