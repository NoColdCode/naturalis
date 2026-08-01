package dev.naturalis.metabolism;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class MetabolismManager {

    private static final double MIN_MULTIPLIER = 0.5D;
    private static final double MAX_MULTIPLIER = 2.0D;

    /** Relative mass units (human-ish morph ≈ 2.4–2.6). Used only for inertia / metabolism flavour. */
    private static final Map<String, Double> MASS_BY_ENTITY = new HashMap<>();

    static {
        seedVanillaMasses();
        seedAlexsMobsMasses();
    }

    private static void seedVanillaMasses() {
        putMass(
            "allay", 0.35D,
            "armadillo", 0.65D,
            "axolotl", 0.75D,
            "bat", 0.12D,
            "bee", 0.28D,
            "blaze", 2.2D,
            "bogged", 2.0D,
            "breeze", 1.9D,
            "camel", 6.2D,
            "cat", 1.15D,
            "cave_spider", 0.78D,
            "chicken", 0.95D,
            "cod", 0.55D,
            "cow", 4.2D,
            "creeper", 1.95D,
            "dolphin", 1.85D,
            "donkey", 4.6D,
            "drowned", 2.15D,
            "elder_guardian", 7.8D,
            "enderman", 4.1D,
            "endermite", 0.22D,
            "ender_dragon", 8.5D,
            "evoker", 2.05D,
            "fox", 1.55D,
            "frog", 0.55D,
            "ghast", 4.8D,
            "happy_ghast", 4.6D,
            "glow_squid", 1.05D,
            "goat", 3.6D,
            "guardian", 5.8D,
            "hoglin", 6.8D,
            "horse", 5.2D,
            "husk", 2.05D,
            "illusioner", 2.0D,
            "iron_golem", 11.5D,
            "llama", 4.7D,
            "magma_cube", 1.45D,
            "mooshroom", 4.35D,
            "mule", 4.85D,
            "ocelot", 1.05D,
            "panda", 4.15D,
            "parrot", 0.45D,
            "phantom", 2.65D,
            "pig", 2.55D,
            "piglin", 2.85D,
            "piglin_brute", 3.55D,
            "pillager", 2.25D,
            "polar_bear", 6.2D,
            "pufferfish", 0.5D,
            "rabbit", 0.48D,
            "ravager", 8.4D,
            "salmon", 0.65D,
            "sheep", 2.05D,
            "shulker", 5.2D,
            "silverfish", 0.38D,
            "skeleton", 1.95D,
            "skeleton_horse", 5.0D,
            "slime", 0.85D,
            "snow_golem", 1.65D,
            "spider", 1.05D,
            "squid", 1.15D,
            "stray", 1.95D,
            "strider", 3.1D,
            "tadpole", 0.12D,
            "trader_llama", 4.7D,
            "tropical_fish", 0.35D,
            "turtle", 2.45D,
            "vex", 0.55D,
            "villager", 2.3D,
            "vindicator", 2.55D,
            "wandering_trader", 2.25D,
            "witch", 2.1D,
            "wither", 7.3D,
            "wither_skeleton", 2.15D,
            "wolf", 2.55D,
            "zoglin", 6.2D,
            "zombie", 2.05D,
            "zombie_horse", 5.1D,
            "zombie_villager", 2.2D,
            "zombified_piglin", 3.05D,
            "creaking", 3.4D
        );
    }

    private static void putMass(Object... keysAndValues) {
        for (int i = 0; i < keysAndValues.length; i += 2) {
            MASS_BY_ENTITY.put((String) keysAndValues[i], (Double) keysAndValues[i + 1]);
        }
    }

    private static void seedAlexsMobsMasses() {
        putMass(
            "anaconda", 4.6D,
            "anteater", 2.8D,
            "bald_eagle", 1.55D,
            "banana_slug", 0.35D,
            "bison", 7.5D,
            "blobfish", 0.85D,
            "blue_jay", 0.75D,
            "bone_serpent", 6.2D,
            "bunfungus", 1.2D,
            "cachalot_whale", 10.8D,
            "capuchin_monkey", 1.05D,
            "catfish", 1.55D,
            "centipede", 0.95D,
            "cockroach", 0.15D,
            "comb_jelly", 0.25D,
            "cosmic_cod", 0.8D,
            "crimson_mosquito", 0.35D,
            "crow", 0.82D,
            "devils_hole_pupfish", 0.25D,
            "dropbear", 2.8D,
            "elephant", 11.2D,
            "emu", 2.05D,
            "flutter_manakin", 0.18D,
            "flutter", 0.18D,
            "fly", 0.08D,
            "flying_fish", 0.52D,
            "frilled_shark", 4.5D,
            "gazelle", 2.4D,
            "gelada_monkey", 3.2D,
            "giant_squid", 5.2D,
            "gorilla", 5.8D,
            "grizzly_bear", 6.6D,
            "guster", 2.2D,
            "hammerhead", 5.1D,
            "hummingbird", 0.2D,
            "jerboa", 0.35D,
            "kangaroo", 3.5D,
            "komodo_dragon", 4.15D,
            "leafcutter_ant", 0.06D,
            "lobster", 1.25D,
            "maned_wolf", 2.55D,
            "mimic_octopus", 1.25D,
            "mimicube", 0.45D,
            "moose", 7.8D,
            "mudskipper", 0.55D,
            "mungus", 7.2D,
            "orca", 9.2D,
            "platypus", 0.85D,
            "raccoon", 2.2D,
            "rattlesnake", 0.82D,
            "rhinoceros", 9.2D,
            "roadrunner", 0.95D,
            "rocky_roller", 1.55D,
            "sea_bear", 5.5D,
            "seagull", 0.85D,
            "seal", 3.6D,
            "shoebill", 1.65D,
            "skelewag", 3.2D,
            "skreecher", 0.85D,
            "skunk", 1.35D,
            "snow_leopard", 3.8D,
            "spectre", 1.05D,
            "straddler", 3.05D,
            "stradpole", 0.48D,
            "sunbird", 2.45D,
            "sunscorcher", 2.8D,
            "tarantula_hawk", 0.52D,
            "tasmanian_devil", 1.65D,
            "terrapin", 1.45D,
            "tiger", 5.6D,
            "toucan", 0.82D,
            "triops", 0.22D,
            "tusklin", 5.4D,
            "void_worm", 8.2D,
            "warped_mosco", 0.62D,
            "warped_toad", 1.55D,
            "crocodile", 5.2D,
            "alligator_snapping_turtle", 4.05D,
            "crocodilian", 5.05D,
            "capybara", 3.05D,
            "dromedary", 4.65D,
            "sugar_glider", 0.42D,
            "underminer", 2.5D,
            "potoo", 0.55D,
            "rocky_shellplate", 2.5D,
            "enderiophage", 1.8D,
            "straddleboard", 1.2D
        );
    }

    private MetabolismManager() {
    }

    public static double getMetabolismMultiplier(ResourceLocation morphId) {
        double mass = getMass(morphId);
        double multiplier = MIN_MULTIPLIER + (Math.log(mass + 1.0D) / Math.log(8.0D)) * 1.5D;
        return Mth.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    public static double getMetabolismMultiplierForPlayer(Player player, ResourceLocation morphId) {
        return getMetabolismMultiplier(morphId);
    }

    public static double getMass(ResourceLocation morphId) {
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        return resolveMass(morphId, type);
    }

    private static double resolveMass(ResourceLocation morphId, EntityType<?> entityType) {
        var profileMass = dev.naturalis.profile.MobProfileRegistry.getMass(morphId);
        if (profileMass.isPresent()) {
            return profileMass.get();
        }

        String path = morphId.getPath();
        Double mapped = MASS_BY_ENTITY.get(path);
        if (mapped != null) {
            return mapped;
        }

        Double heuristic = integrationHeuristicMass(morphId.getNamespace(), path);
        if (heuristic != null) {
            return heuristic;
        }

        if (entityType == null) {
            return 2.2D;
        }

        double width = entityType.getDimensions().width();
        double height = entityType.getDimensions().height();
        double volumeLike = Math.max(0.05D, width * width * height);

        double categoryFactor = switch (entityType.getCategory()) {
            case MONSTER -> 2.55D;
            case CREATURE -> 2.25D;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE -> 2.05D;
            case AMBIENT -> 1.15D;
            default -> 1.85D;
        };

        double baseMass = volumeLike * categoryFactor;
        return Mth.clamp(baseMass, 0.28D, 12.5D);
    }

    /** Path/keyword mass for integrated mods when a profile mass is missing. */
    private static Double integrationHeuristicMass(String namespace, String path) {
        if ("alexsmobs".equals(namespace)) {
            Double alex = alexsHeuristicMass(path);
            if (alex != null) {
                return alex;
            }
        }
        if (containsAny(path, "leviathan", "whale", "elder", "ur_ghast", "wither", "dragon", "hydra")) {
            return 11.0D;
        }
        if (containsAny(path, "elephant", "mammoth", "rhino", "hippo", "ravager", "golem", "slider", "iron")) {
            return 10.0D;
        }
        if (containsAny(path, "bear", "gorilla", "buffalo", "moose", "boss", "queen", "king", "ancient")) {
            return 7.0D;
        }
        if (containsAny(path, "snail", "slug", "caterpillar", "firefly", "butterfly", "mosquito", "fly", "mite")) {
            return 0.25D;
        }
        if (containsAny(path, "bird", "finch", "sparrow", "robin", "jay", "canary", "cardinal", "parrot", "crow")) {
            return 0.45D;
        }
        if (containsAny(path, "spirit", "wraith", "ghost", "shade", "phantom", "whirlwind")) {
            return 1.4D;
        }
        if (containsAny(path, "skeleton", "zombie", "husk", "drowned", "illager", "villager")) {
            return 2.2D;
        }
        if (containsAny(path, "fish", "salmon", "cod", "bass", "koi", "squid", "octopus")) {
            return 0.9D;
        }
        if (containsAny(path, "wolf", "fox", "cat", "dog", "hound")) {
            return 2.0D;
        }
        if (containsAny(path, "deer", "sheep", "pig", "cow", "horse", "camel")) {
            return 3.2D;
        }
        // Namespace flavour when path gives no hint
        return switch (namespace) {
            case "cataclysm", "iceandfire", "twilightforest" -> 4.5D;
            case "goety", "born_in_chaos", "born_in_chaos_v1" -> 2.6D;
            case "aether" -> 3.2D;
            case "naturalist", "crittersandcompanions", "friendsandfoes" -> 2.0D;
            case "aquamirae" -> 2.8D;
            case "mowziesmobs" -> 3.5D;
            default -> null;
        };
    }

    private static Double alexsHeuristicMass(String path) {
        if (containsAny(path, "whale", "leviathan")) {
            return 11.0D;
        }
        if (containsAny(path, "elephant", "mammoth", "tusk")) {
            return 11.0D;
        }
        if (containsAny(path, "ant", "mite", "flea", "mosquito", "fly", "bee", "wasp", "butterfly")) {
            return Mth.clamp(0.05D + path.length() * 0.01D, 0.06D, 0.35D);
        }
        if (containsAny(path, "bird", "hawk", "owl", "parrot", "gull", "sparrow", "finch", "crane", "stork")) {
            return 0.85D;
        }
        if (containsAny(path, "snake", "serpent", "eel")) {
            return 1.35D;
        }
        if (containsAny(path, "shark", "fish", "squid", "lobster", "crab", "shrimp")) {
            return 1.55D;
        }
        if (containsAny(path, "bear", "gorilla", "rhino", "bison", "buffalo")) {
            return 6.5D;
        }
        if (containsAny(path, "worm", "hydra", "dragon")) {
            return 7.5D;
        }
        return null;
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String t : tokens) {
            if (path.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
