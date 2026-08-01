package dev.naturalis.client.perception;

/**
 * Paw vibration sense (ground tremors through pads, separate from hearing).
 */
public record MorphVibrationProfile(
    double scanRange,
    int scanIntervalTicks,
    float minIntensity,
    float actionBarThreshold,
    float cameraTremorScale
) {
    public static final MorphVibrationProfile NONE = new MorphVibrationProfile(0.0D, 40, 0.0F, 1.0F, 0.0F);

    public boolean hasPawVibrationSense() {
        return scanRange > 0.5D;
    }
}
