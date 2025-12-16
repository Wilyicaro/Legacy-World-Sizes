package wily.legacy_world_sizes.mixin.base.client.compat.legacy4j;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.FactoryConfigWidgets;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.util.function.Function;

@Mixin(WorldMoreOptionsScreen.class)
public class WorldMoreOptionsScreenMixin extends PanelVListScreen {
    public WorldMoreOptionsScreenMixin(Panel.Constructor<PanelVListScreen> panelConstructor, Component component) {
        super(panelConstructor, component);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lwily/factoryapi/base/Bearer;)V", at = @At("RETURN"))
    private void init(CreateWorldScreen parent, Bearer trustPlayers, CallbackInfo ci) {
        addLabeledConfigSlider(LWSWorldOptions.legacyBiomeScale);
        addLabeledConfigSlider(LWSWorldOptions.legacyWorldSize);
        renderableVList.renderables.add(3, SimpleLayoutRenderable.create(0, 9, (r) -> (guiGraphics, i, j, f) -> {}));
        renderableVList.renderables.add(3, LegacyConfigWidgets.createWidget(LWSWorldOptions.balancedSeed, 0, 0, 200, LWSWorldOptions.balancedSeed::setDefault));
    }

    @Unique
    private <T> void addLabeledConfigSlider(FactoryConfig<T> config) {
        FactoryConfigControl.FromInt<T> c = (FactoryConfigControl.FromInt<T>) config.control();
        Function<T, Tooltip> tooltipFunction = (v) -> FactoryConfigWidgets.getCachedTooltip(config.getDisplay().tooltip().apply(v));
        renderableVList.renderables.add(3, LegacySliderButton.createFromInt(0, 0, 200, 16, (s) -> config.getDisplay().valueToComponent().apply(s.getObjectValue()), (s) -> tooltipFunction.apply(s.getObjectValue()), config.get(), c.valueGetter(), c.valueSetter(), c.valuesSize(), (s) -> FactoryConfig.saveOptionAndConsume(config, s.getObjectValue(), config::setDefault), config));
        renderableVList.renderables.add(3, SimpleLayoutRenderable.createDrawString(config.getDisplay().name(), 1, 2, 0, 9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }
}
