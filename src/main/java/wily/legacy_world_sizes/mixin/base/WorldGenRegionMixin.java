package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {
    @ModifyReturnValue(method = "ensureCanWrite", at = @At("RETURN"))
    private boolean preserveExpandedWorld(boolean original, BlockPos pos) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        if (!original || !LWSWorldOptions.legacyLevelLimits.get().containsKey(region.getLevel().dimension())) return original;
        ChunkAccess chunk = region.getChunk(pos);
        return chunk.getBlendingData() == null || chunk.getPersistedStatus().isBefore(ChunkStatus.FEATURES);
    }
}
