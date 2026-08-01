package dev.naturalis.client.perception;

import dev.naturalis.client.HumanityClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Motion streak / breathing FOV only during heavy sport or when instinct has taken the body — never during scent vision.
 */
public final class MorphMotionVisionClient {

    private static final double HIGH_SPRINT_SPEED_SQR = 0.028D;
    private static final double PRIMAL_BURST_SPEED_SQR = 0.012D;

    private static boolean primalKeyDown;

    private MorphMotionVisionClient() {
    }

    public static void setPrimalKeyDown(boolean down) {
        primalKeyDown = down;
    }

    public static boolean shouldApplyMotionFov() {
        if (MorphSniffClientState.isScentVisionActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return isHighExertion(mc) || isInstinctPushed(mc);
    }

    private static boolean isHighExertion(Minecraft mc) {
        Player player = mc.player;
        if (player == null) {
            return false;
        }
        boolean primal = primalKeyDown;
        boolean sprinting = player.isSprinting();
        Vec3 motion = player.getDeltaMovement();
        double horizontal = motion.x * motion.x + motion.z * motion.z;
        if (sprinting && horizontal >= HIGH_SPRINT_SPEED_SQR) {
            return true;
        }
        return primal && (sprinting || horizontal >= PRIMAL_BURST_SPEED_SQR);
    }

    private static boolean isInstinctPushed(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        if (drift < 0.52F) {
            return false;
        }
        if (HumanityClientCache.isActive() && HumanityClientCache.getHumanity() <= 42) {
            return true;
        }
        return drift >= 0.78F;
    }

    public static float motionFovMultiplier() {
        if (!shouldApplyMotionFov()) {
            return 0.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.0F;
        }
        Vec3 motion = mc.player.getDeltaMovement();
        float speed = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z)) * 20.0F;
        float instinct = Mth.clamp(MorphIdentityDriftClient.embodimentBlend(), 0.0F, 1.0F);
        float sport = Mth.clamp(speed / 5.5F, 0.0F, 1.0F);
        return Mth.clamp(sport * 0.65F + instinct * 0.35F, 0.0F, 1.0F);
    }
}
