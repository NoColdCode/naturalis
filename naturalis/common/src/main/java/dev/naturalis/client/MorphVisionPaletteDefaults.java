package dev.naturalis.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Static opponent-process palette axes for each morph vision post effect id.
 * Dynamic perception uniforms (PhotoStress, MotionTrail, …) are layered on top each frame.
 */
public final class MorphVisionPaletteDefaults {

    public record Palette(
        float[] axisA,
        float[] axisB,
        float[] axisC,
        float[] colorA,
        float[] colorB,
        float[] colorC,
        float strength,
        float shadowLift,
        float lumaPreserve
    ) {
    }

    private static final Palette WOLF = new Palette(
        vec(0.46F, 0.48F, 0.06F), vec(0.08F, 0.22F, 0.92F), vec(0.28F, 0.74F, 0.14F),
        vec(0.92F, 0.84F, 0.18F), vec(0.14F, 0.38F, 1.00F), vec(0.48F, 0.82F, 0.26F),
        0.94F, 0.24F, 0.41F
    );
    private static final Palette MAMMAL = new Palette(
        vec(0.10F, 0.80F, 0.10F), vec(0.65F, 0.30F, 0.05F), vec(0.15F, 0.20F, 0.65F),
        vec(0.56F, 0.78F, 0.28F), vec(0.84F, 0.82F, 0.32F), vec(0.30F, 0.46F, 0.86F),
        0.92F, 0.22F, 0.45F
    );
    private static final Palette AVIAN = new Palette(
        vec(0.05F, 0.35F, 0.85F), vec(0.75F, 0.25F, 0.05F), vec(0.25F, 0.75F, 0.00F),
        vec(0.50F, 0.62F, 1.00F), vec(0.98F, 0.55F, 0.20F), vec(0.66F, 0.98F, 0.34F),
        0.92F, 0.22F, 0.45F
    );
    private static final Palette AQUATIC = new Palette(
        vec(0.08F, 0.42F, 0.78F), vec(0.72F, 0.28F, 0.12F), vec(0.18F, 0.62F, 0.55F),
        vec(0.42F, 0.72F, 0.96F), vec(0.88F, 0.68F, 0.22F), vec(0.36F, 0.88F, 0.72F),
        0.90F, 0.26F, 0.48F
    );
    private static final Palette REPTILE = new Palette(
        vec(0.22F, 0.62F, 0.18F), vec(0.58F, 0.35F, 0.08F), vec(0.12F, 0.45F, 0.38F),
        vec(0.62F, 0.82F, 0.32F), vec(0.92F, 0.72F, 0.18F), vec(0.38F, 0.68F, 0.42F),
        0.88F, 0.28F, 0.42F
    );
    private static final Palette UNDEAD = new Palette(
        vec(0.35F, 0.55F, 0.35F), vec(0.55F, 0.45F, 0.65F), vec(0.20F, 0.30F, 0.40F),
        vec(0.72F, 0.88F, 0.72F), vec(0.55F, 0.62F, 0.82F), vec(0.45F, 0.55F, 0.65F),
        0.78F, 0.38F, 0.55F
    );
    private static final Palette NETHER = new Palette(
        vec(0.75F, 0.22F, 0.08F), vec(0.15F, 0.08F, 0.72F), vec(0.55F, 0.35F, 0.12F),
        vec(1.00F, 0.45F, 0.12F), vec(0.22F, 0.12F, 0.95F), vec(0.95F, 0.55F, 0.18F),
        0.90F, 0.20F, 0.38F
    );
    private static final Palette ARCANE = new Palette(
        vec(0.42F, 0.18F, 0.82F), vec(0.18F, 0.72F, 0.55F), vec(0.82F, 0.35F, 0.92F),
        vec(0.75F, 0.42F, 1.00F), vec(0.35F, 0.95F, 0.72F), vec(1.00F, 0.55F, 0.95F),
        0.86F, 0.30F, 0.40F
    );

    private MorphVisionPaletteDefaults() {
    }

    private static final Palette INSECT = new Palette(
        vec(0.14F, 0.58F, 0.28F), vec(0.72F, 0.22F, 0.06F), vec(0.08F, 0.88F, 0.04F),
        vec(0.92F, 0.94F, 0.22F), vec(0.22F, 0.78F, 0.42F), vec(0.72F, 0.35F, 1.00F),
        0.94F, 0.26F, 0.42F
    );
    private static final Palette CEPHALOPOD = new Palette(
        vec(0.08F, 0.55F, 0.62F), vec(0.62F, 0.18F, 0.52F), vec(0.22F, 0.42F, 0.78F),
        vec(0.38F, 0.88F, 0.92F), vec(0.78F, 0.32F, 0.88F), vec(0.52F, 0.72F, 1.00F),
        0.90F, 0.28F, 0.46F
    );
    private static final Palette ABYSSAL = new Palette(
        vec(0.06F, 0.42F, 0.78F), vec(0.55F, 0.12F, 0.38F), vec(0.18F, 0.62F, 0.88F),
        vec(0.28F, 0.62F, 0.98F), vec(0.62F, 0.22F, 0.72F), vec(0.42F, 0.82F, 0.95F),
        0.88F, 0.32F, 0.50F
    );
    private static final Palette FUNGAL = new Palette(
        vec(0.42F, 0.22F, 0.55F), vec(0.72F, 0.55F, 0.12F), vec(0.18F, 0.62F, 0.28F),
        vec(0.82F, 0.38F, 0.92F), vec(0.92F, 0.78F, 0.22F), vec(0.48F, 0.92F, 0.42F),
        0.86F, 0.30F, 0.44F
    );
    private static final Palette CRYSTALLINE = new Palette(
        vec(0.72F, 0.82F, 0.95F), vec(0.35F, 0.55F, 0.88F), vec(0.88F, 0.92F, 1.00F),
        vec(0.95F, 0.98F, 1.00F), vec(0.55F, 0.72F, 0.98F), vec(0.88F, 0.95F, 1.00F),
        0.84F, 0.34F, 0.52F
    );
    private static final Palette FERROUS = new Palette(
        vec(0.55F, 0.42F, 0.35F), vec(0.35F, 0.38F, 0.48F), vec(0.62F, 0.48F, 0.32F),
        vec(0.88F, 0.62F, 0.42F), vec(0.52F, 0.58F, 0.72F), vec(0.72F, 0.55F, 0.38F),
        0.82F, 0.30F, 0.40F
    );
    private static final Palette FAE = new Palette(
        vec(0.88F, 0.55F, 0.92F), vec(0.42F, 0.78F, 0.95F), vec(0.95F, 0.72F, 0.55F),
        vec(1.00F, 0.72F, 0.98F), vec(0.55F, 0.92F, 1.00F), vec(1.00F, 0.85F, 0.72F),
        0.86F, 0.28F, 0.42F
    );
    private static final Palette TEMPEST = new Palette(
        vec(0.22F, 0.62F, 0.92F), vec(0.72F, 0.78F, 0.95F), vec(0.55F, 0.88F, 1.00F),
        vec(0.42F, 0.82F, 1.00F), vec(0.88F, 0.92F, 1.00F), vec(0.72F, 0.95F, 1.00F),
        0.88F, 0.26F, 0.46F
    );
    private static final Palette VISCOUS = new Palette(
        vec(0.35F, 0.82F, 0.22F), vec(0.78F, 0.22F, 0.55F), vec(0.22F, 0.65F, 0.18F),
        vec(0.62F, 0.95F, 0.28F), vec(0.92F, 0.35F, 0.72F), vec(0.48F, 0.88F, 0.35F),
        0.90F, 0.28F, 0.44F
    );
    private static final Palette VOID = new Palette(
        vec(0.42F, 0.18F, 0.72F), vec(0.12F, 0.55F, 0.62F), vec(0.72F, 0.22F, 0.88F),
        vec(0.68F, 0.38F, 0.98F), vec(0.28F, 0.72F, 0.88F), vec(0.82F, 0.42F, 0.95F),
        0.86F, 0.32F, 0.48F
    );

    public static Palette forShader(ResourceLocation logicalShaderId) {
        return forShader(logicalShaderId, logicalShaderId);
    }

    public static Palette forShader(ResourceLocation logicalShaderId, ResourceLocation postEffectId) {
        if (logicalShaderId != null) {
            Palette logical = paletteForPath(logicalShaderId.getPath());
            if (logical != null) {
                return logical;
            }
        }
        if (postEffectId != null) {
            Palette post = paletteForPath(postEffectId.getPath());
            if (post != null) {
                return post;
            }
        }
        return MAMMAL;
    }

    private static Palette paletteForPath(String path) {
        if (path == null) {
            return null;
        }
        return switch (path) {
            case "wolf_vision" -> WOLF;
            case "mammal_vision" -> MAMMAL;
            case "avian_vision" -> AVIAN;
            case "aquatic_vision" -> AQUATIC;
            case "reptile_vision" -> REPTILE;
            case "undead_vision" -> UNDEAD;
            case "nether_vision" -> NETHER;
            case "arcane_vision" -> ARCANE;
            case "insect_vision" -> INSECT;
            case "cephalopod_vision" -> CEPHALOPOD;
            case "abyssal_vision" -> ABYSSAL;
            case "fungal_vision" -> FUNGAL;
            case "crystalline_vision" -> CRYSTALLINE;
            case "ferrous_vision" -> FERROUS;
            case "fae_vision" -> FAE;
            case "tempest_vision" -> TEMPEST;
            case "viscous_vision" -> VISCOUS;
            case "void_vision" -> VOID;
            default -> null;
        };
    }

    private static float[] vec(float x, float y, float z) {
        return new float[] {x, y, z};
    }
}
