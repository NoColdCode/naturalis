package dev.naturalis.client.perception;

/**
 * Client-side embodiment tuning for a morph (camera, FOV, first-person arms).
 */
public record MorphEmbodimentProfile(
    double fovMultiplier,
    double eyeHeightOffset,
    double forwardOffset,
    double firstPersonArmHideStrength,
    MorphArmInteractionStyle armInteractionStyle,
    boolean lowerFirstPersonCamera
) {
    public static final MorphEmbodimentProfile NONE = new MorphEmbodimentProfile(
        1.0D, 0.0D, 0.0D, 0.0D, MorphArmInteractionStyle.NONE, false
    );

    public boolean hasEmbodiment() {
        return Math.abs(fovMultiplier - 1.0D) > 1.0E-6D
            || Math.abs(eyeHeightOffset) > 1.0E-6D
            || Math.abs(forwardOffset) > 1.0E-6D
            || firstPersonArmHideStrength > 1.0E-6D
            || armInteractionStyle != MorphArmInteractionStyle.NONE;
    }

    public MorphEmbodimentProfile withFovMultiplier(double multiplier) {
        return new MorphEmbodimentProfile(
            multiplier,
            eyeHeightOffset,
            forwardOffset,
            firstPersonArmHideStrength,
            armInteractionStyle,
            lowerFirstPersonCamera
        );
    }
}
