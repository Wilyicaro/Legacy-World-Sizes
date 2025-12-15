package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import wily.factoryapi.util.ListMap;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.level.FakeLevelChunk;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public record LegacyWorldSize(ResourceLocation id, Component name, Consumer<ApplyContext> applier) {
    public static final BlockPos LEGACY_END_SPAWN_POINT = new BlockPos(60, 49, 60);

    public static final ListMap<ResourceLocation, LegacyWorldSize> map = new ListMap<>();
    public static final Codec<LegacyWorldSize> CODEC = map.createCodec(ResourceLocation.CODEC);

    public static final LegacyWorldSize CUSTOM = register(new LegacyWorldSize(LegacyWorldSizes.createModLocation("custom"), LegacyWSComponents.optionName("worldSizes.infinity"), ctx -> {}));

    public static final LegacyWorldSize CLASSIC = register(new LegacyWorldSize(LegacyWorldSizes.createModLocation("classic"), LegacyWSComponents.optionName("worldSizes.classic"), ctx -> {
        LWSWorldOptions.legacyLevelLimits.set(Map.of(Level.OVERWORLD, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-27, -27), new ChunkPos(27, 27))), true, getChunkContent(ctx), Optional.of(ctx.registry().getOrThrow(Biomes.PLAINS)), false), Level.NETHER, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-9, -9), new ChunkPos(9, 9))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), true), Level.END, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-6, -6), new ChunkPos(6, 6))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), false)));
        LWSWorldOptions.maxEndGateways.set(1);
        LWSWorldOptions.endOuterIslandsRay.set(18);
        LWSWorldOptions.endSpawnPoint.set(LEGACY_END_SPAWN_POINT);
        LWSWorldOptions.legacyEndSpikes.set(true);
    }));

    public static final LegacyWorldSize SMALL = register(new LegacyWorldSize(LegacyWorldSizes.createModLocation("small"), LegacyWSComponents.optionName("worldSizes.small"), ctx -> {
        LWSWorldOptions.legacyLevelLimits.set(Map.of(Level.OVERWORLD, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-32, -32), new ChunkPos(32, 32))), true, getChunkContent(ctx), Optional.of(ctx.registry().getOrThrow(Biomes.PLAINS)), false), Level.NETHER, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-11, -11), new ChunkPos(11, 11))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), true), Level.END, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-6, -6), new ChunkPos(6, 6))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), false)));
        LWSWorldOptions.maxEndGateways.set(4);
        LWSWorldOptions.endOuterIslandsRay.set(128);
        LWSWorldOptions.endSpawnPoint.set(LEGACY_END_SPAWN_POINT);
        LWSWorldOptions.legacyEndSpikes.set(true);
    }));

    public static final LegacyWorldSize MEDIUM = register(new LegacyWorldSize(LegacyWorldSizes.createModLocation("medium"), LegacyWSComponents.optionName("worldSizes.medium"), ctx -> {
        LWSWorldOptions.legacyLevelLimits.set(Map.of(Level.OVERWORLD, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-96, -96), new ChunkPos(96, 96))), true, getChunkContent(ctx), Optional.of(ctx.registry().getOrThrow(Biomes.PLAINS)), false), Level.NETHER, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-16, -16), new ChunkPos(16, 16))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), true), Level.END, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-6, -6), new ChunkPos(6, 6))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), false)));
        LWSWorldOptions.maxEndGateways.set(4);
        LWSWorldOptions.endOuterIslandsRay.set(128);
        LWSWorldOptions.endSpawnPoint.set(LEGACY_END_SPAWN_POINT);
        LWSWorldOptions.legacyEndSpikes.set(true);
    }));

    public static final LegacyWorldSize LARGE = register(new LegacyWorldSize(LegacyWorldSizes.createModLocation("large"), LegacyWSComponents.optionName("worldSizes.large"), ctx -> {
        LWSWorldOptions.legacyLevelLimits.set(Map.of(Level.OVERWORLD, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-160, -160), new ChunkPos(160, 160))), true, getChunkContent(ctx), Optional.of(ctx.registry().getOrThrow(Biomes.PLAINS)), false), Level.NETHER, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-20, -20), new ChunkPos(20, 20))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), true), Level.END, new LegacyLevelLimit(List.of(new LegacyChunkBounds(new ChunkPos(-6, -6), new ChunkPos(6, 6))), false, FakeLevelChunk.ContentType.NONE, Optional.empty(), false)));
        LWSWorldOptions.maxEndGateways.set(4);
        LWSWorldOptions.endOuterIslandsRay.set(128);
        LWSWorldOptions.endSpawnPoint.set(LEGACY_END_SPAWN_POINT);
        LWSWorldOptions.legacyEndSpikes.set(true);
    }));

    public static FakeLevelChunk.ContentType getChunkContent(ApplyContext ctx) {
        return isOverworldFlat(ctx.registry()) ? FakeLevelChunk.ContentType.FLAT : FakeLevelChunk.ContentType.OCEAN;
    }

    public static boolean isOverworldFlat(RegistryAccess access) {
        return access.getOrThrow(LevelStem.OVERWORLD).value().generator() instanceof FlatLevelSource;
    }

    public record ApplyContext(RegistryAccess registry) {
    }

    public static LegacyWorldSize register(LegacyWorldSize worldSize) {
        map.put(worldSize.id(), worldSize);
        return worldSize;
    }
}
