package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy_world_sizes.config.LWSWorldOptions;

@Mixin(Structure.class)
public class StructureMixin {
    @ModifyArg(method = "isValidBiome", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeSource;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"), index = 0)
    private static int changeValidBiomeX(int i, @Local(argsOnly = true) Structure.GenerationContext generationContext) {
        if (generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level && LWSWorldOptions.legacyLevelLimits.get().get(level.dimension()) != null) {
            return QuartPos.fromSection(generationContext.chunkPos().x);
        }
        return i;
    }

    @ModifyArg(method = "isValidBiome", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeSource;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"), index = 2)
    private static int changeValidBiomeZ(int i, @Local(argsOnly = true) Structure.GenerationContext generationContext) {
        if (generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level && LWSWorldOptions.legacyLevelLimits.get().get(level.dimension()) != null) {
            return QuartPos.fromSection(generationContext.chunkPos().z);
        }
        return i;
    }
}
