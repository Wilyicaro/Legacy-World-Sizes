package wily.legacy_world_sizes.mixin.base;

import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DedicatedServerProperties.class)
public interface DedicatedServerPropertiesAccessor {
    @Mutable
    @Accessor
    void setWorldOptions(WorldOptions worldOptions);
}
