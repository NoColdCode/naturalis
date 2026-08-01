package dev.naturalis.diet;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Set;

public final class DietManager {

    public enum DietType {
        CARNIVORE,
        HERBIVORE,
        OMNIVORE,
        /** Fish and aquatic prey. */
        PISCIVORE,
        /** Insects, arthropods, larvae. */
        INSECTIVORE,
        /** Carrion, rotten flesh, decayed organic matter. */
        NECROVORE,
        /** Fruit, berries, sweet plant matter. */
        FRUGIVORE,
        /** Mushrooms and fungal matter. */
        FUNGIVORE,
        /** Blood and raw meat only. */
        HEMATOPHAGE,
        /** Opportunistic carrion + meat; dislikes fresh plants. */
        SCAVENGER,
        /** Minerals and earth (golems, elementals). */
        LITHOVORE,
        /** Flowers and soft plant matter. */
        FLORIVORE,
        /** Nectar, honey, pollen. */
        NECTARIVORE
    }

    public enum FoodType {
        MEAT,
        VEGGIE,
        NEUTRAL,
        OTHER
    }

    private static final Set<String> CARNIVORE_MORPHS = Set.of(
        "wolf", "fox", "ocelot", "cat", "axolotl", "dolphin", "guardian", "elder_guardian",
        "cod", "salmon", "pufferfish", "tropical_fish", "spider", "cave_spider", "ravager",
        "hoglin", "strider", "wither_skeleton", "creeper", "enderman", "endermite", "blaze",
        "ghast", "magma_cube", "slime", "warden", "zombie"
    );

    private static final Set<String> HERBIVORE_MORPHS = Set.of(
        "cow", "sheep", "goat", "horse", "donkey", "mule", "llama", "camel", "rabbit",
        "sniffer", "mooshroom", "turtle", "frog", "panda", "bee", "allay"
    );

    private static final Set<String> MEAT_FOODS = Set.of(
        "beef", "cooked_beef", "porkchop", "cooked_porkchop", "mutton", "cooked_mutton",
        "chicken", "cooked_chicken", "rabbit", "cooked_rabbit", "cod", "cooked_cod",
        "salmon", "cooked_salmon", "tropical_fish", "rotten_flesh", "spider_eye"
    );

    private static final Set<String> VEGGIE_FOODS = Set.of(
        "carrot", "golden_carrot", "potato", "baked_potato", "poisonous_potato", "beetroot",
        "beetroot_soup", "dried_kelp", "kelp", "sweet_berries", "glow_berries", "melon_slice",
        "apple", "golden_apple", "enchanted_golden_apple", "chorus_fruit"
    );

    private static final Set<String> NEUTRAL_FOODS = Set.of(
        "bread", "mushroom_stew", "rabbit_stew", "suspicious_stew", "pumpkin_pie", "cookie",
        "honey_bottle", "dried_kelp", "baked_potato"
    );

    private DietManager() {
    }

    public static DietType getDietType(ResourceLocation morphId) {
        var profileDiet = dev.naturalis.profile.MobProfileRegistry.getDiet(morphId);
        if (profileDiet.isPresent()) {
            return profileDiet.get();
        }

        if ("minecraft".equals(morphId.getNamespace())) {
            DietType vanilla = getVanillaDietType(morphId.getPath());
            if (vanilla != null) {
                return vanilla;
            }
        }

        if ("alexsmobs".equals(morphId.getNamespace())) {
            DietType modded = getAlexsMobsDietType(morphId.getPath());
            if (modded != null) {
                return modded;
            }
        }

        String path = morphId.getPath();

        // Explicit overrides requested.
        if ("zombie".equals(path)) {
            return DietType.CARNIVORE;
        }
        if ("skeleton".equals(path)) {
            return DietType.OMNIVORE;
        }

        if (CARNIVORE_MORPHS.contains(path)) {
            return DietType.CARNIVORE;
        }
        if (HERBIVORE_MORPHS.contains(path)) {
            return DietType.HERBIVORE;
        }

        // Heuristic coverage for all vanilla + modded mobs by realistic family keywords.
        if (matchesAny(path, "cow", "sheep", "goat", "horse", "donkey", "mule", "llama", "camel", "deer", "moose",
            "elk", "bison", "buffalo", "gazelle", "antelope", "yak", "giraffe", "elephant", "rhino", "hippo",
            "sniffer", "turtle", "tortoise", "rabbit", "hare", "capybara", "manatee")) {
            return DietType.HERBIVORE;
        }

        if (matchesAny(path, "wolf", "fox", "cat", "ocelot", "lion", "tiger", "leopard", "jaguar", "bear", "shark",
            "croc", "alligator", "gator", "serpent", "snake", "anaconda", "boa", "spider", "scorpion",
            "piranha", "angler", "raptor", "eagle", "hawk", "falcon", "owl", "vulture", "wyvern", "drake",
            "orca", "killer", "hammerhead")) {
            return DietType.CARNIVORE;
        }

        if (matchesAny(path, "pig", "boar", "raccoon", "crow", "raven", "parrot", "chicken", "duck", "goose",
            "turkey", "monkey", "ape", "villager")) {
            return DietType.OMNIVORE;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MONSTER
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AXOLOTLS) {
                return DietType.CARNIVORE;
            }

            if (category == MobCategory.CREATURE) {
                return DietType.OMNIVORE;
            }
        }

        return DietType.OMNIVORE;
    }

    private static DietType getAlexsMobsDietType(String path) {
        return switch (path) {
            case "grizzly_bear", "tiger", "anaconda", "hammerhead", "orca", "seal", "komodo_dragon",
                 "spectre", "void_worm", "warped_mosco", "tarantula_hawk", "bald_eagle", "sunbird",
                 "bone_serpent", "crocodilian", "lobster", "giant_squid", "blobfish", "cachalot_whale",
                 "alligator_snapping_turtle", "maned_wolf", "skreecher", "platypus", "straddler", "stradpole"
                    -> DietType.CARNIVORE;
            case "rhinoceros", "elephant", "capybara", "hummingbird", "flutter", "dromedary"
                    -> DietType.HERBIVORE;
            case "gorilla", "catfish", "warped_toad", "crow", "emu", "rocky_roller", "mungus", "mimic_octopus"
                    -> DietType.OMNIVORE;
            default -> null;
        };
    }

    private static DietType getVanillaDietType(String path) {
        return switch (path) {
            case "wolf", "cat", "ocelot", "polar_bear", "axolotl", "dolphin", "guardian", "elder_guardian",
                "cod", "salmon", "pufferfish", "tropical_fish", "spider", "cave_spider", "ravager", "hoglin",
                "strider", "wither_skeleton", "creeper", "enderman", "endermite", "blaze", "ghast", "magma_cube",
                "slime", "warden", "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged",
                "zombified_piglin", "piglin", "piglin_brute", "vex", "phantom", "wither", "ender_dragon",
                "silverfish", "zoglin" -> DietType.CARNIVORE;

            case "cow", "sheep", "goat", "horse", "donkey", "mule", "llama", "trader_llama", "camel", "rabbit",
                "sniffer", "mooshroom", "turtle", "bee", "armadillo" -> DietType.HERBIVORE;

            case "fox", "pig", "chicken", "parrot", "bat", "allay", "frog", "villager", "wandering_trader",
                "pillager", "vindicator", "evoker", "illusioner", "witch", "iron_golem", "snow_golem", "breeze",
                "shulker", "panda" -> DietType.OMNIVORE;

            default -> null;
        };
    }

    private static boolean matchesAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static FoodType getFoodType(ResourceLocation itemId) {
        String path = itemId.getPath();
        if (MEAT_FOODS.contains(path)) {
            return FoodType.MEAT;
        }
        if (VEGGIE_FOODS.contains(path)) {
            return FoodType.VEGGIE;
        }
        if (NEUTRAL_FOODS.contains(path)) {
            return FoodType.NEUTRAL;
        }
        return FoodType.OTHER;
    }
}
