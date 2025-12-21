package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPI;
import wily.legacy_world_sizes.LegacyWorldSizes;

import java.util.HashSet;
import java.util.Set;

public record LegacyChunkBounds(ChunkPos min, ChunkPos max, VoxelShape shape) {
    public static final Codec<LegacyChunkBounds> CODEC = RecordCodecBuilder.create(i -> i.group(ChunkPos.CODEC.fieldOf("min").forGetter(LegacyChunkBounds::min), ChunkPos.CODEC.fieldOf("max").forGetter(LegacyChunkBounds::max)).apply(i, LegacyChunkBounds::new));

    public static final ResourceLocation BEDROCK_WALLS_RANDOM = FactoryAPI.createVanillaLocation("bedrock_walls");

    public LegacyChunkBounds(ChunkPos min, ChunkPos max) {
        this(min, max, Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                        min.getMinBlockX(),
                        Double.NEGATIVE_INFINITY,
                        min.getMinBlockZ(),
                        max.getMinBlockX(),
                        Double.POSITIVE_INFINITY,
                        max.getMinBlockZ()
                ),
                BooleanOp.ONLY_FIRST
        ));
    }

    public boolean isInside(BoundingBox boundingBox) {
        return isInside(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ())) && isInside(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));
    }

    public boolean isInside(int x, int z) {
        return x >= min().x && z >= min().z && x < max().x && z < max().z;
    }

    public boolean isInside(double x, double z, double inflate) {
        return x >= min.getMinBlockX() - inflate && z >= min.getMinBlockZ() - inflate && x < max.getMinBlockX() + inflate && z < max.getMinBlockZ() + inflate;
    }

    public boolean isInsideCloseToBorder(Entity entity, AABB aabb) {
        double d = Math.max(Mth.absMax(aabb.getXsize(), aabb.getZsize()), 1.0);
        return distanceTo(entity) < d * 2.0 && isInside(entity.getX(), entity.getZ(), d);
    }

    public double distanceTo(Entity entity) {
        return distanceTo(entity.getX(), entity.getZ());
    }

    public double distanceTo(double x, double z) {
        double f = z - min.getMinBlockZ();
        double g = max.getMinBlockZ() - z;
        double h = x - min.getMinBlockX();
        double i = max.getMinBlockX() - x;
        double j = Math.min(h, i);
        j = Math.min(j, f);
        return Math.min(j, g);
    }

    public boolean isBorder(int x, int z, int add) {
        int addPlus = add + 1;
        return (x == min.x - addPlus || z == min.z - addPlus || x == max.x + add || z == max.z + add) && (x >= min().x - addPlus && z >= min().z - addPlus && x < max().x + addPlus && z < max().z + addPlus);
    }

    public boolean isBorder(int x, int z) {
        return isBorder(x, z, 0);
    }

    public boolean isInsideBorder(int x, int z) {
        return isBorder(x, z, -1);
    }

    public double hyp() {
        return Math.sqrt(Mth.square(max.x - min.x) + Mth.square(max.z - min.z));
    }

    public LegacyChunkBounds move(int x, int z) {
        return new LegacyChunkBounds(new ChunkPos(min.x + x, min.z + z), new ChunkPos(max.x + x, max.z + z));
    }

    public LegacyChunkBounds moveTo(int x, int z) {
        return move(x + (max.x - min.x) / 2 * Mth.sign(x), z + (max.z - min.z) / 2 * Mth.sign(z));
    }

    public ChunkPos middle() {
        return new ChunkPos((min().x + max().x) / 2, (min().z + max().z) / 2);
    }

    public void generateBedrockWalls(ChunkAccess chunkAccess, ChunkGenerator generator, RandomState randomState) {
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(BEDROCK_WALLS_RANDOM);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = generator.getMinY(); y < generator.getMinY() + generator.getGenDepth(); y++) {
            pos.setY(y);
            for (int dx = 0; dx < 16; dx++) {
                pos.setX(chunkAccess.getPos().getMinBlockX() + dx);
                for (int dz = 0; dz < 16; dz++) {
                    pos.setZ(chunkAccess.getPos().getMinBlockZ() + dz);
                    RandomSource randomSource = factory.at(pos);
                    int randomAmount = randomSource.nextInt(5);

                    if (pos.getX() <= min.getMinBlockX() + randomAmount ||
                            pos.getZ() <= min.getMinBlockZ() + randomAmount ||
                            pos.getX() >= max.getMinBlockX() - 1 - randomAmount ||
                            pos.getZ() >= max.getMinBlockZ() - 1 - randomAmount) {
                        chunkAccess.setBlockState(pos.setY(y), Blocks.BEDROCK.defaultBlockState());
                    }
                }
            }
        }
    }

    public BlockPos findOrCreateValidTeleportPos(ServerLevel serverLevel) {
        ChunkPos chunkPos = findExitPortalXZPosTentative(serverLevel);
        LevelChunk levelChunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
        BlockPos blockPos2 = findValidSpawnInChunk(levelChunk);
        if (blockPos2 == null) {
            BlockPos blockPos3 = BlockPos.containing(chunkPos.getMinBlockX() + 0.5, 75.0, chunkPos.getMinBlockZ() + 0.5);
            LegacyWorldSizes.LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", blockPos3);
            serverLevel.registryAccess()
                    .lookup(Registries.CONFIGURED_FEATURE)
                    .flatMap(registry -> registry.get(EndFeatures.END_ISLAND))
                    .ifPresent(reference -> reference.value().place(serverLevel, serverLevel.getChunkSource().getGenerator(), RandomSource.create(blockPos3.asLong()), blockPos3));
            blockPos2 = blockPos3;
        } else {
            LegacyWorldSizes.LOGGER.debug("Found suitable block to teleport to: {}", blockPos2);
        }

        return findTallestBlock(serverLevel, blockPos2, 16, true);
    }

    public ChunkPos findExitPortalXZPosTentative(ServerLevel serverLevel) {
        for (int x = min().x + 1; x < max().x; x++) {
            for (int z = min().z + 1; z < max().z; z++) {
                if (serverLevel.getChunk(x, z).getHighestFilledSectionIndex() != -1)
                    return new ChunkPos(x, z);
            }
        }

        return middle();
    }


    public static BlockPos findTallestBlock(BlockGetter blockGetter, BlockPos blockPos, int i, boolean bl) {
        BlockPos blockPos2 = null;

        for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
                if (j != 0 || k != 0 || bl) {
                    for (int l = blockGetter.getMaxY(); l > (blockPos2 == null ? blockGetter.getMinY() : blockPos2.getY()); l--) {
                        BlockPos blockPos3 = new BlockPos(blockPos.getX() + j, l, blockPos.getZ() + k);
                        BlockState blockState = blockGetter.getBlockState(blockPos3);
                        if (blockState.isCollisionShapeFullBlock(blockGetter, blockPos3) && (bl || !blockState.is(Blocks.BEDROCK))) {
                            blockPos2 = blockPos3;
                            break;
                        }
                    }
                }
            }
        }

        return blockPos2 == null ? blockPos : blockPos2;
    }

    public static LevelChunk getChunk(Level level, Vec3 vec3) {
        return level.getChunk(Mth.floor(vec3.x / 16.0), Mth.floor(vec3.z / 16.0));
    }

    @Nullable
    public static BlockPos findValidSpawnInChunk(LevelChunk levelChunk) {
        ChunkPos chunkPos = levelChunk.getPos();
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), 30, chunkPos.getMinBlockZ());
        int i = levelChunk.getHighestSectionPosition() + 16 - 1;
        BlockPos blockPos2 = new BlockPos(chunkPos.getMaxBlockX(), i, chunkPos.getMaxBlockZ());
        BlockPos blockPos3 = null;
        double d = 0.0;

        for (BlockPos blockPos4 : BlockPos.betweenClosed(blockPos, blockPos2)) {
            BlockState blockState = levelChunk.getBlockState(blockPos4);
            BlockPos blockPos5 = blockPos4.above();
            BlockPos blockPos6 = blockPos4.above(2);
            if (blockState.is(Blocks.END_STONE)
                    && !levelChunk.getBlockState(blockPos5).isCollisionShapeFullBlock(levelChunk, blockPos5)
                    && !levelChunk.getBlockState(blockPos6).isCollisionShapeFullBlock(levelChunk, blockPos6)) {
                double e = blockPos4.distToCenterSqr(0.0, 0.0, 0.0);
                if (blockPos3 == null || e < d) {
                    blockPos3 = blockPos4;
                    d = e;
                }
            }
        }

        return blockPos3;
    }

    public long findBalancedSeed(RegistryAccess registryAccess, int tries) {
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromPreset(registryAccess.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        int actualBiomeCount = 0;
        long actualSeed = 0;

        for (int i = 0; i < tries; i++) {
            long seed = WorldOptions.randomSeed();
            RandomState randomState = RandomState.create(registryAccess, NoiseGeneratorSettings.OVERWORLD, seed);
            Set<Holder<Biome>> set = new HashSet<>();

            int xd = Math.max(1, (max().x - min().x) / 64);
            int zd = Math.max(1, (max().z - min().z) / 64);

            for (int x = min().x; x < max().x; x += xd) {
                for (int z = min().z; z < max().z; z += zd) {
                    set.add(biomeSource.getNoiseBiome(QuartPos.fromSection(x), QuartPos.fromBlock(60), QuartPos.fromSection(z), randomState.sampler()));
                }
            }

            if (set.size() > actualBiomeCount) {
                actualSeed = seed;
                actualBiomeCount = set.size();
            }
        }

        LegacyWorldSizes.LOGGER.debug("Balanced seed with {} biomes", actualBiomeCount);

        return actualSeed;
    }

    public static float getHeightFalloff(int nearestDist) {
        if (nearestDist < 32)
            return (32 - nearestDist) * 0.03125f * 128.0f;

        return 0.0f;
    }

    public int distanceToEdge(float a, int x, int z) {
        Vec3 topLeft = new Vec3(min.x * 16, 0.0f, min.z * 16);
        Vec3 topRight = new Vec3(max.x * 16 - 1, 0.0f, min.z * 16);
        Vec3 bottomLeft = new Vec3(min.x * 16, 0.0f, max.z * 16 - 1);
        Vec3 bottomRight = new Vec3(max.x * 16 - 1, 0.0f, max.z * 16 - 1);

        double distance = a;

        if (((x > (topLeft.x - a)) && (x < (topLeft.x + a))) || ((x > (bottomRight.x - a)) && (x < (bottomRight.x + a)))) {
            distance = LegacyLevelLimit.distanceToSegment(new Vec3(x, 0.0, z), x < 1 ? topLeft : topRight, x < 1 ? bottomLeft : bottomRight);
        }

        if (((z > (topLeft.z - a)) && (z < (topLeft.z + a))) || ((z > (bottomRight.z - a)) && (z < (bottomRight.z + a)))) {
            double verticalDistance = LegacyLevelLimit.distanceToSegment(new Vec3(x, 0.0, z), z < 1 ? topLeft : bottomLeft, z < 1 ? topRight : bottomRight);

            if (verticalDistance < distance)
                distance = verticalDistance;
        }

        return (int) distance;
    }
}
