package wily.legacy_world_sizes.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.OptionalLong;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo info) {
        LWSWorldOptions.restoreChangedDefaults();
    }

    @Shadow @Final private WorldCreationUiState uiState;

    @Inject(method = "onCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createLevelSettings(Z)Lnet/minecraft/world/level/LevelSettings;"))
    private void onCreate(CallbackInfo ci, @Local LayeredRegistryAccess<RegistryLayer> registryAccess) {
        if (uiState.getSeed().isBlank() && LWSWorldOptions.balancedSeed.get()) {
            LWSWorldOptions.setupLegacyWorldSize(registryAccess.compositeAccess());
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.OVERWORLD);
            if (limit != null) {
                LegacyChunkBounds bounds = limit.bounds().get(0);
                uiState.setSettings(uiState.getSettings().withOptions(options -> options.withSeed(OptionalLong.of(bounds.findBalancedSeed(registryAccess.compositeAccess(), 100)))));
            }
        }
    }
}
