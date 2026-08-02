package dev.naturalis.client;

/**
 * 1.21.1 stub: UBO palette upload uses 1.21.2+ render APIs; legacy uniform path is used instead.
 */
public final class MorphPostEffectUniformWriter {

    private MorphPostEffectUniformWriter() {
    }

    public static boolean upload(
        Object postChain,
        MorphVisionPaletteDefaults.Palette palette,
        float chromaticMode,
        float photoStress,
        float kaleidoStrength,
        float kaleidoFolds,
        float spectralProfile,
        float motionTrailStrength,
        float motionX,
        float motionY,
        float strengthBoost
    ) {
        return false;
    }
}
