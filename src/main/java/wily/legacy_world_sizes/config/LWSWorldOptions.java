package wily.legacy_world_sizes.config;

import com.google.common.collect.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.mixin.base.*;
import wily.legacy_world_sizes.util.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class LWSWorldOptions {
    public static final FactoryConfig.StorageHandler WORLD_STORAGE = new FactoryConfig.StorageHandler(true) {
        @Override
        public void save() {
            if (file == null) return;
            super.save();
        }

        @Override
        public <T> DataResult<T> decode(FactoryConfigControl<T> control, Consumer<T> setter, Dynamic<?> dynamic) {
            return super.decode(control, setter, convertToRegistryIfPossible(dynamic));
        }

        @Override
        public <T, E> DataResult<E> encode(FactoryConfigControl<T> control, T value, DynamicOps<E> ops) {
            return super.encode(control, value, createRegistryOps(ops));
        }

        @Override
        public <T> void decodeConfigs(Dynamic<T> dynamic) {
            super.decodeConfigs(convertToRegistryIfPossible(dynamic));
        }

        @Override
        public <T> T encodeConfigs(DynamicOps<T> ops) {
            return super.encodeConfigs(createRegistryOps(ops));
        }
    };

    public static <T> Dynamic<T> convertToRegistryIfPossible(Dynamic<T> dynamic) {
        return dynamic.convert(createRegistryOps(dynamic.getOps()));
    }

    public static <T> DynamicOps<T> createRegistryOps(DynamicOps<T> ops) {
        MinecraftServer server = FactoryAPI.currentServer;
        return server == null ? ops : RegistryOps.create(ops, server.registryAccess());
    }

    public static final List<SpikeFeature.EndSpike> LEGACY_END_SPIKES = createLegacyEndSpikes(8);

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer, FactoryConfigDisplay.Builder<T> builder) {
        return consumer.apply(new FactoryConfig.Builder<>()).displayFromKey(t -> builder.build(LegacyWSComponents.optionName(t))).buildAndRegister(WORLD_STORAGE);
    }

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer) {
        return buildAndRegister(consumer, FactoryConfigDisplay.builder());
    }

    public static final FactoryConfig<Map<ResourceKey<Level>, LegacyLevelLimit>> legacyLevelLimits = buildAndRegister(b -> b.key("legacyLevelLimits").control(() -> LegacyLevelLimit.MAP_CODEC).defaultValue(Collections.emptyMap()));

    public static final FactoryConfig<Integer> maxEndGateways = buildAndRegister(b -> b.key("maxEndGateways").control(FactoryConfigControl.of(Codec.INT)).defaultValue(20));

    public static final FactoryConfig<Integer> endOuterIslandsRay = buildAndRegister(b -> b.key("endOuterIslandsRay").control(FactoryConfigControl.of(Codec.INT)).defaultValue(64));

    public static final FactoryConfig<BlockPos> endSpawnPoint = buildAndRegister(b -> b.key("endSpawnPoint").control(FactoryConfigControl.of(BlockPos.CODEC)).defaultValue(ServerLevel.END_SPAWN_POINT));

    public static final FactoryConfig<Boolean> legacyEndSpikes = buildAndRegister(b -> b.key("legacyEndSpikes").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false));

    public static final FactoryConfig<Boolean> balancedSeed = buildAndRegister(b -> b.key("balancedSeed").control(FactoryConfigControl.TOGGLE).defaultValue(true), FactoryConfigDisplay.toggleBuilder().tooltip(LegacyWSComponents.staticTooltip(LegacyWSComponents.optionName("balancedSeed.description"))));

    public static final FactoryConfig<LegacyWorldSize> legacyWorldSize = buildAndRegister(b -> b.key("legacyWorldSize").control(new FactoryConfigControl.FromInt<>(LegacyWorldSize.CODEC, LegacyWorldSize.map::getByIndex, LegacyWorldSize.map::indexOf, LegacyWorldSize.map::size)).defaultValue(LegacyWorldSize.CUSTOM), FactoryConfigDisplay.<LegacyWorldSize>builder().valueToComponent(LegacyWorldSize::name).tooltip(LegacyWSComponents.staticTooltip(LegacyWSComponents.optionName("legacyWorldSize.description"))));

    public static final FactoryConfig<LegacyBiomeScale.AddOctave> addToBiomeFirstOctave = buildAndRegister(b -> b.key("addToBiomeFirstOctave").control(FactoryConfigControl.of(LegacyBiomeScale.AddOctave.CODEC)).defaultValue(LegacyBiomeScale.AddOctave.ZERO));

    public static final FactoryConfig<LegacyBiomeScale> legacyBiomeScale = buildAndRegister(b -> b.key("legacyBiomeScale").control(new FactoryConfigControl.FromInt<>(LegacyBiomeScale.CODEC, LegacyBiomeScale.map::getByIndex, LegacyBiomeScale.map::indexOf, LegacyBiomeScale.map::size)).defaultValue(LegacyBiomeScale.CUSTOM), FactoryConfigDisplay.<LegacyBiomeScale>builder().valueToComponent(LegacyBiomeScale::name).tooltip(LegacyWSComponents.staticTooltip(LegacyWSComponents.optionName("legacyBiomeScale.description"))));


    public static boolean isValidChunk(LevelChunk chunk) {
        return isValidPos(chunk.getLevel().dimension(), chunk.getPos());
    }

    public static boolean isValidChunk(LevelHeightAccessor accessor, ChunkAccess access) {
        if (accessor instanceof Level level) {
            return isValidPos(level.dimension(), access.getPos());
        }
        return true;
    }

    public static boolean isValidChunk(StructureManager manager, ChunkAccess access) {
        if ((((StructureManagerAccessor) manager).getLevel() instanceof WorldGenRegion level)) {
            return isValidPos(level.getLevel().dimension(), access.getPos());
        }
        return true;
    }

    public static boolean isValidPos(ResourceKey<Level> level, int x, int z) {
        LegacyLevelLimit limit = legacyLevelLimits.get().get(level);

        if (limit != null && !limit.bounds().isEmpty()) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                if (bounds.isInside(x, z)) {
                    return true;
                }
            }

            return false;
        }
        return true;
    }

    public static boolean isValidPos(ResourceKey<Level> level, ChunkPos pos) {
        return isValidPos(level, pos.x, pos.z);
    }

    public static boolean isValidPos(ResourceKey<Level> level, BlockPos pos) {
        return isValidPos(level, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static boolean isBorder(ResourceKey<Level> level, int x, int z) {
        LegacyLevelLimit limit = legacyLevelLimits.get().get(level);

        if (limit != null && !limit.bounds().isEmpty()) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                if (bounds.isBorder(x, z)) {
                    return true;
                }
            }

            return false;
        }
        return true;
    }

    public static boolean isBorder(ResourceKey<Level> level, ChunkPos pos) {
        return isBorder(level, pos.x, pos.z);
    }

    public static double getTeleportationScale(ResourceKey<Level> from, ResourceKey<Level> to, double original) {
        LegacyLevelLimit limitFrom = legacyLevelLimits.get().get(from);
        LegacyLevelLimit limitTo = legacyLevelLimits.get().get(to);

        if (limitFrom == null || limitTo == null) return original;

        LegacyChunkBounds boundsFrom = limitFrom.bounds().get(0);
        LegacyChunkBounds boundsTo = limitTo.bounds().get(0);

        double hypFrom = boundsFrom.hyp();
        double hypTo = boundsTo.hyp();

        return hypTo < hypFrom ? Math.max(1d / Math.round(hypFrom / hypTo), original) : Math.min(Math.round(hypTo / hypFrom), original);
    }

    public static void restoreChangedDefaults() {
        balancedSeed.setDefault(true);
        legacyWorldSize.setDefault(LegacyWorldSize.CUSTOM);
        legacyBiomeScale.setDefault(LegacyBiomeScale.CUSTOM);
        balancedSeed.reset();
        legacyWorldSize.reset();
        legacyBiomeScale.reset();
    }

    public static void setupLegacyWorldSize(RegistryAccess access) {
        legacyWorldSize.get().applier().accept(new LegacyWorldSize.ApplyContext(access));
        legacyBiomeScale.get().applyToAddToBiomeFirstOctave();
        addToBiomeFirstOctave.get().applyToBiomeNoiseParameters(access);
    }

    public static void setupDedicatedServerBalancedSeed(DedicatedServer server) {
        if (balancedSeed.get() && server.getProperties() instanceof SettingsAccessor settings && WorldOptions.parseSeed(settings.getString("level-seed", "")).isEmpty()) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.OVERWORLD);
            if (limit != null && server.getProperties() instanceof DedicatedServerPropertiesAccessor accessor) {
                LegacyChunkBounds bounds = limit.bounds().get(0);
                accessor.setWorldOptions(server.getProperties().worldOptions.withSeed(OptionalLong.of(bounds.findBalancedSeed(server.registryAccess(), 100))));
            }
        }
    }

    public static void setupEndLimits(MinecraftServer server) {
        int max = maxEndGateways.get();
        LegacyLevelLimit limit = legacyLevelLimits.get().get(Level.END);

        if (limit != null && !limit.bounds().isEmpty() && (limit.bounds().size() - 1) != max) {
            ImmutableList.Builder<LegacyChunkBounds> bounds = ImmutableList.builder();
            LegacyChunkBounds chunkBounds = limit.bounds().get(0);
            bounds.add(chunkBounds);

            if (max > 0) {
                int ray = Math.max(endOuterIslandsRay.get(), Math.round((11.28f + (float) chunkBounds.hyp()) * max / Mth.TWO_PI));
                LegacyWorldSizes.LOGGER.debug("The end outer islands ray is: {}", ray);
                if (ray != endOuterIslandsRay.get()) {
                    LegacyWorldSizes.LOGGER.debug("Adjusting saved end outer islands ray from {} to {}", endOuterIslandsRay.get(), ray);
                    endOuterIslandsRay.set(ray);
                }
                for (int i = 0; i < max; i++) {
                    double dist = 2.0 * Math.PI * i / max;
                    int x = Math.round(ray * (float) Math.cos(dist));
                    int z = Math.round(ray * (float) Math.sin(dist));
                    LegacyWorldSizes.LOGGER.debug("Moving end bounds {} to: {}, {}", i, x, z);
                    bounds.add(chunkBounds.moveTo(x, z));
                }
            }
            legacyLevelLimits.set(ImmutableMap.<ResourceKey<Level>, LegacyLevelLimit>builder().putAll(legacyLevelLimits.get()).put(Level.END, limit = limit.withBounds(bounds.build())).buildKeepingLast());
            legacyLevelLimits.save();
        }

        ServerLevel end = server.getLevel(Level.END);

        if (limit != null && end != null && end.getChunkSource().getGeneratorState() instanceof ChunkGeneratorStructureStateAccessor accessor) {
            ImmutableList.Builder<Holder<StructureSet>> structures = ImmutableList.<Holder<StructureSet>>builder().addAll(end.getChunkSource().getGeneratorState().possibleStructureSets());
            for (int i = 1; i < limit.bounds().size(); i++) {
                LegacyChunkBounds bound = limit.bounds().get(i);
                ChunkPos middle = bound.middle();
                structures.add(Holder.direct(new StructureSet(server.registryAccess().getOrThrow(BuiltinStructureSets.END_CITIES).value().structures(), new RandomSpreadStructurePlacement(1, 1, RandomSpreadType.LINEAR, 10387313) {
                    @Override
                    public ChunkPos getPotentialStructureChunk(long l, int i, int j) {
                        return middle;
                    }
                })));
                accessor.setHasGeneratedPositions(false);
                accessor.getPlacementsForStructure().clear();
            }

            accessor.setPossibleStructureSets(structures.build());
        }

        if (LWSWorldOptions.maxEndGateways.get() != 20 && end != null && end.getDragonFight() instanceof EndDragonFightAccessor accessor && end.getServer().getWorldData().endDragonFightData().gateways().isEmpty()) {
            accessor.getGateways().clear();

            if (LWSWorldOptions.maxEndGateways.get() == 1) accessor.getGateways().add(0);
            else {
                accessor.getGateways().addAll(ContiguousSet.create(Range.closedOpen(0, LWSWorldOptions.maxEndGateways.get()), DiscreteDomain.integers()));
                Util.shuffle(accessor.getGateways(), RandomSource.create(end.getSeed()));
            }
        }
    }

    public static void setupStrongholdValidPlacement(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        LegacyLevelLimit limit = legacyLevelLimits.get().get(Level.OVERWORLD);

        if (limit != null && overworld.getChunkSource().getGeneratorState() instanceof ChunkGeneratorStructureStateAccessor accessor) {
            final int minDist = -96;
            final int maxDist = 96;

            for (LegacyChunkBounds bound : limit.bounds()) {
                if (bound.min().x > minDist || bound.min().z > minDist || bound.max().x < maxDist || bound.max().z < maxDist) {
                    Holder<StructureSet> strongholds = server.registryAccess().getOrThrow(BuiltinStructureSets.STRONGHOLDS);
                    List<Holder<StructureSet>> structures = new ArrayList<>(overworld.getChunkSource().getGeneratorState().possibleStructureSets());
                    if (structures.remove(strongholds)) {
                        structures.add(Holder.direct(new StructureSet(strongholds.value().structures(), new RandomSpreadStructurePlacement(1, 1, RandomSpreadType.LINEAR, 10387313) {
                            @Override
                            public ChunkPos getPotentialStructureChunk(long l, int i, int j) {
                                RandomSource random = RandomSource.create(accessor.getConcentricRingsSeed());
                                return new ChunkPos(random.nextInt(bound.min().x + 2, bound.max().x - 2), random.nextInt(bound.min().z + 2, bound.max().z - 2));
                            }

                            @Override
                            public boolean isStructureChunk(ChunkGeneratorStructureState chunkGeneratorStructureState, int i, int j) {
                                return isPlacementChunk(chunkGeneratorStructureState, i, j);
                            }
                        })));
                    }
                    accessor.setHasGeneratedPositions(false);
                    accessor.getPlacementsForStructure().clear();
                    accessor.setPossibleStructureSets(ImmutableList.<Holder<StructureSet>>builder().addAll(structures).build());
                    return;
                }
            }
        }
    }

    public static List<SpikeFeature.EndSpike> createLegacyEndSpikes(int amount) {
        ImmutableList.Builder<SpikeFeature.EndSpike> builder = ImmutableList.builder();

        for (int i = 0; i < amount; i++) {
            double ang = 2.0 * (-Math.PI + (Math.PI / amount) * i);
            int x = Mth.floor(42.0 * Math.cos(ang));
            int z = Mth.floor(42.0 * Math.sin(ang));
            int width = 2 + i / 3;
            int height = 73 + i * 3;
            builder.add(new SpikeFeature.EndSpike(x, z, width, height, i >= amount - 2));
        }

        return builder.build();
    }
}
