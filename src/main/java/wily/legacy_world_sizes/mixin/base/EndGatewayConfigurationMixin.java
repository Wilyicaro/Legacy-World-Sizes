package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.util.Optional;

@Mixin(EndGatewayConfiguration.class)
public class EndGatewayConfigurationMixin {
    @ModifyReturnValue(method = "getExit", at = @At("RETURN"))
    private Optional<BlockPos> knownExit(Optional<BlockPos> original) {
        return original.isPresent() && original.get().equals(ServerLevel.END_SPAWN_POINT) ? LWSWorldOptions.endSpawnPoint.optional() : original;
    }
}
