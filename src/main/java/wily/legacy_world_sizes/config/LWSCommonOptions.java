package wily.legacy_world_sizes.config;

import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy_world_sizes.util.LegacyBiomeScale;
import wily.legacy_world_sizes.util.LWSComponents;
import wily.legacy_world_sizes.util.LegacyWorldSize;

import java.util.function.UnaryOperator;

public class LWSCommonOptions {
    public static final FactoryConfig.StorageHandler COMMON_STORAGE = new FactoryConfig.StorageHandler().withFile("legacy_world_sizes/common.json");

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer, FactoryConfigDisplay.Builder<T> builder) {
        return consumer.apply(new FactoryConfig.Builder<>()).displayFromKey(t -> builder.build(LWSComponents.optionName(t))).buildAndRegister(COMMON_STORAGE);
    }

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer) {
        return buildAndRegister(consumer, FactoryConfigDisplay.builder());
    }

    public static final FactoryConfig<Boolean> balancedSeed = buildAndRegister(b -> b.key("balancedSeed").control(FactoryConfigControl.TOGGLE).defaultValue(true), FactoryConfigDisplay.toggleBuilder().tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("balancedSeed.description"))));

    public static final FactoryConfig<LegacyWorldSize> legacyWorldSize = buildAndRegister(b -> b.key("legacyWorldSize").control(new FactoryConfigControl.FromInt<>(LegacyWorldSize.CODEC, LegacyWorldSize.map::getByIndex, LegacyWorldSize.map::indexOf, LegacyWorldSize.map::size)).defaultValue(LegacyWorldSize.CUSTOM), FactoryConfigDisplay.<LegacyWorldSize>builder().valueToComponent(LegacyWorldSize::name).tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("legacyWorldSize.description"))));

    public static final FactoryConfig<LegacyBiomeScale> legacyBiomeScale = buildAndRegister(b -> b.key("legacyBiomeScale").control(new FactoryConfigControl.FromInt<>(LegacyBiomeScale.CODEC, LegacyBiomeScale.map::getByIndex, LegacyBiomeScale.map::indexOf, LegacyBiomeScale.map::size)).defaultValue(LegacyBiomeScale.CUSTOM), FactoryConfigDisplay.<LegacyBiomeScale>builder().valueToComponent(LegacyBiomeScale::name).tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("legacyBiomeScale.description"))));
}
