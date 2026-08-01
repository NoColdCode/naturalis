package dev.naturalis.client.perception;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public record MorphVibrationCue(
    MorphVibrationCueKind kind,
    double bearingDegrees,
    double distance,
    float intensity,
    Component feltMessage,
    @Nullable Entity source
) {
}
