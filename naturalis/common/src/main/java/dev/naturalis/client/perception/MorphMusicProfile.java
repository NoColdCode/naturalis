package dev.naturalis.client.perception;

/**
 * How a morph perceives human music (muffled, emotionally distant, not "for" the animal).
 */
public record MorphMusicProfile(
    float musicVolumeMultiplier,
    float recordVolumeMultiplier,
    float distantHarmonicVolume
) {
    public static final MorphMusicProfile NONE = new MorphMusicProfile(1.0F, 1.0F, 0.0F);

    public boolean altersMusicPerception() {
        return musicVolumeMultiplier < 0.98F || recordVolumeMultiplier < 0.98F;
    }
}
