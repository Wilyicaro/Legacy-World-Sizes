package wily.legacy_world_sizes.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

import java.util.List;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
    private void legacy_world_sizes$cullSeparatedEndBounds(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.dimension() != Level.END) return;

        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);
        if (limit == null || limit.bounds().size() < 2) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        int currentBoundIndex = legacy_world_sizes$findBoundIndex(limit, cameraPos.x, cameraPos.z);
        if (currentBoundIndex < 0) return;

        visibleSections.removeIf(section -> legacy_world_sizes$isOutsideCurrentEndBound(limit, currentBoundIndex, section));
        nearbyVisibleSections.removeIf(section -> legacy_world_sizes$isOutsideCurrentEndBound(limit, currentBoundIndex, section));
    }

    @Unique
    private static int legacy_world_sizes$findBoundIndex(LegacyLevelLimit limit, double x, double z) {
        for (int i = 0; i < limit.bounds().size(); i++) {
            if (limit.bounds().get(i).isInside(x, z, 0)) return i;
        }
        return -1;
    }

    @Unique
    private static int legacy_world_sizes$findBoundIndex(LegacyLevelLimit limit, int sectionX, int sectionZ) {
        for (int i = 0; i < limit.bounds().size(); i++) {
            LegacyChunkBounds bound = limit.bounds().get(i);
            if (bound.isInside(sectionX, sectionZ)) return i;
        }
        return -1;
    }

    @Unique
    private static boolean legacy_world_sizes$isOutsideCurrentEndBound(LegacyLevelLimit limit, int currentBoundIndex, SectionRenderDispatcher.RenderSection section) {
        int targetBoundIndex = legacy_world_sizes$findBoundIndex(limit, SectionPos.x(section.getSectionNode()), SectionPos.z(section.getSectionNode()));
        return targetBoundIndex >= 0 && targetBoundIndex != currentBoundIndex;
    }
}
