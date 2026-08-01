package dev.naturalis.client.perception;

/**
 * Per-archetype neck pitch/yaw limits (MC pitch: positive = down, negative = up).
 */
public record MorphHeadLimits(
    float maxLookUpStand,
    float maxLookUpCrouch,
    float maxLookDown,
    float freeLookUpHorizon,
    float upwardRateStand,
    float upwardRateCrouch,
    float maxNeckYawOffset,
    float maxYawRatePerTick,
    float digPitchMultiplier
) {
    public static final MorphHeadLimits NONE = new MorphHeadLimits(
        0.0F, 0.0F, 90.0F, 0.0F, 90.0F, 90.0F, 180.0F, 180.0F, 0.0F
    );

    public boolean applies(MorphArmInteractionStyle style) {
        return style != MorphArmInteractionStyle.NONE;
    }

    public static MorphHeadLimits forStyle(MorphArmInteractionStyle style) {
        return switch (style) {
            case CANINE -> new MorphHeadLimits(
                -58.0F, -82.0F, 78.0F,
                -20.0F, 1.15F, 3.8F,
                58.0F, 12.0F, 2.4F
            );
            case FELINE -> new MorphHeadLimits(
                -52.0F, -75.0F, 76.0F,
                -18.0F, 1.35F, 4.2F,
                62.0F, 14.0F, 2.2F
            );
            case EQUINE -> new MorphHeadLimits(
                -34.0F, -48.0F, 62.0F,
                -10.0F, 0.85F, 2.4F,
                38.0F, 8.0F, 1.8F
            );
            case SPIDER -> new MorphHeadLimits(
                -22.0F, -32.0F, 52.0F,
                -8.0F, 0.65F, 1.8F,
                44.0F, 10.0F, 2.8F
            );
            case AVIAN -> new MorphHeadLimits(
                -88.0F, -92.0F, 48.0F,
                -35.0F, 2.8F, 4.5F,
                72.0F, 18.0F, 1.6F
            );
            case AQUATIC -> new MorphHeadLimits(
                -42.0F, -55.0F, 58.0F,
                -14.0F, 1.05F, 2.6F,
                48.0F, 9.0F, 1.4F
            );
            case GENERIC -> new MorphHeadLimits(
                -48.0F, -72.0F, 78.0F,
                -16.0F, 1.05F, 3.2F,
                52.0F, 11.0F, 2.0F
            );
            default -> NONE;
        };
    }
}
