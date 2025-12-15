package wily.legacy_world_sizes.mixin.base;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReceiver(method = "collectColliders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;", remap = false))
    private static ImmutableList.Builder<VoxelShape> collectColliders(ImmutableList.Builder<VoxelShape> instance, @Nullable Entity entity, Level level, List<VoxelShape> list, AABB aabb) {
        if (entity != null) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());

            if (limit != null && !limit.bounds().isEmpty()) {
                for (LegacyChunkBounds bounds : limit.bounds()) {
                    if (bounds.isInsideCloseToBorder(entity, aabb)) {
                        instance.add(bounds.shape());
                    }
                }
            }
        }

        return instance;
    }
}
