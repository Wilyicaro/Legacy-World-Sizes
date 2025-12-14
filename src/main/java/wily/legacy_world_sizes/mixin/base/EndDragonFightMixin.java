package wily.legacy_world_sizes.mixin.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(EndDragonFight.class)
public abstract class EndDragonFightMixin {
    @Shadow @Final private ObjectArrayList<Integer> gateways;

    @Shadow protected abstract void spawnNewGateway(BlockPos blockPos);

    @Shadow private boolean skipArenaLoadedCheck;

    @Shadow @Final private ServerLevel level;

    @Shadow @Final private BlockPos origin;

    @Inject(method = "spawnNewGateway()V", at = @At("HEAD"), cancellable = true)
    private void spawnNewGateway(CallbackInfo ci) {
        if (!this.gateways.isEmpty()) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);

            int ray = 96;

            if (limit != null) {
                LegacyLevelLimit.ChunkBounds mainBound = limit.bounds().get(0);
                ray = Math.min(Math.min(mainBound.max().x - mainBound.min().x, mainBound.max().z - mainBound.min().z) * 8 - 31, ray);
            }

            int i = this.gateways.remove(this.gateways.size() - 1);
            LegacyWorldSizes.LOGGER.debug("End gateway {} is being spawned", i);
            int j = Mth.floor(ray * Math.cos(2.0 * (-Math.PI + (Math.PI / LWSWorldOptions.maxEndGateways.get()) * i)));
            int k = Mth.floor(ray * Math.sin(2.0 * (-Math.PI + (Math.PI / LWSWorldOptions.maxEndGateways.get()) * i)));
            BlockPos gatewayPos = new BlockPos(j, 75, k);
            if (limit != null && limit.bounds().size() > i) {
                LegacyLevelLimit.ChunkBounds chunkBounds = limit.bounds().get(i + 1);
                BlockPos blockPos2 = chunkBounds.findOrCreateValidTeleportPos(level).above(10);
                Feature.END_GATEWAY.place(EndGatewayConfiguration.knownExit(blockPos2, false), level, level.getChunkSource().getGenerator(), RandomSource.create(), gatewayPos);
                LegacyWorldSizes.LOGGER.debug("Creating portal at {}", blockPos2);
                Feature.END_GATEWAY.place(EndGatewayConfiguration.knownExit(gatewayPos, false), level, level.getChunkSource().getGenerator(), RandomSource.create(), blockPos2);
            } else {
                this.spawnNewGateway(gatewayPos);
            }
        }
        ci.cancel();
    }

    @Inject(method = "isArenaLoaded", at = @At("HEAD"), cancellable = true)
    private void isArenaLoaded(CallbackInfoReturnable<Boolean> cir) {
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);

        if (skipArenaLoadedCheck || limit == null || limit.bounds().isEmpty()) return;
        LegacyLevelLimit.ChunkBounds chunkBounds = limit.bounds().get(0);

        for (int i = chunkBounds.min().x; i < chunkBounds.max().x; i++) {
            for (int j = chunkBounds.min().z; j < chunkBounds.max().z; j++) {
                ChunkAccess chunkAccess = this.level.getChunk(i, j, ChunkStatus.FULL, false);
                if (!(chunkAccess instanceof LevelChunk levelChunk)) {
                    cir.setReturnValue(false);
                    return;
                }

                FullChunkStatus fullChunkStatus = levelChunk.getFullStatus();
                if (!fullChunkStatus.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        cir.setReturnValue(true);
    }
}