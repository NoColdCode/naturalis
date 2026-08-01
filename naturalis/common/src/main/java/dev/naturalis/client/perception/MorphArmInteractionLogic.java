package dev.naturalis.client.perception;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

/**
 * First-person arm pose offsets while morphed (paw swipe, peck, etc.).
 */
public final class MorphArmInteractionLogic {

    private MorphArmInteractionLogic() {
    }

    private static void applyPawDigMotion(
        PoseStack poseStack,
        float breakAnim,
        float placeAnim,
        double side,
        float tickTime,
        double breakPhase,
        double placePhase
    ) {
        float rake = (float) Math.max(0.0D, Math.sin(tickTime * 4.2D));
        float alternation = (float) Math.sin(tickTime * 2.1D + (side > 0 ? 0.0D : Math.PI));
        poseStack.translate(
            0.14D * breakAnim * side * alternation,
            -0.12D * breakAnim * rake - 0.03D * breakAnim,
            -0.14D * breakAnim * rake - 0.04D * breakAnim
        );
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (14.0D * breakAnim * side * alternation)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-22.0D * breakAnim * rake - 5.0D * placeAnim * placePhase)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (6.0D * breakAnim * breakPhase * side)));
    }

    public static void applyArmBaseOffset(PoseStack poseStack, double normalization, double side) {
        double t = Math.max(0.0D, Math.min(1.0D, normalization));
        poseStack.translate(0.18D * t * side, -0.06D * t, 0.02D * t);
    }

    public static void applyInteractionMotion(
        PoseStack poseStack,
        MorphArmInteractionStyle style,
        float breakAnim,
        float placeAnim,
        double side,
        float tickTime
    ) {
        if (style == MorphArmInteractionStyle.NONE || (breakAnim < 0.01F && placeAnim < 0.01F)) {
            return;
        }

        double breakPhase = Math.sin(tickTime * 1.7D);
        double placePhase = Math.sin(tickTime * 1.35D + 0.6D);

        switch (style) {
            case SPIDER -> {
                poseStack.translate(0.11D * breakAnim * side * breakPhase, -0.06D * breakAnim, -0.08D * breakAnim);
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (7.5D * breakAnim * breakPhase * side)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-10.0D * breakAnim + -6.0D * placeAnim * placePhase)));
            }
            case CANINE, FELINE -> applyPawDigMotion(poseStack, breakAnim, placeAnim, side, tickTime, breakPhase, placePhase);
            case EQUINE -> {
                poseStack.translate(0.04D * breakAnim * side, -0.08D * breakAnim * Math.abs(breakPhase), -0.10D * breakAnim);
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-13.0D * breakAnim - 5.0D * placeAnim * placePhase)));
            }
            case AVIAN -> {
                poseStack.translate(0.03D * side * breakAnim * breakPhase, -0.03D * breakAnim, -0.12D * (breakAnim + 0.5F * placeAnim * (float) Math.abs(placePhase)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-15.0D * breakAnim - 9.0D * placeAnim * placePhase)));
            }
            case AQUATIC -> {
                poseStack.translate(0.06D * side * (breakAnim + placeAnim) * breakPhase, -0.02D * breakAnim, -0.05D * (breakAnim + placeAnim));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (5.0D * (breakAnim + placeAnim) * breakPhase * side)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-6.0D * breakAnim - 4.0D * placeAnim * placePhase)));
            }
            case GENERIC -> {
                poseStack.translate(0.05D * breakAnim * side * breakPhase, -0.03D * breakAnim, -0.05D * (breakAnim + 0.5F * placeAnim));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-7.0D * breakAnim - 4.0D * placeAnim * placePhase)));
            }
            default -> {
            }
        }
    }

    public static float lerpAnim(float current, boolean active) {
        return Mth.lerp(0.35F, current, active ? 1.0F : 0.0F);
    }

    public static float lerpToward(float current, float target) {
        return Mth.lerp(0.42F, current, target);
    }

    public static DigCameraNudge digCameraNudge(
        MorphArmInteractionStyle style,
        float digAnim,
        float destroyProgress,
        float tickTime,
        float partialTick
    ) {
        if (digAnim < 0.05F || style == MorphArmInteractionStyle.NONE) {
            return DigCameraNudge.NONE;
        }

        float phase = tickTime + partialTick;
        float rake = (float) Math.max(0.0D, Math.sin(phase * 4.2D));
        float intensity = Mth.clamp(digAnim * (0.55F + destroyProgress * 0.45F), 0.0F, 1.0F);

        float bob = (float) Math.sin(phase * 3.6D) * 2.5F * intensity;
        float sway = (float) Math.sin(phase * 1.85D) * 3.2F * intensity;
        // Positive pitch in MC = look down toward the block.
        float lookDown = (5.0F + destroyProgress * 6.0F) * intensity;

        return switch (style) {
            case CANINE, FELINE -> new DigCameraNudge(
                lookDown + rake * 3.0F + bob * 0.5F,
                sway * 0.6F + (float) Math.sin(phase * 2.1D) * 0.8F * intensity,
                rake * 0.05F * intensity
            );
            case SPIDER -> new DigCameraNudge(
                lookDown * 0.75F + rake * 6.0F + bob * 0.6F,
                sway * 1.3F,
                rake * 0.08F * intensity
            );
            case EQUINE -> new DigCameraNudge(
                lookDown * 0.55F + rake * 4.5F,
                sway * 0.45F,
                rake * 0.16F * intensity
            );
            case AVIAN -> new DigCameraNudge(
                lookDown * 0.9F + rake * 7.0F + bob,
                sway * 0.9F,
                rake * 0.03F * intensity
            );
            case AQUATIC -> new DigCameraNudge(
                lookDown * 0.5F + rake * 3.5F,
                sway * 1.1F,
                rake * 0.04F * intensity
            );
            case GENERIC -> new DigCameraNudge(
                lookDown * 0.6F + rake * 5.0F,
                sway * 0.5F,
                rake * 0.14F * intensity
            );
            default -> DigCameraNudge.NONE;
        };
    }

    /** Pitch/yaw offsets in degrees; forward in blocks. */
    public record DigCameraNudge(float pitchDegrees, float yawDegrees, float forwardBlocks) {
        public static final DigCameraNudge NONE = new DigCameraNudge(0.0F, 0.0F, 0.0F);

        public boolean isActive() {
            return pitchDegrees != 0.0F || yawDegrees != 0.0F || forwardBlocks != 0.0F;
        }
    }
}
