package dev.naturalis.client.perception;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves {@link MorphEmbodimentProfile} for any morph id. Explicit vanilla mobs plus archetype fallbacks.
 */
public final class MorphEmbodimentProfiles {

    private static final double MAX_FOV_MULTIPLIER = 1.40D;

    private static final MorphEmbodimentProfile CANINE = new MorphEmbodimentProfile(
        1.40D, -0.62D, 0.30D, 1.0D, MorphArmInteractionStyle.CANINE, true
    );

    private static final MorphEmbodimentProfile FELINE = new MorphEmbodimentProfile(
        1.24D, -0.44D, 0.18D, 0.92D, MorphArmInteractionStyle.FELINE, true
    );

    private static final MorphEmbodimentProfile EQUINE = new MorphEmbodimentProfile(
        1.40D, -0.28D, 0.35D, 0.75D, MorphArmInteractionStyle.EQUINE, true
    );

    private static final MorphEmbodimentProfile SPIDER = new MorphEmbodimentProfile(
        1.14D, -0.55D, 0.18D, 0.88D, MorphArmInteractionStyle.SPIDER, true
    );

    private static final MorphEmbodimentProfile AVIAN = new MorphEmbodimentProfile(
        1.18D, -0.20D, 0.10D, 0.88D, MorphArmInteractionStyle.AVIAN, false
    );

    private static final MorphEmbodimentProfile AQUATIC = new MorphEmbodimentProfile(
        1.10D, -0.15D, 0.08D, 0.88D, MorphArmInteractionStyle.AQUATIC, false
    );

    private static final MorphEmbodimentProfile PREY = new MorphEmbodimentProfile(
        1.12D, -0.30D, 0.06D, 0.88D, MorphArmInteractionStyle.GENERIC, true
    );

    private static final MorphEmbodimentProfile GENERIC_QUADRUPED = new MorphEmbodimentProfile(
        1.14D, -0.32D, 0.14D, 0.88D, MorphArmInteractionStyle.GENERIC, true
    );

    /** Strong arm-hide so integration morphs (snail, insects, …) replace the vanilla FP hand. */
    private static final MorphEmbodimentProfile GENERIC_ANIMAL = new MorphEmbodimentProfile(
        1.10D, -0.22D, 0.08D, 0.88D, MorphArmInteractionStyle.GENERIC, true
    );

    private static final Map<String, MorphEmbodimentProfile> EXPLICIT = Map.ofEntries(
        // Canids
        Map.entry("wolf", CANINE),
        Map.entry("fox", CANINE),
        // Felines
        Map.entry("cat", FELINE),
        Map.entry("ocelot", FELINE),
        // Equines & camelids
        Map.entry("horse", EQUINE),
        Map.entry("skeleton_horse", EQUINE),
        Map.entry("zombie_horse", EQUINE),
        Map.entry("donkey", EQUINE),
        Map.entry("mule", EQUINE),
        Map.entry("camel", EQUINE),
        Map.entry("llama", EQUINE),
        Map.entry("trader_llama", EQUINE),
        // Arachnids
        Map.entry("spider", SPIDER),
        Map.entry("cave_spider", SPIDER),
        // Avians & flying insects
        Map.entry("chicken", AVIAN),
        Map.entry("parrot", AVIAN),
        Map.entry("bat", AVIAN),
        Map.entry("phantom", AVIAN),
        Map.entry("bee", AVIAN),
        Map.entry("allay", AVIAN),
        // Aquatic
        Map.entry("dolphin", AQUATIC),
        Map.entry("cod", AQUATIC),
        Map.entry("salmon", AQUATIC),
        Map.entry("tropical_fish", AQUATIC),
        Map.entry("pufferfish", AQUATIC),
        Map.entry("axolotl", AQUATIC),
        Map.entry("squid", AQUATIC),
        Map.entry("glow_squid", AQUATIC),
        Map.entry("tadpole", AQUATIC),
        Map.entry("guardian", AQUATIC),
        Map.entry("elder_guardian", AQUATIC),
        Map.entry("turtle", AQUATIC),
        Map.entry("frog", AQUATIC),
        // Prey / herd
        Map.entry("rabbit", PREY),
        Map.entry("sheep", PREY),
        Map.entry("goat", PREY),
        Map.entry("cow", PREY),
        Map.entry("mooshroom", PREY),
        Map.entry("pig", PREY),
        // Heavy quadrupeds
        Map.entry("polar_bear", GENERIC_QUADRUPED),
        Map.entry("panda", GENERIC_QUADRUPED),
        Map.entry("hoglin", GENERIC_QUADRUPED),
        Map.entry("zoglin", GENERIC_QUADRUPED),
        Map.entry("ravager", GENERIC_QUADRUPED),
        Map.entry("iron_golem", GENERIC_QUADRUPED),
        Map.entry("snow_golem", GENERIC_ANIMAL),
        Map.entry("strider", GENERIC_QUADRUPED),
        Map.entry("armadillo", PREY),
        Map.entry("sniffer", GENERIC_QUADRUPED),
        Map.entry("warden", GENERIC_QUADRUPED),
        // Small ground
        Map.entry("silverfish", SPIDER),
        Map.entry("endermite", SPIDER),
        Map.entry("slime", GENERIC_ANIMAL),
        Map.entry("magma_cube", GENERIC_ANIMAL)
    );

    private static final Set<String> HUMANOID_PATHS = Set.of(
        "villager", "wandering_trader", "player", "zombie", "husk", "drowned", "skeleton",
        "stray", "wither_skeleton", "bogged", "creeper", "pillager", "vindicator", "evoker",
        "illusioner", "witch", "piglin", "piglin_brute", "zombified_piglin", "enderman",
        "blaze", "ghast", "shulker", "vex", "wither", "ender_dragon", "giant", "breeze"
    );

    private MorphEmbodimentProfiles() {
    }

    public static MorphEmbodimentProfile resolve(ResourceLocation morphId) {
        if (morphId == null) {
            return MorphEmbodimentProfile.NONE;
        }

        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        MorphEmbodimentProfile explicit = EXPLICIT.get(path);
        if (explicit != null) {
            return explicit;
        }

        if (HUMANOID_PATHS.contains(path) || containsAny(path, "villager", "zombie", "skeleton", "illager", "piglin")) {
            return MorphEmbodimentProfile.NONE;
        }

        if (containsAny(path, "wolf", "fox", "hound", "canid", "coyote", "jackal", "dog")) {
            return CANINE;
        }
        if (containsAny(path, "cat", "ocelot", "lynx", "tiger", "leopard", "feline", "panther", "cheetah")) {
            return FELINE;
        }
        if (containsAny(path, "horse", "donkey", "mule", "camel", "llama", "zebra", "unicorn")) {
            return EQUINE;
        }
        if (containsAny(path, "spider", "mite", "scorpion", "arachnid")) {
            return SPIDER;
        }
        if (containsAny(path, "bird", "crow", "eagle", "hawk", "owl", "vulture", "duck", "chicken", "parrot", "bat", "bee")) {
            return AVIAN;
        }
        if (containsAny(path, "fish", "shark", "whale", "dolphin", "squid", "axolotl", "frog", "turtle", "aquatic", "salmon", "cod")) {
            return AQUATIC;
        }
        if (containsAny(path, "rabbit", "deer", "sheep", "goat", "cow", "pig", "rodent", "moose", "bison", "calf")) {
            return PREY;
        }
        if (containsAny(path, "bear", "boar", "hyena", "puma", "wolfman", "hound", "ravager", "warden", "golem", "hog")) {
            return GENERIC_QUADRUPED;
        }
        if (containsAny(path, "snail", "slug", "insect", "bug", "beetle", "butterfly", "moth", "worm")) {
            return GENERIC_ANIMAL;
        }

        return GENERIC_ANIMAL;
    }

    public static double clampFov(double baseFov, MorphEmbodimentProfile profile) {
        double multiplier = profile.fovMultiplier();
        if (Math.abs(multiplier - 1.0D) < 1.0E-6D) {
            return baseFov;
        }
        double adjusted = baseFov * multiplier;
        return Math.max(30.0D, Math.min(150.0D, adjusted));
    }

    public static double armOffsetNormalization(MorphEmbodimentProfile profile) {
        double multiplier = profile.fovMultiplier();
        if (multiplier <= 1.0D + 1.0E-6D) {
            return 0.0D;
        }
        double t = (multiplier - 1.0D) / (MAX_FOV_MULTIPLIER - 1.0D);
        return Math.max(0.0D, Math.min(1.0D, t));
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
