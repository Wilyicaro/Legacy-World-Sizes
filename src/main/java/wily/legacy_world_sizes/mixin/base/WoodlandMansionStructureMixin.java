package wily.legacy_world_sizes.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.Optional;

@Mixin(WoodlandMansionStructure.class)
public abstract class WoodlandMansionStructureMixin extends Structure {

    protected WoodlandMansionStructureMixin(StructureSettings structureSettings) {
        super(structureSettings);
    }

    @Shadow protected abstract void generatePieces(StructurePiecesBuilder arg, Structure.GenerationContext arg2, BlockPos arg3, Rotation arg4);

    @ModifyReturnValue(method = "findGenerationPoint", at = @At("RETURN"))
    public Optional<Structure.GenerationStub> findGenerationPoint(Optional<Structure.GenerationStub> original, Structure.GenerationContext generationContext, @Local Rotation rotation, @Local BlockPos lowest) {
        if (original.isEmpty() && generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());

            if (limit != null && level.dimension() == Level.OVERWORLD) {
                for (LegacyChunkBounds bound : limit.bounds()) {
                    if (bound.isInside(generationContext.chunkPos().x, generationContext.chunkPos().z)) {
                        return Optional.of(new Structure.GenerationStub(lowest, structurePiecesBuilder -> this.generatePieces(structurePiecesBuilder, generationContext, lowest, rotation)));
                    }
                }
            }
        }
        return original;
    }
}
