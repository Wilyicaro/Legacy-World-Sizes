package wily.legacy_world_sizes.level;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FakeLevelChunk extends LevelChunk {
    private final Holder<Biome> biome;

    public FakeLevelChunk(ServerLevel level, ChunkPos chunkPos, LevelChunkSection[] sections, boolean hasBlending, Holder<Biome> biome) {
        super(level, chunkPos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, sections, null, hasBlending ? BlendingData.unpack(new BlendingData.Packed(SectionPos.blockToSectionCoord(-64), SectionPos.blockToSectionCoord(320), Optional.empty())) : null);
        this.biome = biome;
    }

    public static ProtoChunk fillChunk(ServerLevel level, ProtoChunk chunk, ContentType contentType) {
        switch (contentType) {
            case OCEAN -> {
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
                Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
                Heightmap heightmap2 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

                int seaLevel = level.getSeaLevel();

                for (int h = level.getMinY(); h < seaLevel; h++) {
                    BlockState blockState = h == level.getMinY() ? Blocks.BEDROCK.defaultBlockState() : h > seaLevel - 10 ? Blocks.WATER.defaultBlockState() : Blocks.STONE.defaultBlockState();

                    for (int k = 0; k < 16; k++) {
                        for (int l = 0; l < 16; l++) {
                            chunk.setBlockState(mutableBlockPos.set(k, h, l), blockState);
                            heightmap.update(k, h, l, blockState);
                            heightmap2.update(k, h, l, blockState);
                        }
                    }
                }
            }
            case FLAT -> {
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
                Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
                Heightmap heightmap2 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

                int max = level.getMinY() + 4;

                for (int h = level.getMinY(); h < max; h++) {
                    BlockState blockState = h == level.getMinY() ? Blocks.BEDROCK.defaultBlockState() : h == max - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();

                    for (int k = 0; k < 16; k++) {
                        for (int l = 0; l < 16; l++) {
                            chunk.setBlockState(mutableBlockPos.set(k, h, l), blockState);
                            heightmap.update(k, h, l, blockState);
                            heightmap2.update(k, h, l, blockState);
                        }
                    }
                }
            }
        }

        return chunk;
    }

    @Override
    public boolean tryMarkSaved() {
        return false;
    }

    @Override
    public boolean isUnsaved() {
        return false;
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos blockPos, BlockState blockState, int i) {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos blockPos, LevelChunk.EntityCreationType entityCreationType) {
        return null;
    }

    @Override
    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
    }

    @Override
    public void removeBlockEntity(BlockPos blockPos) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isYSpaceEmpty(int i, int j) {
        return true;
    }

    @Override
    public FullChunkStatus getFullStatus() {
        return FullChunkStatus.FULL;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return super.getPersistedStatus();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return biome == null ? super.getNoiseBiome(i, j, k) : biome;
    }

    @Override
    public void postProcessGeneration(ServerLevel serverLevel) {
    }

    @Override
    public void markPosForPostprocessing(BlockPos blockPos) {
    }

    //TODO: Replace this with something data-driven
    public enum ContentType implements StringRepresentable {
        NONE("none"),OCEAN("ocean"),FLAT("flat");

        public static final Codec<ContentType> CODEC = StringRepresentable.fromEnum(ContentType::values);

        private final String id;

        ContentType(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
