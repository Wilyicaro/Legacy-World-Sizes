package wily.legacy_world_sizes.config;

import wily.factoryapi.base.config.FactoryMixinToggle;

public class LWSMixinToggles {
    public static final FactoryMixinToggle.Storage COMMON_STORAGE = new FactoryMixinToggle.Storage("legacy_world_sizes/common_mixin.json");

    //private static FactoryMixinToggle createAndRegisterMixinOption(String key, String translationKey) {
    //    return COMMON_STORAGE.register(createMixinOption(key, translationKey, true));
    //}

    //public static FactoryMixinToggle createMixinOption(String key, String translationKey, boolean defaultValue) {
    //    return new FactoryMixinToggle(key, defaultValue, ()-> new FactoryConfigDisplay.Instance<>(optionName(translationKey), b-> LegacyComponents.NEEDS_RESTART, (c,v)->c));
    //}
}
