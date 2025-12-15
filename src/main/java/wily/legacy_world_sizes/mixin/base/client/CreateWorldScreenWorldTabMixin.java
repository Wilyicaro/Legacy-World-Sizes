package wily.legacy_world_sizes.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryConfigWidgets;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public class CreateWorldScreenWorldTabMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CreateWorldScreen createWorldScreen, CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
        rowHelper.addChild(FactoryConfigWidgets.createWidget(LWSWorldOptions.balancedSeed, 0, 0, 150, LWSWorldOptions.balancedSeed::setDefault));
        rowHelper.addChild(FactoryConfigWidgets.createWidget(LWSWorldOptions.legacyWorldSize, 0, 0, 308, LWSWorldOptions.legacyWorldSize::setDefault), 2);
    }
}
