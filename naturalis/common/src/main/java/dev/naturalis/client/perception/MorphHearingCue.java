package dev.naturalis.client.perception;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * A directional sound cue from {@link MorphHearingLogic}.
 */
public record MorphHearingCue(
    MorphHearingCueKind kind,
    double bearingDegrees,
    double distance,
    float intensity,
    Component label,
    Entity source
) {
}
