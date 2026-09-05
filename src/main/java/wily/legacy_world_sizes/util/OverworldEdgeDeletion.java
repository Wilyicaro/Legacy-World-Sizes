package wily.legacy_world_sizes.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.LevelResource;
import wily.legacy_world_sizes.mixin.base.RegionFileStorageAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class OverworldEdgeDeletion {
    public static final int EDGE_WIDTH = 2;

    public static int run(MinecraftServer server, LegacyLevelLimit limit, boolean overwrite) throws IOException {
        Path path = DimensionType.getStorageFolder(Level.OVERWORLD, server.getWorldPath(LevelResource.ROOT));
        String name = server.getWorldData().getLevelName();
        Set<ChunkPos> deleted = new LinkedHashSet<>();
        Set<ChunkPos> retained = new LinkedHashSet<>();
        int width = overwrite ? EDGE_WIDTH : 0;
        for (LegacyChunkBounds bounds : limit.bounds()) {
            for (int inset = 0; inset <= Blender.HEIGHT_BLENDING_RANGE_CHUNKS; inset++) {
                addRing(inset < width ? deleted : retained, bounds, inset);
            }
        }
        retained.removeAll(deleted);

        int count = deleteChunks(path.resolve("region"), new RegionStorageInfo(name, Level.OVERWORLD, "chunk"), deleted);
        deleteChunks(path.resolve("entities"), new RegionStorageInfo(name, Level.OVERWORLD, "entities"), deleted);
        deleteChunks(path.resolve("poi"), new RegionStorageInfo(name, Level.OVERWORLD, "poi"), deleted);
        stampBlending(server, path.resolve("region"), new RegionStorageInfo(name, Level.OVERWORLD, "chunk"), retained);
        return count;
    }

    private static int deleteChunks(Path path, RegionStorageInfo info, Set<ChunkPos> chunks) throws IOException {
        if (chunks.isEmpty() || !Files.isDirectory(path)) return 0;
        int count = 0;
        try (RegionFileStorage storage = RegionFileStorageAccessor.lws$create(info, path, true)) {
            for (ChunkPos pos : chunks) {
                if (!Files.isRegularFile(regionPath(path, pos))) continue;
                CompoundTag chunk = storage.read(pos);
                if (chunk == null) continue;
                if (info.type().equals("chunk")) validatePosition(chunk, pos);
                ((RegionFileStorageAccessor) (Object) storage).lws$write(pos, null);
                count++;
            }
            storage.flush();
        }
        return count;
    }

    private static void stampBlending(MinecraftServer server, Path path, RegionStorageInfo info, Set<ChunkPos> chunks) throws IOException {
        if (!Files.isDirectory(path)) return;
        DimensionType dimension = server.registryAccess().getOrThrow(LevelStem.OVERWORLD).value().type().value();
        CompoundTag blending = new CompoundTag();
        blending.putInt("min_section", Math.floorDiv(dimension.minY(), 16));
        blending.putInt("max_section", Math.floorDiv(dimension.minY() + dimension.height(), 16));
        try (RegionFileStorage storage = RegionFileStorageAccessor.lws$create(info, path, true)) {
            for (ChunkPos pos : chunks) {
                if (!Files.isRegularFile(regionPath(path, pos))) continue;
                CompoundTag chunk = storage.read(pos);
                if (chunk == null) continue;
                validatePosition(chunk, pos);
                ChunkStatus status = chunk.read("Status", ChunkStatus.CODEC)
                        .orElseThrow(() -> new IOException("Invalid chunk status at " + pos));
                if (status.isBefore(ChunkStatus.NOISE) || chunk.contains("blending_data")) continue;
                if (chunk.getList("sections").isEmpty()) throw new IOException("Missing chunk sections at " + pos);
                chunk.put("blending_data", blending.copy());
                ((RegionFileStorageAccessor) (Object) storage).lws$write(pos, chunk);
            }
            storage.flush();
        }
    }

    static void validatePosition(CompoundTag chunk, ChunkPos pos) throws IOException {
        if (chunk.getInt("xPos").filter(x -> x == pos.x()).isEmpty() || chunk.getInt("zPos").filter(z -> z == pos.z()).isEmpty()) {
            throw new IOException("Invalid chunk position at " + pos);
        }
    }

    static Path regionPath(Path path, ChunkPos pos) {
        return path.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
    }

    static void addRing(Set<ChunkPos> chunks, LegacyChunkBounds bounds, int inset) {
        int minX = bounds.min().x() + inset;
        int minZ = bounds.min().z() + inset;
        int maxX = bounds.max().x() - inset - 1;
        int maxZ = bounds.max().z() - inset - 1;
        if (minX > maxX || minZ > maxZ) return;
        for (int x = minX; x <= maxX; x++) {
            chunks.add(new ChunkPos(x, minZ));
            chunks.add(new ChunkPos(x, maxZ));
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            chunks.add(new ChunkPos(minX, z));
            chunks.add(new ChunkPos(maxX, z));
        }
    }
}
