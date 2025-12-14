package wily.legacy_world_sizes.config;

import wily.factoryapi.base.config.FactoryConfig;

public class LegacyWSCommonOptions {
    public static final FactoryConfig.StorageHandler COMMON_STORAGE = new FactoryConfig.StorageHandler(true).withFile("legacy_world_sizes/common.json");

}
