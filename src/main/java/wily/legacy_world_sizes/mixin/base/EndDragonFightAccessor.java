package wily.legacy_world_sizes.mixin.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EndDragonFight.class)
public interface EndDragonFightAccessor {
    @Accessor
    ObjectArrayList<Integer> getGateways();
}
