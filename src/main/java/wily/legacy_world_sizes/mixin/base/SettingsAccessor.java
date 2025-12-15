package wily.legacy_world_sizes.mixin.base;

import net.minecraft.server.dedicated.Settings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Settings.class)
public interface SettingsAccessor {
    @Invoker("get")
    String getString(String string, String string2);
}
