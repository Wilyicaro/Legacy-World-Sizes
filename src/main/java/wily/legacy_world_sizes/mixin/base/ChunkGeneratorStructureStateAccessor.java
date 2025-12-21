package wily.legacy_world_sizes.mixin.base;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ChunkGeneratorStructureState.class)
public interface ChunkGeneratorStructureStateAccessor {
    @Mutable
    @Accessor
    void setPossibleStructureSets(List<Holder<StructureSet>> possibleStructureSets);

    @Accessor
    void setHasGeneratedPositions(boolean hasGeneratedPositions);

    @Accessor
    long getConcentricRingsSeed();

    @Accessor
    BiomeSource getBiomeSource();

    @Accessor
    Map<Structure, List<StructurePlacement>> getPlacementsForStructure();
}
