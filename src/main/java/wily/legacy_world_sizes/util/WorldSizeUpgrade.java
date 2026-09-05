package wily.legacy_world_sizes.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import wily.factoryapi.base.MinecraftServerAccessor;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldSizeUpgrade {
    private static final List<LegacyWorldSize> SIZES = List.of(LegacyWorldSize.CLASSIC, LegacyWorldSize.SMALL, LegacyWorldSize.MEDIUM, LegacyWorldSize.LARGE);

    public static void loadAndUpgrade(MinecraftServer server) {
        LWSWorldOptions.WORLD_STORAGE.withServerFile(server, "config/legacy_world_sizes.json").reset();
        Path path = LWSWorldOptions.WORLD_STORAGE.file.toPath();
        Path source = Files.exists(path) ? path : server.getWorldPath(LevelResource.ROOT).resolve("config/legacy_world_sizes.json");
        try {
            JsonObject options = new JsonObject();
            if (Files.exists(source)) {
                options = read(source);
                var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
                for (var entry : options.entrySet()) {
                    var config = LWSWorldOptions.WORLD_STORAGE.configMap.get(entry.getKey());
                    if (config != null) config.decode(new Dynamic<>(ops, entry.getValue())).getOrThrow();
                }
            }

            LegacyWorldSize selected = LWSWorldOptions.legacyWorldSize.get();
            Path progressPath = path.resolveSibling("legacy_world_sizes_upgrade.json");
            JsonObject progress = Files.exists(progressPath) ? read(progressPath) : null;
            if (!server.getWorldData().overworldData().isInitialized()) {
                if (progress != null) throw new IllegalStateException("An unfinished world size upgrade has no initialized world");
                LWSWorldOptions.appliedLegacyWorldSize.set(selected);
                LWSWorldOptions.overwriteWorldEdge.set(false);
                return;
            }
            if (!options.has(LWSWorldOptions.appliedLegacyWorldSize.getKey()) || LWSWorldOptions.appliedLegacyWorldSize.get() == LegacyWorldSize.CUSTOM && selected != LegacyWorldSize.CUSTOM) {
                boolean savedPreset = options.has(LWSWorldOptions.legacyWorldSize.getKey()) && !options.has(LWSWorldOptions.appliedLegacyWorldSize.getKey());
                LWSWorldOptions.appliedLegacyWorldSize.set(inferAppliedSize(server.registryAccess(), selected, savedPreset));
            }

            LegacyWorldSize applied = LWSWorldOptions.appliedLegacyWorldSize.get();
            if (progress == null && selected == applied) {
                LWSWorldOptions.overwriteWorldEdge.set(false);
                return;
            }
            LegacyWorldSize from = progress == null ? applied : LegacyWorldSize.CODEC.parse(JsonOps.INSTANCE, progress.get("from")).getOrThrow();
            LegacyWorldSize to = progress == null ? selected : LegacyWorldSize.CODEC.parse(JsonOps.INSTANCE, progress.get("to")).getOrThrow();
            if (!SIZES.contains(from) || !SIZES.contains(to) || SIZES.indexOf(to) <= SIZES.indexOf(from)) {
                throw new IllegalStateException("World sizes can only increase between finite presets");
            }
            if (progress != null) {
                boolean overwrite = Codec.BOOL.parse(JsonOps.INSTANCE, progress.get("overwrite")).getOrThrow();
                if (selected == to && applied == to && !LWSWorldOptions.overwriteWorldEdge.get()) {
                    Files.delete(progressPath);
                    return;
                }
                if (applied != from || selected != to || LWSWorldOptions.overwriteWorldEdge.get() != overwrite) throw new IllegalStateException("Finish the pending world size upgrade or restore the original backup before changing its options");
            }

            Map<ResourceKey<Level>, LegacyLevelLimit> oldLimits = collectLevelLimits(applied, server.registryAccess());
            if (progress == null) {
                backup(server, applied, selected, oldLimits);
                progress = new JsonObject();
                progress.add("from", LegacyWorldSize.CODEC.encodeStart(JsonOps.INSTANCE, applied).getOrThrow());
                progress.add("to", LegacyWorldSize.CODEC.encodeStart(JsonOps.INSTANCE, selected).getOrThrow());
                progress.addProperty("overwrite", LWSWorldOptions.overwriteWorldEdge.get());
                write(progressPath, progress);
            } else LegacyWorldSizes.LOGGER.info("Resuming world size upgrade; retaining the original pre-upgrade backup");
            int overwritten = OverworldEdgeDeletion.run(server, oldLimits.get(Level.OVERWORLD), LWSWorldOptions.overwriteWorldEdge.get());
            int walls = NetherWallMigration.run(server, oldLimits.get(Level.NETHER));
            LegacyWorldSizes.LOGGER.info("Expanded world from {} to {}: cleared {} edge chunks and removed old Nether walls in {} chunks", applied.id(), selected.id(), overwritten, walls);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot safely increase the world size; the world has not been opened", error);
        }
    }

    private static LegacyWorldSize inferAppliedSize(RegistryAccess access, LegacyWorldSize selected, boolean savedPreset) {
        if (selected == LegacyWorldSize.CUSTOM) return selected;
        LegacyLevelLimit saved = LWSWorldOptions.legacyLevelLimits.get().get(Level.OVERWORLD);
        if (saved == null) return savedPreset ? selected : LegacyWorldSize.CUSTOM;
        for (LegacyWorldSize size : SIZES) {
            LegacyLevelLimit expected = collectLevelLimits(size, access).get(Level.OVERWORLD);
            if (saved.bounds().size() != expected.bounds().size()) continue;
            boolean matches = true;
            for (int i = 0; i < saved.bounds().size(); i++) {
                LegacyChunkBounds first = saved.bounds().get(i);
                LegacyChunkBounds second = expected.bounds().get(i);
                matches &= first.min().equals(second.min()) && first.max().equals(second.max());
            }
            if (matches) return size;
        }
        throw new IllegalStateException("Cannot determine the original finite world size from its saved bounds");
    }

    private static void backup(MinecraftServer server, LegacyWorldSize applied, LegacyWorldSize selected, Map<ResourceKey<Level>, LegacyLevelLimit> oldLimits) throws IOException {
        boolean overwrite = LWSWorldOptions.overwriteWorldEdge.get();
        Map<ResourceKey<Level>, LegacyLevelLimit> limits = LWSWorldOptions.legacyLevelLimits.get();
        Map<ResourceKey<Level>, LegacyLevelLimit> backupLimits = new HashMap<>(limits);
        backupLimits.put(Level.OVERWORLD, oldLimits.get(Level.OVERWORLD));
        backupLimits.put(Level.NETHER, oldLimits.get(Level.NETHER));
        try {
            LWSWorldOptions.legacyWorldSize.set(applied);
            LWSWorldOptions.legacyLevelLimits.set(backupLimits);
            LWSWorldOptions.overwriteWorldEdge.set(false);
            save();
            long bytes = MinecraftServerAccessor.of(server).getStorageSource().makeWorldBackup();
            LegacyWorldSizes.LOGGER.info("Created a world backup before increasing size from {} to {} ({} bytes)", applied.id(), selected.id(), bytes);
        } finally {
            LWSWorldOptions.legacyWorldSize.set(selected);
            LWSWorldOptions.legacyLevelLimits.set(limits);
            LWSWorldOptions.overwriteWorldEdge.set(overwrite);
            save();
        }
    }

    public static void finish() {
        LegacyWorldSize applied = LWSWorldOptions.appliedLegacyWorldSize.get();
        boolean overwrite = LWSWorldOptions.overwriteWorldEdge.get();
        try {
            LWSWorldOptions.appliedLegacyWorldSize.set(LWSWorldOptions.legacyWorldSize.get());
            LWSWorldOptions.overwriteWorldEdge.set(false);
            save();
            Files.deleteIfExists(LWSWorldOptions.WORLD_STORAGE.file.toPath().resolveSibling("legacy_world_sizes_upgrade.json"));
        } catch (IOException | RuntimeException error) {
            LWSWorldOptions.appliedLegacyWorldSize.set(applied);
            LWSWorldOptions.overwriteWorldEdge.set(overwrite);
            throw new IllegalStateException("Cannot save the completed world size upgrade", error);
        }
    }

    private static void save() throws IOException {
        Path path = LWSWorldOptions.WORLD_STORAGE.file.toPath();
        write(path, LWSWorldOptions.WORLD_STORAGE.encodeConfigs(JsonOps.INSTANCE));
    }

    private static JsonObject read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void write(Path path, JsonElement value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "legacy_world_sizes-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(value, writer);
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Map<ResourceKey<Level>, LegacyLevelLimit> collectLevelLimits(LegacyWorldSize size, RegistryAccess access) {
        Map<ResourceKey<Level>, LegacyLevelLimit> limits = LWSWorldOptions.legacyLevelLimits.get();
        int maxEndGateways = LWSWorldOptions.maxEndGateways.get();
        int endOuterIslandsRay = LWSWorldOptions.endOuterIslandsRay.get();
        BlockPos endSpawnPoint = LWSWorldOptions.endSpawnPoint.get();
        boolean legacyEndSpikes = LWSWorldOptions.legacyEndSpikes.get();
        try {
            size.applier().accept(new LegacyWorldSize.ApplyContext(access));
            return LWSWorldOptions.legacyLevelLimits.get();
        } finally {
            LWSWorldOptions.legacyLevelLimits.set(limits);
            LWSWorldOptions.maxEndGateways.set(maxEndGateways);
            LWSWorldOptions.endOuterIslandsRay.set(endOuterIslandsRay);
            LWSWorldOptions.endSpawnPoint.set(endSpawnPoint);
            LWSWorldOptions.legacyEndSpikes.set(legacyEndSpikes);
        }
    }
}
