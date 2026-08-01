package dev.naturalis.client.perception;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MorphHearingProfiles {

    private static final Set<String> DEFAULT_PREY = Set.of(
        "sheep", "rabbit", "chicken", "fox", "pig", "cow", "goat", "villager"
    );

    private static final Set<String> DEFAULT_THREAT = Set.of(
        "zombie", "skeleton", "creeper", "spider", "cave_spider", "husk", "stray", "drowned", "phantom", "witch"
    );

    private static final MorphHearingProfile CANINE = new MorphHearingProfile(
        2.45F,
        28.0D,
        10,
        0.10F,
        Set.of("sheep", "rabbit", "chicken", "fox", "pig", "cow", "goat", "villager", "mooshroom"),
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile FELINE = new MorphHearingProfile(
        1.55F,
        24.0D,
        12,
        0.16F,
        Set.of("rabbit", "chicken", "cod", "salmon", "bat"),
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile BAT = new MorphHearingProfile(
        2.0F,
        28.0D,
        10,
        0.12F,
        DEFAULT_PREY,
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile SPIDER = new MorphHearingProfile(
        1.5F,
        22.0D,
        12,
        0.14F,
        Set.of("rabbit", "chicken", "silverfish", "endermite"),
        DEFAULT_THREAT,
        true,
        false
    );

    private static final MorphHearingProfile HERBIVORE = new MorphHearingProfile(
        1.42F,
        22.0D,
        12,
        0.18F,
        Set.of("grass", "wheat", "flower", "player", "wolf", "fox"),
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile PREDATOR = new MorphHearingProfile(
        1.65F,
        26.0D,
        10,
        0.14F,
        DEFAULT_PREY,
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile GENERIC_ANIMAL = new MorphHearingProfile(
        1.28F,
        26.0D,
        11,
        0.20F,
        DEFAULT_PREY,
        DEFAULT_THREAT,
        true,
        true
    );

    private static final MorphHearingProfile WARDEN = new MorphHearingProfile(
        2.2F, 56.0D, 4, 0.10F, Set.of(), DEFAULT_THREAT, true, false
    );
    private static final MorphHearingProfile HORSE = new MorphHearingProfile(
        1.38F, 34.0D, 8, 0.17F, DEFAULT_PREY, DEFAULT_THREAT, true, true
    );
    private static final MorphHearingProfile BEE = new MorphHearingProfile(
        1.72F, 30.0D, 8, 0.16F, Set.of("flower"), DEFAULT_THREAT, true, true
    );

    private static final Map<String, MorphHearingProfile> EXPLICIT = Map.ofEntries(
        Map.entry("wolf", CANINE),
        Map.entry("fox", CANINE),
        Map.entry("cat", FELINE),
        Map.entry("ocelot", FELINE),
        Map.entry("bat", BAT),
        Map.entry("spider", SPIDER),
        Map.entry("cave_spider", SPIDER),
        Map.entry("warden", WARDEN),
        Map.entry("cow", HERBIVORE),
        Map.entry("mooshroom", HERBIVORE),
        Map.entry("sheep", HERBIVORE),
        Map.entry("goat", HERBIVORE),
        Map.entry("pig", HERBIVORE),
        Map.entry("rabbit", HERBIVORE),
        Map.entry("horse", HORSE),
        Map.entry("polar_bear", PREDATOR),
        Map.entry("panda", PREDATOR),
        Map.entry("bee", BEE)
    );

    private MorphHearingProfiles() {
    }

    public static MorphHearingProfile resolve(ResourceLocation morphId) {
        if (morphId == null) {
            return MorphHearingProfile.NONE;
        }

        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        MorphHearingProfile explicit = EXPLICIT.get(path);
        if (explicit != null) {
            return explicit;
        }

        if (containsAny(path, "wolf", "fox", "hound", "coyote", "jackal", "canid")) {
            return CANINE;
        }
        if (containsAny(path, "cat", "ocelot", "lynx", "feline")) {
            return FELINE;
        }
        if (containsAny(path, "bat", "owl")) {
            return BAT;
        }
        if (containsAny(path, "spider", "mite")) {
            return SPIDER;
        }
        if (containsAny(path, "warden")) {
            return EXPLICIT.get("warden");
        }
        if (containsAny(path, "bear", "lion", "tiger", "leopard", "shark", "orca", "croc", "alligator")) {
            return PREDATOR;
        }
        if (containsAny(path, "cow", "sheep", "goat", "pig", "deer", "moose", "elk", "bison", "buffalo")) {
            return HERBIVORE;
        }
        if (isHumanoidMorph(path)) {
            return MorphHearingProfile.NONE;
        }

        return GENERIC_ANIMAL;
    }

    private static boolean isHumanoidMorph(String path) {
        return containsAny(path, "villager", "player", "zombie", "skeleton", "drowned", "husk", "stray",
            "pillager", "vindicator", "evoker", "witch", "illusioner", "piglin", "hoglin");
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
