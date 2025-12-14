package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(StrongholdStructure.class)
public abstract class StrongholdStructureMixin {

    @ModifyExpressionValue(method = "generatePieces", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$StartPiece;portalRoomPiece:Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$PortalRoom;"))
    private static StrongholdPieces.PortalRoom generatePieces(StrongholdPieces.PortalRoom original, StructurePiecesBuilder structurePiecesBuilder, Structure.GenerationContext generationContext) {
        if (original != null && generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
            if (limit != null && level.dimension() == Level.OVERWORLD) {
                for (LegacyLevelLimit.ChunkBounds bound : limit.bounds()) {
                    if (bound.isInside(original.getBoundingBox())) return original;
                }
                return null;
            }
        }
        return original;
    }
}
