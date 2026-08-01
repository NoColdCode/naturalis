package dev.naturalis.client;

import dev.naturalis.client.perception.MorphSniffClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Colors for scent-vision mob rendering (flat fill + ribbon outline).
 */
public final class ScentVisionTintState {

    /** Pre-filter tone chosen to match the blue-gray scent-vision world after post-processing. */
    private static final int FILTER_FILL_ARGB = 0xFFC9CDD6;
    private static final double POSITION_MATCH_SQR = 0.35D;

    private ScentVisionTintState() {
    }

    public static boolean shouldStyleScentedMobs() {
        return MorphSniffClientState.isScentVisionActive() || ScentTrailClient.hasDeepRibbons();
    }

    public static int filterFillArgb() {
        return FILTER_FILL_ARGB;
    }

    public static void applyTint(EntityRenderStateScentAccess access, LivingEntityRenderState state) {
        resolveOutline(access, state).ifPresentOrElse(
            access::naturalis$setScentTintArgb,
            () -> access.naturalis$setScentTintArgb(-1)
        );
    }

    public static Optional<Integer> resolveOutline(EntityRenderStateScentAccess access, LivingEntityRenderState state) {
        if (!shouldStyleScentedMobs()) {
            return Optional.empty();
        }
        int entityId = access.naturalis$getScentEntityId();
        if (entityId >= 0) {
            Optional<Integer> fromId = outlineForEntityId(entityId);
            if (fromId.isPresent()) {
                return fromId;
            }
        }
        return outlineForPosition(state);
    }

    public static Optional<Integer> outlineForEntity(int entityId) {
        if (!shouldStyleScentedMobs()) {
            return Optional.empty();
        }
        return outlineForEntityId(entityId);
    }

    private static Optional<Integer> outlineForEntityId(int entityId) {
        return ScentTrailClient.scentCategoryForEntity(entityId).map(ScentVisionTintState::outlineArgb);
    }

    private static Optional<Integer> outlineForPosition(LivingEntityRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Optional.empty();
        }
        for (ScentTrailClient.ScentRibbon ribbon : ScentTrailClient.activeRibbons()) {
            Entity entity = mc.level.getEntity(ribbon.key());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (living.distanceToSqr(state.x, state.y, state.z) > POSITION_MATCH_SQR) {
                continue;
            }
            return Optional.of(outlineArgb(ribbon.category()));
        }
        return Optional.empty();
    }

    /** Ribbon category color for silhouette outlines — flat, not emissive. */
    public static int outlineArgb(byte category) {
        return ScentTrailClient.ribbonColor(category, 1.0F) | 0xFF000000;
    }

    public static Optional<Integer> modelTintForEntity(int entityId) {
        return outlineForEntity(entityId);
    }
}
