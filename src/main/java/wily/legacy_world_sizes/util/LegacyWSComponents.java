package wily.legacy_world_sizes.util;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class LegacyWSComponents {

    public static Component optionName(String key){
        return Component.translatable("legacy_world_sizes.options."+key);
    }

    public static <T> Function<T, Component> staticTooltip(Component component) {
        return t -> component;
    }
}
