package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.util.LevelWorldBorder;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @ModifyExpressionValue(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder$Settings;toWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"))
    private WorldBorder createLevels(WorldBorder original, @Local(ordinal = 1) ServerLevel level) {
        return LevelWorldBorder.withLevel(original, level);
    }
}
