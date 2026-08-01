package dev.naturalis.client.perception;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class MorphVibrationProfiles {

    private static final MorphVibrationProfile CANINE = new MorphVibrationProfile(
        40.0D, 3, 0.10F, 0.28F, 1.45F
    );

    private static final MorphVibrationProfile FELINE = new MorphVibrationProfile(
        22.0D, 6, 0.16F, 0.42F, 0.7F
    );

    private static final MorphVibrationProfile HEAVY = new MorphVibrationProfile(
        24.0D, 7, 0.18F, 0.45F, 0.72F
    );

    private static final MorphVibrationProfile GENERIC = new MorphVibrationProfile(
        20.0D, 9, 0.20F, 0.48F, 0.62F
    );

    private static final MorphVibrationProfile WARDEN = new MorphVibrationProfile(40.0D, 4, 0.08F, 0.3F, 1.2F);
    private static final MorphVibrationProfile RABBIT = new MorphVibrationProfile(16.0D, 10, 0.22F, 0.52F, 0.58F);
    private static final MorphVibrationProfile BEE = new MorphVibrationProfile(14.0D, 11, 0.24F, 0.55F, 0.45F);

    private static final Map<String, MorphVibrationProfile> EXPLICIT = Map.ofEntries(
        Map.entry("wolf", CANINE),
        Map.entry("fox", CANINE),
        Map.entry("cat", FELINE),
        Map.entry("ocelot", FELINE),
        Map.entry("warden", WARDEN),
        Map.entry("cow", HEAVY),
        Map.entry("horse", HEAVY),
        Map.entry("pig", GENERIC),
        Map.entry("sheep", GENERIC),
        Map.entry("rabbit", RABBIT),
        Map.entry("bee", BEE)
    );

    private MorphVibrationProfiles() {
    }

    public static MorphVibrationProfile resolve(ResourceLocation morphId) {
        if (morphId == null) {
            return MorphVibrationProfile.NONE;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        MorphVibrationProfile explicit = EXPLICIT.get(path);
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(path, "wolf", "fox", "hound", "canid", "coyote")) {
            return CANINE;
        }
        if (containsAny(path, "cat", "ocelot", "lynx", "feline")) {
            return FELINE;
        }
        if (containsAny(path, "horse", "cow", "ravager", "iron_golem", "elephant", "rhino", "hippo")) {
            return HEAVY;
        }
        if (containsAny(path, "pig", "sheep", "goat", "deer", "moose", "camel", "llama")) {
            return GENERIC;
        }
        if (containsAny(path, "villager", "player", "zombie", "skeleton")) {
            return MorphVibrationProfile.NONE;
        }
        return GENERIC;
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
