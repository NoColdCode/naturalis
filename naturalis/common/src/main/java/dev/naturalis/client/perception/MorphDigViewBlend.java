package dev.naturalis.client.perception;

import dev.naturalis.experience.NaturalisExperienceProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic-style blend from free look toward a dig target (vanilla smooth-camera feel).
 * Only affects camera offset while mining meaningful blocks — not general mouse look.
 */
public final class MorphDigViewBlend {

    /** Pull-in / release duration (similar to holding cinematic camera). */
    public static final float BLEND_SECONDS = 0.55F;

    private static final float MAX_PITCH_NUDGE_SOFT = 14.0F;
    private static final float MAX_YAW_NUDGE_SOFT = 10.0F;
    private static final float NUDGE_STRENGTH_SOFT = 0.42F;

    private static final float MAX_PITCH_NUDGE_REALISTIC = 36.0F;
    private static final float MAX_YAW_NUDGE_REALISTIC = 14.0F;
    private static final float NUDGE_STRENGTH_REALISTIC = 0.92F;

    private static float blend;
    private static float targetPitchOffset;
    private static float targetYawOffset;
    private static float pitchOffset;
    private static float yawOffset;

    private MorphDigViewBlend() {
    }

    public static void reset() {
        blend = 0.0F;
        targetPitchOffset = 0.0F;
        targetYawOffset = 0.0F;
        pitchOffset = 0.0F;
        yawOffset = 0.0F;
    }

    public static float pitchOffset() {
        return pitchOffset;
    }

    public static float yawOffset() {
        return yawOffset;
    }

    public static boolean isMeaningfulDigTarget(Player player, BlockHitResult blockHit) {
        if (player == null || player.level() == null || blockHit == null) {
            return false;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return state.getDestroySpeed(player.level(), pos) > 0.0F;
    }

    public static void tick(Player player, BlockHitResult blockHit, boolean activeDig) {
        float linearStep = 1.0F / (BLEND_SECONDS * 20.0F);

        if (!activeDig || player == null || blockHit == null) {
            blend = Math.max(0.0F, blend - linearStep);
            targetPitchOffset = 0.0F;
            targetYawOffset = 0.0F;
        } else {
            Vec3 eye = player.getEyePosition();
            Vec3 target = blockHit.getLocation();
            double dx = target.x - eye.x;
            double dy = target.y - eye.y;
            double dz = target.z - eye.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);

            // MC pitch: positive = down. Block below eye => dy < 0 => towardPitch > 0.
            float towardPitch = (float) (-Mth.RAD_TO_DEG * Math.atan2(dy, Math.max(horizontal, 1.0E-4D)));
            float towardYaw = (float) Mth.wrapDegrees(Mth.RAD_TO_DEG * Math.atan2(-dx, dz));

            boolean realistic = NaturalisExperienceProfile.useMorphHeadAndCameraClient();
            float maxPitch = realistic ? MAX_PITCH_NUDGE_REALISTIC : MAX_PITCH_NUDGE_SOFT;
            float maxYaw = realistic ? MAX_YAW_NUDGE_REALISTIC : MAX_YAW_NUDGE_SOFT;
            float strength = realistic ? NUDGE_STRENGTH_REALISTIC : NUDGE_STRENGTH_SOFT;

            float viewPitch = MorphAnimalView.embodiedPitch();
            if (Float.isNaN(viewPitch)) {
                viewPitch = player.getXRot();
            }
            float deltaPitch = Mth.clamp(towardPitch - viewPitch, -maxPitch, maxPitch) * strength;
            // Never pull the neck upward when raking a block under the eyes.
            if (dy < -0.05D) {
                deltaPitch = Mth.clamp(deltaPitch, 0.0F, maxPitch);
            } else if (dy > 0.05D) {
                deltaPitch = Mth.clamp(deltaPitch, -maxPitch, 0.0F);
            }
            float viewYaw = MorphAnimalView.embodiedYaw();
            if (Float.isNaN(viewYaw)) {
                viewYaw = player.getYRot();
            }
            float deltaYaw = Mth.clamp(Mth.wrapDegrees(towardYaw - viewYaw), -maxYaw, maxYaw) * strength;

            float intensity = Mth.clamp(
                0.4F + Math.max(MorphDigClientState.destroyProgress(), MorphDigClientState.digAnim() * 0.45F) * 0.55F,
                0.0F,
                1.0F
            );
            targetPitchOffset = deltaPitch * intensity;
            targetYawOffset = deltaYaw * intensity;
            blend = Math.min(1.0F, blend + linearStep);
        }

        float cinematic = smoothStep(blend);
        float follow = cinematicFollowRate(linearStep);
        pitchOffset = Mth.lerp(follow, pitchOffset, targetPitchOffset * cinematic);
        yawOffset = Mth.lerp(follow, yawOffset, targetYawOffset * cinematic);

        if (blend <= 0.001F && Math.abs(pitchOffset) < 0.02F && Math.abs(yawOffset) < 0.02F) {
            reset();
        }
    }

    /** Per-frame interpolation between ticks (cinematic camera cadence). */
    public static void tickPartial(float partialTick) {
        if (blend <= 0.001F && pitchOffset == 0.0F && yawOffset == 0.0F) {
            return;
        }
        float frameFollow = partialTick * (1.0F / BLEND_SECONDS) * 2.8F;
        float cinematic = smoothStep(Math.min(1.0F, blend + partialTick * 0.08F));
        pitchOffset = Mth.lerp(frameFollow, pitchOffset, targetPitchOffset * cinematic);
        yawOffset = Mth.lerp(frameFollow, yawOffset, targetYawOffset * cinematic);
    }

    /** Hermite smoothstep — ease in/out like vanilla cinematic camera. */
    private static float smoothStep(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    /** Slower follow when approaching target, faster when releasing. */
    private static float cinematicFollowRate(float linearStep) {
        return Mth.clamp(linearStep * 3.2F, 0.06F, 0.28F);
    }
}
