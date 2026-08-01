package dev.naturalis.client.perception;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class MorphMusicProfiles {

    /** Distant, same mood — wolf hears pressure in air, not melody. */
    private static final MorphMusicProfile CANINE = new MorphMusicProfile(0.10F, 0.08F, 0.0F);

    private static final MorphMusicProfile FELINE = new MorphMusicProfile(0.32F, 0.28F, 0.04F);

    private static final MorphMusicProfile GENERIC_FERAL = new MorphMusicProfile(0.28F, 0.24F, 0.02F);

    private static final MorphMusicProfile HERBIVORE = new MorphMusicProfile(0.42F, 0.36F, 0.02F);

    private static final MorphMusicProfile GENERIC_ANIMAL = new MorphMusicProfile(0.34F, 0.30F, 0.02F);

    private static final Map<String, MorphMusicProfile> EXPLICIT = Map.of(
        "wolf", CANINE,
        "fox", CANINE,
        "cat", FELINE,
        "ocelot", FELINE
    );

    private MorphMusicProfiles() {
    }

    public static MorphMusicProfile resolve(ResourceLocation morphId) {
        if (morphId == null) {
            return MorphMusicProfile.NONE;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        MorphMusicProfile explicit = EXPLICIT.get(path);
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(path, "wolf", "fox", "hound", "canid", "coyote")) {
            return CANINE;
        }
        if (containsAny(path, "cat", "ocelot", "lynx", "feline")) {
            return FELINE;
        }
        if (containsAny(path, "spider", "bat", "phantom", "warden", "bear", "boar")) {
            return GENERIC_FERAL;
        }
        if (containsAny(path, "cow", "sheep", "goat", "pig", "deer", "moose", "horse", "camel", "llama")) {
            return HERBIVORE;
        }
        if (containsAny(path, "villager", "player", "zombie", "skeleton")) {
            return MorphMusicProfile.NONE;
        }
        return GENERIC_ANIMAL;
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
