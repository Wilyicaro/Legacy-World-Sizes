package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.util.function.BiConsumer;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {

    @WrapWithCondition(method = "interactWithBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"))
    private boolean interactWithBlocks(BlockState instance, ServerLevel level, BlockPos blockPos, Explosion explosion, BiConsumer biConsumer) {
        return LWSWorldOptions.isValidPos(level.dimension(), blockPos);
    }
}
