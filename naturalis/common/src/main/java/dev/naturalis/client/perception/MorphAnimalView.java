package dev.naturalis.client.perception;

import dev.naturalis.experience.NaturalisExperienceProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Animal neck model: player input stays free for interaction; embodied pitch/yaw feed the camera.
 * MC pitch: positive = down, negative = up. Horizon = 0°, 20° above horizon = -20°.
 */
public final class MorphAnimalView {

    private static float embodiedPitch = Float.NaN;
    private static float embodiedYaw = Float.NaN;

    private MorphAnimalView() {
    }

    public static void reset() {
        embodiedPitch = Float.NaN;
        embodiedYaw = Float.NaN;
        MorphBodyYawTracker.reset();
    }

    public static float constrainNeckPitch(Player player, MorphEmbodimentProfile profile) {
        if (!NaturalisExperienceProfile.useMorphHeadAndCameraClient()) {
            reset();
            return player.getXRot();
        }

        MorphHeadLimits limits = MorphHeadLimits.forStyle(profile.armInteractionStyle());
        if (!limits.applies(profile.armInteractionStyle())) {
            reset();
            return player.getXRot();
        }

        boolean crouching = player.isCrouching() || player.isPassenger();
        float maxUp = crouching ? limits.maxLookUpCrouch() : limits.maxLookUpStand();
        float upwardRate = crouching ? limits.upwardRateCrouch() : limits.upwardRateStand();

        float input = player.getXRot();
        float previous = Float.isNaN(embodiedPitch) ? input : embodiedPitch;

        float pitch = Mth.clamp(input, maxUp, limits.maxLookDown());

        if (pitch < limits.freeLookUpHorizon()) {
            float delta = pitch - previous;
            if (delta < -upwardRate) {
                pitch = previous - upwardRate;
            }
            pitch = Math.max(pitch, maxUp);
        }

        embodiedPitch = pitch;
        return pitch;
    }

    /**
     * Head yaw relative to body facing; player {@code yRot} stays free for movement.
     */
    public static float constrainNeckYaw(Player player, MorphEmbodimentProfile profile) {
        if (!NaturalisExperienceProfile.useMorphHeadAndCameraClient()) {
            return player.getYRot();
        }

        MorphHeadLimits limits = MorphHeadLimits.forStyle(profile.armInteractionStyle());
        if (!limits.applies(profile.armInteractionStyle())) {
            return player.getYRot();
        }

        float bodyYaw = MorphBodyYawTracker.bodyYaw(player);
        float input = player.getYRot();
        float previous = Float.isNaN(embodiedYaw) ? input : embodiedYaw;

        float delta = Mth.wrapDegrees(input - previous);
        delta = Mth.clamp(delta, -limits.maxYawRatePerTick(), limits.maxYawRatePerTick());
        float yaw = Mth.wrapDegrees(previous + delta);

        float offset = Mth.wrapDegrees(yaw - bodyYaw);
        offset = Mth.clamp(offset, -limits.maxNeckYawOffset(), limits.maxNeckYawOffset());
        yaw = Mth.wrapDegrees(bodyYaw + offset);

        embodiedYaw = yaw;
        return yaw;
    }

    public static float applyDigNeckPitch(float neckPitch, MorphEmbodimentProfile profile) {
        if (!NaturalisExperienceProfile.useMorphHeadAndCameraClient()) {
            return neckPitch;
        }

        MorphHeadLimits limits = MorphHeadLimits.forStyle(profile.armInteractionStyle());
        if (!limits.applies(profile.armInteractionStyle()) || limits.digPitchMultiplier() <= 0.0F) {
            return neckPitch;
        }

        float dig = MorphDigViewBlend.pitchOffset() * limits.digPitchMultiplier();
        return Mth.clamp(neckPitch + dig, limits.maxLookUpStand(), limits.maxLookDown());
    }

    public static void setEmbodiedPitch(float pitch) {
        embodiedPitch = pitch;
    }

    public static float embodiedPitch() {
        return embodiedPitch;
    }

    public static float embodiedYaw() {
        return embodiedYaw;
    }

    public static void setEmbodiedYaw(float yaw) {
        embodiedYaw = yaw;
    }

    public static float cameraSniffPitchOffset(Player player, MorphEmbodimentProfile profile, float partialTick) {
        if (!MorphGaitLogic.usesStyleGait(profile)) {
            return 0.0F;
        }
        Vec3 velocity = player.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < 0.02D && !MorphGaitLogic.usesIdleHeadBob(profile)) {
            return 0.0F;
        }
        var gait = MorphGaitLogic.computeGaitOffsets(player, profile, partialTick);
        return gait.pitch() * MorphGaitLogic.cameraPitchScale(profile);
    }

    public static float cameraSwayYawOffset(Player player, MorphEmbodimentProfile profile, float partialTick) {
        if (!MorphGaitLogic.usesStyleGait(profile)) {
            return 0.0F;
        }
        var gait = MorphGaitLogic.computeGaitOffsets(player, profile, partialTick);
        return gait.yaw() * MorphGaitLogic.cameraYawScale(profile);
    }

    public static float cameraRollDegrees(Player player, MorphEmbodimentProfile profile, float partialTick) {
        if (!MorphGaitLogic.usesStyleGait(profile)) {
            return 0.0F;
        }
        var gait = MorphGaitLogic.computeGaitOffsets(player, profile, partialTick);
        float drift = MorphIdentityDriftClient.embodimentBlend();
        return gait.roll() * (0.4F + drift * 0.25F);
    }
}
