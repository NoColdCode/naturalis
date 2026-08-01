package dev.naturalis.client.perception;

import java.util.Set;

/**
 * Hearing perception tuning for a morph (range, prey/threat bias, volume boost).
 */
public record MorphHearingProfile(
    float volumeMultiplier,
    double scanRange,
    int scanIntervalTicks,
    float minCueIntensity,
    Set<String> preyEntityPaths,
    Set<String> threatEntityPaths,
    boolean directionalCues,
    boolean ambientPulse
) {
    public static final MorphHearingProfile NONE = new MorphHearingProfile(
        1.0F, 0.0D, 40, 0.0F, Set.of(), Set.of(), false, false
    );

    public boolean hasEnhancedHearing() {
        return volumeMultiplier > 1.01F || scanRange > 0.5D;
    }
}
