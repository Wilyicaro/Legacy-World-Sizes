package wily.legacy_world_sizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.client.screen.FactoryConfigScreen;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy_world_sizes.client.Legacy4JCompat;
import wily.legacy_world_sizes.config.LWSCommonOptions;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LWSComponents;

import java.util.List;

public class LegacyWorldSizesClient {
    public static final List<FactoryConfig<?>> DISPLAYED_CONFIGS = List.of(LWSCommonOptions.balancedSeed, LWSCommonOptions.legacyBiomeScale, LWSCommonOptions.legacyWorldSize);

    public static void init() {
        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(LegacyWorldSizesClient::onDisconnect);
        FactoryAPIClient.registerConfigScreen(FactoryAPIPlatform.getModInfo(LegacyWorldSizes.MOD_ID), LegacyWorldSizesClient::createConfigScreen);
        FactoryAPIClient.setup(LegacyWorldSizesClient::setup);
        if (FactoryAPI.isModLoaded("legacy"))
            Legacy4JCompat.init();
    }

    public static Screen createConfigScreen(Screen parent) {
        return new FactoryConfigScreen(parent, DISPLAYED_CONFIGS, LWSComponents.OPTIONS_TITLE);
    }

    public static void setup(Minecraft minecraft) {
    }

    public static void onDisconnect(LocalPlayer player) {
        LWSWorldOptions.restoreChangedDefaults();
        LWSWorldOptions.WORLD_STORAGE.file = null;
    }
}
