package dev.naturalis.client;

import dev.naturalis.experience.NaturalisExperienceMode;

public final class ExperienceModeClientCache {

    private ExperienceModeClientCache() {
    }

    public static NaturalisExperienceMode getEffectiveMode() {
        return NaturalisExperienceMode.REALISTIC;
    }
}
