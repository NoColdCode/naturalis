package dev.naturalis.client.perception;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Separates body facing from head look so lateral neck limits feel like a real spine.
 */
public final class MorphBodyYawTracker {

    private static float trackedBodyYaw = Float.NaN;

    private MorphBodyYawTracker() {
    }

    public static void reset() {
        trackedBodyYaw = Float.NaN;
    }

    public static void snapBodyYaw(float yaw) {
        trackedBodyYaw = yaw;
    }

    public static float bodyYaw(Player player) {
        if (player == null) {
            return 0.0F;
        }
        if (Float.isNaN(trackedBodyYaw)) {
            trackedBodyYaw = player.getYRot();
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal > 0.06D) {
            float moveYaw = (float) (Mth.atan2(-velocity.x, velocity.z) * (180.0F / (float) Math.PI));
            float blend = (float) Mth.clamp(horizontal * 2.2D, 0.12D, 0.38D);
            trackedBodyYaw = Mth.rotLerp(blend, trackedBodyYaw, moveYaw);
        } else {
            trackedBodyYaw = Mth.rotLerp(0.1F, trackedBodyYaw, player.getYRot());
        }
        return trackedBodyYaw;
    }
}
