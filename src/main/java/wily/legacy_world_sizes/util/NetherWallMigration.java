package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import wily.legacy_world_sizes.mixin.base.RegionFileStorageAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class NetherWallMigration {
    public static int run(MinecraftServer server, LegacyLevelLimit limit) throws IOException {
        Path path = DimensionType.getStorageFolder(Level.NETHER, server.getWorldPath(LevelResource.ROOT)).resolve("region");
        if (!limit.bedrockBarrier() || !Files.isDirectory(path)) return 0;
        LevelStem stem = server.registryAccess().getOrThrow(LevelStem.NETHER).value();
        ChunkGenerator generator = stem.generator();
        DimensionType dimension = stem.type().value();
        LevelHeightAccessor height = LevelHeightAccessor.create(dimension.minY(), dimension.height());
        NoiseGeneratorSettings settings = generator instanceof NoiseBasedChunkGenerator noise ? noise.generatorSettings().value() : NoiseGeneratorSettings.dummy();
        RandomState randomState = RandomState.create(settings, server.registryAccess().lookupOrThrow(Registries.NOISE), server.getWorldGenSettings().options().seed());
        WallReplacement replacement = new WallReplacement(generator, height, randomState, randomState.getOrCreateRandomFactory(LegacyChunkBounds.BEDROCK_WALLS_RANDOM), PalettedContainerFactory.create(server.registryAccess()).blockStatesContainerCodec());
        RegionStorageInfo info = new RegionStorageInfo(server.getWorldData().getLevelName(), Level.NETHER, "chunk");
        Set<ChunkPos> seen = new LinkedHashSet<>();
        int count = 0;
        try (RegionFileStorage storage = RegionFileStorageAccessor.lws$create(info, path, true)) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                Set<ChunkPos> chunks = new LinkedHashSet<>();
                OverworldEdgeDeletion.addRing(chunks, bounds, 0);
                for (ChunkPos pos : chunks) {
                    if (!seen.add(pos) || !Files.isRegularFile(OverworldEdgeDeletion.regionPath(path, pos))) continue;
                    CompoundTag chunk = storage.read(pos);
                    if (chunk == null) continue;
                    OverworldEdgeDeletion.validatePosition(chunk, pos);
                    ChunkStatus status = chunk.read("Status", ChunkStatus.CODEC)
                            .orElseThrow(() -> new IOException("Invalid Nether chunk status at " + pos));
                    if (status.isBefore(ChunkStatus.FEATURES) || !replacement.clean(chunk, pos, bounds)) continue;
                    ((RegionFileStorageAccessor) (Object) storage).lws$write(pos, chunk);
                    count++;
                }
            }
            storage.flush();
        }
        return count;
    }

    private record WallReplacement(ChunkGenerator generator, LevelHeightAccessor height, RandomState randomState, PositionalRandomFactory wallRandom, Codec<PalettedContainer<BlockState>> statesCodec) {
        private boolean clean(CompoundTag chunk, ChunkPos pos, LegacyChunkBounds bounds) throws IOException {
            ListTag sections = chunk.getList("sections").orElseThrow(() -> new IOException("Missing Nether chunk sections at " + pos));
            NoiseColumn[] columns = new NoiseColumn[256];
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
            int floor = Math.max(height.getMinY(), generator.getMinY());
            int roof = generator instanceof FlatLevelSource ? height.getMaxY() + 1 : Math.min(height.getMaxY() + 1, generator.getMinY() + generator.getGenDepth());
            boolean changed = false;
            for (int i = 0; i < sections.size(); i++) {
                CompoundTag section = sections.getCompound(i).orElseThrow(() -> new IOException("Invalid Nether chunk section at " + pos));
                if (!section.contains("block_states")) continue;
                int sectionY = section.getByte("Y").orElseThrow(() -> new IOException("Missing Nether section height at " + pos)) * 16;
                if (sectionY < height.getMinY() || sectionY > height.getMaxY()) throw new IOException("Invalid Nether section height at " + pos);
                PalettedContainer<BlockState> states = statesCodec.parse(NbtOps.INSTANCE, section.get("block_states")).getOrThrow();
                boolean sectionChanged = false;
                for (int dx = 0; dx < 16; dx++) {
                    int x = pos.getMinBlockX() + dx;
                    for (int dz = 0; dz < 16; dz++) {
                        int z = pos.getMinBlockZ() + dz;
                        for (int dy = 0; dy < 16; dy++) {
                            int y = sectionY + dy;
                            if (y < floor + 5 || y >= roof - 5 && y < roof || !states.get(dx, dy, dz).is(Blocks.BEDROCK)) continue;
                            blockPos.set(x, y, z);
                            int thickness = wallRandom.at(blockPos).nextInt(5);
                            if (x > bounds.min().getMinBlockX() + thickness && z > bounds.min().getMinBlockZ() + thickness && x < bounds.max().getMinBlockX() - 1 - thickness && z < bounds.max().getMinBlockZ() - 1 - thickness) continue;
                            int columnIndex = dz * 16 + dx;
                            NoiseColumn column = columns[columnIndex];
                          
                            if (column == null) columns[columnIndex] = column = generator.getBaseColumn(x, z, height, randomState);
                            BlockState state = column.getBlock(y);
                            if (state.is(Blocks.BEDROCK)) continue;
                            states.set(dx, dy, dz, state);
                            sectionChanged = true;
                        }
                    }
                }
                if (!sectionChanged) continue;
                section.put("block_states", statesCodec.encodeStart(NbtOps.INSTANCE, states).getOrThrow());
                changed = true;
            }
            if (changed) {
                chunk.remove("Heightmaps");
                chunk.putBoolean("isLightOn", false);
                for (int i = 0; i < sections.size(); i++) {
                    CompoundTag section = sections.getCompoundOrEmpty(i);
                    section.remove("BlockLight");
                    section.remove("SkyLight");
                }
            }
            return changed;
        }
    }
}
