package wily.legacy_world_sizes.util;

import net.minecraft.network.chat.Component;

public class LegacyWSComponents {

    public static Component optionName(String key){
        return Component.translatable("legacy_world_sizes.options."+key);
    }
}
