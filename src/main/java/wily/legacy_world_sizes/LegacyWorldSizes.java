package wily.legacy_world_sizes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy_world_sizes.config.LegacyWSCommonOptions;
import wily.legacy_world_sizes.config.LegacyMixinToggles;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.init.LWSRegistries;
import wily.legacy_world_sizes.level.FakeLevelChunk;

//? if fabric {
//?} else if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
*///?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
*///?}

import java.util.*;
import java.util.List;
import java.util.function.*;

//? if forge || neoforge
/*@Mod(LegacyWorldSizes.MOD_ID)*/
public class LegacyWorldSizes {

    public static final String MOD_ID = "legacy_world_sizes";
    public static final Supplier<String> VERSION = () -> FactoryAPIPlatform.getModInfo(MOD_ID).getVersion();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final FactoryConfig.StorageHandler MIXIN_CONFIGS_STORAGE = FactoryConfig.StorageHandler.fromMixin(LegacyMixinToggles.COMMON_STORAGE, true);

    public LegacyWorldSizes() {
        init();
        //? if forge || neoforge {
        /*if (FactoryAPI.isClient())
            LegacyWorldSizesClient.init();
        *///?}
    }

    public static List<Integer> getParsedVersion(String version) {
        List<Integer> parsedVersion = new ArrayList<>();
        String[] versions = version.split("[.\\-]");
        for (String s : versions) {
            int value;
            try {
                value = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                value = 0;
            }
            parsedVersion.add(value);
        }
        return parsedVersion;
    }

    public static boolean isNewerVersion(String actualVersion, String previous) {
        return isNewerVersion(actualVersion, previous, 2);
    }

    public static boolean isNewerVersion(String actualVersion, String previous, int limitCount) {
        List<Integer> v = getParsedVersion(actualVersion);
        List<Integer> v1 = getParsedVersion(previous);
        int size = limitCount <= 0 ? v.size() : Math.min(limitCount, v.size());
        for (int i = 0; i < size; i++) {
            if (v.get(i) > (v1.size() <= i ? 0 : v1.get(i))) return true;
        }
        return false;
    }

    public static void init() {
        FactoryConfig.registerCommonStorage(createModLocation("common"), LegacyWSCommonOptions.COMMON_STORAGE);
        FactoryConfig.registerCommonStorage(createModLocation("mixin_common"), MIXIN_CONFIGS_STORAGE);
        LWSRegistries.register();
        FactoryEvent.setup(LegacyWorldSizes::setup);
        FactoryConfig.registerCommonStorage(createModLocation("config"), LWSWorldOptions.WORLD_STORAGE);
        FactoryEvent.serverStarted(LegacyWorldSizes::onServerStart);
        FactoryEvent.serverStopped(LegacyWorldSizes::onServerStop);
        FactoryEvent.PlayerEvent.JOIN_EVENT.register(LegacyWorldSizes::onServerPlayerJoin);
        FactoryEvent.PlayerEvent.RELOAD_RESOURCES_EVENT.register(LegacyWorldSizes::onResourcesReload);
    }

    public static ResourceLocation createModLocation(String path) {
        return FactoryAPI.createLocation(MOD_ID, path);
    }

    public static void setup() {
        LegacyWSCommonOptions.COMMON_STORAGE.load();

    }

    public static void onServerPlayerJoin(ServerPlayer p) {

    }

    public static void serverStarting(MinecraftServer server) {
        LWSWorldOptions.WORLD_STORAGE.withServerFile(server, "config/legacy_world_sizes.json").resetAndLoad();
        LWSWorldOptions.setupLegacyWorldSize(server.registryAccess());
        if (server instanceof DedicatedServer dedicatedServer)
            LWSWorldOptions.setupDedicatedServerBalancedSeed(dedicatedServer);
    }

    public static void onServerStart(MinecraftServer server) {
        LWSWorldOptions.setupStrongholdValidPlacement(server);
        LWSWorldOptions.setupEndLimits(server);
    }

    public static void onServerStop(MinecraftServer server) {
        FakeLevelChunk.ContentType.CACHE.clear();
    }

    public static void onResourcesReload(PlayerList playerList) {
        onServerStart(playerList.getServer());
    }


}