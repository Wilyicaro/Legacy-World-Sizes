package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(Blender.class)
public class BlenderMixin {
    @Shadow @Final private static int HEIGHT_BLENDING_RANGE_CHUNKS;

    @ModifyExpressionValue(method = "of", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;isOldChunkAround(Lnet/minecraft/world/level/ChunkPos;I)Z"))
    private static boolean changeOldChunkAround(boolean original, WorldGenRegion region) {
        if (original) return true;
        ChunkPos center = region.getCenter();

        if (LWSWorldOptions.isValidPos(region.getLevel().dimension(), center.x, center.z)) {
            int i = Mth.square(HEIGHT_BLENDING_RANGE_CHUNKS + 1);
            for (int dx = -HEIGHT_BLENDING_RANGE_CHUNKS; dx <= HEIGHT_BLENDING_RANGE_CHUNKS; dx++) {
                for (int dz = -HEIGHT_BLENDING_RANGE_CHUNKS; dz <= HEIGHT_BLENDING_RANGE_CHUNKS; dz++) {
                    if (dx == 0 || dz == 0 || dx * dx + dz * dz > i) continue;
                    if (!LWSWorldOptions.isValidPos(region.getLevel().dimension(), center.x + dx, center.z + dz)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
