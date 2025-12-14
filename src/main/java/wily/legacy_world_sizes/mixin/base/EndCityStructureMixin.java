package wily.legacy_world_sizes.mixin.base;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.List;
import java.util.Optional;

@Mixin(EndCityStructure.class)
public abstract class EndCityStructureMixin {
    @Shadow protected abstract void generatePieces(StructurePiecesBuilder structurePiecesBuilder, BlockPos blockPos, Rotation rotation, Structure.GenerationContext generationContext);

    @ModifyReturnValue(method = "findGenerationPoint", at = @At("RETURN"))
    public Optional<Structure.GenerationStub> findGenerationPoint(Optional<Structure.GenerationStub> original, Structure.GenerationContext generationContext, @Local Rotation rotation, @Local BlockPos lowest) {
        if (original.isEmpty() && generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());

            if (limit != null && level.dimension() == Level.END) {
                for (LegacyLevelLimit.ChunkBounds bound : limit.bounds()) {
                    if (bound.isInside(generationContext.chunkPos().x, generationContext.chunkPos().z) && bound.middle().equals(generationContext.chunkPos()))
                        return Optional.of(new Structure.GenerationStub(lowest, structurePiecesBuilder -> this.generatePieces(structurePiecesBuilder, lowest, rotation, generationContext)));
                }
            }
        }
        return original;
    }

    @Inject(method = "generatePieces", at = @At("HEAD"), cancellable = true)
    public void generatePieces(StructurePiecesBuilder structurePiecesBuilder, BlockPos blockPos, Rotation rotation, Structure.GenerationContext generationContext, CallbackInfo ci) {
        List<StructurePiece> list = Lists.newArrayList();
        if (generationContext.heightAccessor() instanceof ChunkAccessAccessor accessor && accessor.getLevelHeightAccessor() instanceof Level level) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(level.dimension());
            if (limit != null && level.dimension() == Level.END) {
                LegacyLevelLimit.ChunkBounds bounds = null;

                for (LegacyLevelLimit.ChunkBounds bound : limit.bounds()) {
                    if (bound.isInside(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
                        bounds = bound;
                        break;
                    }
                }

                if (bounds == null) return;

                for (int i = 0; i < 1000; i++) {
                    list.clear();
                    EndCityPieces.startHouseTower(generationContext.structureTemplateManager(), blockPos, rotation, list, generationContext.random());

                    boolean hasShip = false;

                    for (StructurePiece structurePiece : list) {
                        if (structurePiece instanceof TemplateStructurePieceAccessor piece && piece.getTemplateName().equals("ship") && bounds.isInside(structurePiece.getBoundingBox())) {
                            hasShip = true;
                            break;
                        }
                    }

                    if (hasShip) {
                        LegacyWorldSizes.LOGGER.debug("Found End City with limited size after {} tries!", i);
                        break;
                    }

                    if (i == 999) {
                        LegacyWorldSizes.LOGGER.debug("Couldn't find End City with limited size after {} tries, placing anyway.", i);
                        break;
                    }
                }

                LegacyWorldSizes.LOGGER.debug("Placing End City with {} pieces at {}, {}, {}", list.size(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
                list.forEach(structurePiecesBuilder::addPiece);
                ci.cancel();
            }
        }
    }
}
