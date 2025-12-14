package wily.legacy_world_sizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyWorldSize;

public class LegacyWorldSizesClient {
    public static void init() {
        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(LegacyWorldSizesClient::onDisconnect);
        FactoryAPIClient.setup(LegacyWorldSizesClient::setup);
    }

    public static void setup(Minecraft minecraft) {
    }

    public static void onDisconnect(LocalPlayer player) {
        LWSWorldOptions.legacyWorldSize.setDefault(LegacyWorldSize.CUSTOM);
        LWSWorldOptions.WORLD_STORAGE.file = null;
    }
}
