package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import wily.legacy_world_sizes.level.FakeLevelChunk;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LegacyLevelLimit(List<LegacyChunkBounds> bounds, boolean heightFallOff, FakeLevelChunk.ContentType content, Optional<Holder<Biome>> fixedBiome, boolean bedrockBarrier) {
    public static final Codec<LegacyLevelLimit> CODEC = RecordCodecBuilder.create(i -> i.group(LegacyChunkBounds.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("bounds").forGetter(LegacyLevelLimit::bounds), Codec.BOOL.fieldOf("borderBlending").orElse(true).forGetter(LegacyLevelLimit::heightFallOff), FakeLevelChunk.ContentType.CODEC.fieldOf("content").orElse(FakeLevelChunk.ContentType.OCEAN).forGetter(LegacyLevelLimit::content), Biome.CODEC.optionalFieldOf("fixedBiome").forGetter(LegacyLevelLimit::fixedBiome), Codec.BOOL.fieldOf("bedrockBarrier").orElse(false).forGetter(LegacyLevelLimit::bedrockBarrier)).apply(i, LegacyLevelLimit::new));

    public static final Codec<Map<ResourceKey<Level>, LegacyLevelLimit>> MAP_CODEC = Codec.dispatchedMap(Level.RESOURCE_KEY_CODEC, l -> CODEC);

    public static Vec3 closestPointOnSegment(Vec3 actual, Vec3 segStart, Vec3 segEnd)
    {
        Vec3 v1 = actual.subtract(segStart);
        Vec3 v2 = segEnd.subtract(segStart);

        double dot = v1.dot(v2);
        if (dot <= 0.0)
            return segStart;

        double len2 = v2.dot(v2);
        if (len2 <= dot)
            return segEnd;

        double t = dot / len2;

        return segStart.add(v2.multiply(t, t, t));
    }

    public static double distanceToSegment(Vec3 actual, Vec3 segStart, Vec3 segEnd) {
        return actual.subtract(closestPointOnSegment(actual, segStart, segEnd)).length();
    }

    public LegacyLevelLimit withBounds(List<LegacyChunkBounds> bounds) {
        return new LegacyLevelLimit(bounds, heightFallOff, content, fixedBiome, bedrockBarrier);
    }

}
