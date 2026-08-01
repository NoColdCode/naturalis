package dev.naturalis.experience;

/**
 * Per-world Naturalis presentation: full embodiment vs comfort-oriented.
 */
public enum NaturalisExperienceMode {
    /** Not chosen yet — defaults to {@link #REALISTIC} until the player confirms. */
    UNSET(0),
    /** Full morph camera, neck limits, dig pull, gait sway, scent trails, etc. */
    REALISTIC(1),
    /** Vision + scent + cinematic dig blend; no nausea-prone head/gait/FP body effects. */
    SOFTENED(2);

    private final byte id;

    NaturalisExperienceMode(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return id;
    }

    public boolean isRealistic() {
        return this == REALISTIC;
    }

    public boolean isSoftened() {
        return this == SOFTENED;
    }

    public static NaturalisExperienceMode fromId(byte id) {
        return switch (id) {
            case 1 -> REALISTIC;
            case 2 -> SOFTENED;
            default -> UNSET;
        };
    }
}
