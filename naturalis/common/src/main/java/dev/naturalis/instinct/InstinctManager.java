package dev.naturalis.instinct;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;
import java.util.Set;

public final class InstinctManager {

    private static final Set<String> WANDER_MORPHS = Set.of(
        "wolf", "fox", "cat", "ocelot", "rabbit", "pig", "cow", "sheep", "goat", "horse", "llama",
        "chicken", "villager", "wandering_trader", "mooshroom", "sniffer",
        "zombie", "skeleton", "spider", "cave_spider", "enderman", "creeper", "slime", "magma_cube",
        // Flying mobs that idle-wander in vanilla (checked before flight-only exclusion)
        "bat", "bee", "phantom", "parrot", "allay",
        // Alex's Mobs
        "crow", "bald_eagle", "toucan", "maned_wolf", "tiger", "grizzly_bear", "gorilla", "emu",
        "hummingbird", "flutter"
    );

    private static final Set<String> FEAR_WATER = Set.of(
        "blaze", "enderman", "endermite", "strider", "magma_cube", "ghast"
    );

    private static final Set<String> FEAR_CATS = Set.of(
        "creeper"
    );

    private static final Set<String> FEAR_WOLVES = Set.of(
        "skeleton", "stray", "bogged"
    );

    private static final Set<String> FEAR_IRON_GOLEM = Set.of(
        "zombie", "husk", "drowned", "zombie_villager", "zoglin"
    );

    private static final Set<String> FEAR_ZOGLIN = Set.of(
        "piglin", "piglin_brute"
    );

    private static final Set<String> FEAR_BEES = Set.of(
        "bear", "polar_bear"
    );

    private static final Set<String> FLIGHT_ONLY = Set.of(
        "ghast", "phantom", "vex", "allay", "blaze", "bat", "bee", "ender_dragon",
        // Alex's Mobs
        "spectre", "void_worm", "warped_mosco", "hummingbird", "flutter"
    );

    // Morphs with strong olfactory tracking. Higher value = longer smell range.
    private static final Map<String, Integer> SMELL_STRENGTH = Map.ofEntries(
        Map.entry("wolf", 3),
        Map.entry("fox", 3),
        Map.entry("cat", 2),
        Map.entry("ocelot", 2),
        Map.entry("spider", 2),
        Map.entry("cave_spider", 2),
        Map.entry("warden", 3),
        Map.entry("pig", 1),
        // Alex's Mobs
        Map.entry("maned_wolf", 3),
        Map.entry("tiger", 3),
        Map.entry("grizzly_bear", 2),
        Map.entry("anaconda", 1),
        Map.entry("komodo_dragon", 2),
        Map.entry("crocodilian", 1)
    );

    private static final Set<String> NYCTALOP_HOSTILE = Set.of(
        "fox",
        "cat",
        "ocelot",
        "bat",
        "wolf",
        "spider",
        "cave_spider",
        "enderman",
        "endermite",
        "silverfish",
        "skeleton",
        "stray",
        "zombie",
        "husk",
        "drowned",
        "creeper",
        "phantom",
        "witch",
        // Alex's Mobs
        "spectre",
        "void_worm",
        "crow",
        "maned_wolf",
        "tiger"
    );

    private static final Map<String, Set<String>> HUNT_PREY_BY_MORPH = Map.ofEntries(
        Map.entry("wolf", Set.of("sheep", "rabbit", "chicken", "fox")),
        Map.entry("fox", Set.of("rabbit", "chicken", "cod", "salmon")),
        Map.entry("cat", Set.of("rabbit", "chicken", "cod", "salmon")),
        Map.entry("ocelot", Set.of("rabbit", "chicken", "cod", "salmon")),
        Map.entry("spider", Set.of("rabbit", "chicken", "frog")),
        Map.entry("cave_spider", Set.of("rabbit", "chicken", "frog")),
        Map.entry("enderman", Set.of("endermite", "silverfish")),
        Map.entry("creeper", Set.of("chicken", "rabbit")),
        Map.entry("skeleton", Set.of("rabbit", "chicken")),
        Map.entry("zombie", Set.of("sheep", "rabbit", "chicken")),
        // Alex's Mobs
        Map.entry("maned_wolf", Set.of("sheep", "rabbit", "chicken")),
        Map.entry("tiger", Set.of("sheep", "cow", "pig", "rabbit")),
        Map.entry("grizzly_bear", Set.of("salmon", "cod", "rabbit")),
        Map.entry("orca", Set.of("cod", "salmon", "squid")),
        Map.entry("bald_eagle", Set.of("rabbit", "chicken", "cod"))
    );

    private InstinctManager() {
    }

    public static boolean isWanderMorph(ResourceLocation morphId) {
        if (morphId == null || isStaticMorph(morphId)) {
            return false;
        }

        var profile = dev.naturalis.profile.MobProfileRegistry.getWander(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }

        String path = morphId.getPath();
        if (WANDER_MORPHS.contains(path)) {
            return true;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }

        MobCategory category = type.getCategory();
        if (category == MobCategory.MISC) {
            return false;
        }

        if (isFlightOnly(morphId)) {
            return false;
        }

        return category != MobCategory.WATER_AMBIENT
            && category != MobCategory.WATER_CREATURE
            && category != MobCategory.UNDERGROUND_WATER_CREATURE
            && category != MobCategory.AXOLOTLS;
    }

    public static boolean fearsWater(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "water")) {
            return true;
        }
        return FEAR_WATER.contains(morphId.getPath());
    }

    public static boolean fearsCats(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "cats")) {
            return true;
        }
        return FEAR_CATS.contains(morphId.getPath());
    }

    public static boolean fearsWolves(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "wolves")) {
            return true;
        }
        return FEAR_WOLVES.contains(morphId.getPath());
    }

    public static boolean fearsIronGolem(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "iron_golem")) {
            return true;
        }
        return FEAR_IRON_GOLEM.contains(morphId.getPath());
    }

    public static boolean fearsZoglin(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "zoglins")) {
            return true;
        }
        return FEAR_ZOGLIN.contains(morphId.getPath());
    }

    public static boolean fearsBees(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.hasFear(morphId, "bees")) {
            return true;
        }
        return FEAR_BEES.contains(morphId.getPath());
    }

    public static boolean isFlightOnly(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getFlightOnly(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }

        String path = morphId.getPath();
        return FLIGHT_ONLY.contains(path)
            || matchesAny(path, "vulture", "eagle", "hawk", "falcon", "crow", "raven", "harpy", "wyvern", "drake");
    }

    /** Sessile morphs that cannot walk (shulker, aechor plant, etc.). */
    public static boolean isStaticMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        var profile = dev.naturalis.profile.MobProfileRegistry.getStaticMorph(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        String path = morphId.getPath();
        return "shulker".equals(path)
            || path.contains("aechor")
            || path.endsWith("_plant")
            || path.contains("anemone")
            || path.contains("monolith");
    }

    /** Keen-nose morphs shown as the Scentbound trait. */
    public static boolean isScentbound(ResourceLocation morphId) {
        return getSmellStrength(morphId) >= 2;
    }

    /** Sun-vulnerable morphs shown as the Photophobic trait. */
    public static boolean isPhotophobic(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        var profile = dev.naturalis.profile.MobProfileRegistry.getSunlightSensitive(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        return isNyctalopHostile(morphId);
    }

    /**
     * Surface floaters — cannot dive; stay on water (chicken, duck, waterfowl, …).
     * Not true aquatics (fish / dolphins) who swim submerged.
     */
    public static boolean isFloatingMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        var profile = dev.naturalis.profile.MobProfileRegistry.getFloating(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        if (isAquaticDiver(morphId)) {
            return false;
        }
        String path = morphId.getPath();
        if (FLOATING_MORPHS.contains(path)
            || path.contains("duck")
            || path.contains("goose")
            || path.contains("chicken")
            || path.contains("waterfowl")
            || path.contains("seagull")
            || path.contains("pelican")
            || path.contains("swan")
            || path.contains("mallard")) {
            return true;
        }
        return dev.naturalis.profile.MobProfileRegistry.getTags(morphId).contains("waterfowl");
    }

    /** True water divers / swimmers (Walkers aquatic), not surface floaters. */
    public static boolean isAquaticDiver(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath();
        if (AQUATIC_DIVERS.contains(path)
            || path.contains("fish")
            || path.contains("dolphin")
            || path.contains("shark")
            || path.contains("whale")
            || path.contains("squid")
            || path.contains("axolotl")
            || path.contains("guardian")
            || path.contains("turtle")
            || path.contains("tadpole")) {
            return true;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }
        MobCategory category = type.getCategory();
        return category == MobCategory.WATER_AMBIENT
            || category == MobCategory.WATER_CREATURE
            || category == MobCategory.UNDERGROUND_WATER_CREATURE
            || category == MobCategory.AXOLOTLS;
    }

    public static boolean isClimbMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath();
        return CLIMB_MORPHS.contains(path)
            || path.contains("spider")
            || path.contains("gecko")
            || path.contains("lizard")
            || path.contains("climb");
    }

    public static boolean isCantSwimMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        if (isFloatingMorph(morphId) || isAquaticDiver(morphId)) {
            return false;
        }
        String path = morphId.getPath();
        return CANT_SWIM_MORPHS.contains(path)
            || path.contains("golem")
            || path.contains("ravager")
            || path.contains("warden")
            || path.endsWith("_golem");
    }

    public static boolean isUndrownableMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath();
        return UNDROWNABLE_MORPHS.contains(path)
            || path.contains("golem")
            || path.contains("undead")
            || "drowned".equals(path);
    }

    public static boolean isSlowFallingMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath();
        return SLOW_FALL_MORPHS.contains(path)
            || path.contains("chicken")
            || path.contains("parrot")
            || path.contains("owl");
    }

    public static boolean isPowderSnowWalker(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        if (dev.naturalis.profile.MobProfileRegistry.isSnowAdaptedMorph(morphId)) {
            return true;
        }
        String path = morphId.getPath();
        return POWDER_SNOW_MORPHS.contains(path)
            || path.contains("rabbit")
            || path.contains("goat")
            || path.contains("polar")
            || path.contains("yeti")
            || path.contains("arctic");
    }

    public static boolean isLavaWalker(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath();
        return LAVA_WALK_MORPHS.contains(path)
            || path.contains("strider")
            || path.contains("magma");
    }

    private static final Set<String> FLOATING_MORPHS = Set.of(
        "chicken", "duck"
    );

    private static final Set<String> AQUATIC_DIVERS = Set.of(
        "cod", "salmon", "tropical_fish", "pufferfish", "dolphin", "squid", "glow_squid",
        "guardian", "elder_guardian", "axolotl", "turtle", "tadpole", "frog"
    );

    private static final Set<String> CLIMB_MORPHS = Set.of(
        "spider", "cave_spider"
    );

    private static final Set<String> CANT_SWIM_MORPHS = Set.of(
        "iron_golem", "snow_golem", "ravager"
    );

    private static final Set<String> UNDROWNABLE_MORPHS = Set.of(
        "iron_golem", "snow_golem", "drowned"
    );

    private static final Set<String> SLOW_FALL_MORPHS = Set.of(
        "chicken", "parrot"
    );

    private static final Set<String> POWDER_SNOW_MORPHS = Set.of(
        "rabbit", "goat", "polar_bear", "snow_golem"
    );

    private static final Set<String> LAVA_WALK_MORPHS = Set.of(
        "strider", "magma_cube"
    );

    public static int getSmellStrength(ResourceLocation morphId) {
        if (morphId == null) {
            return 0;
        }

        var profile = dev.naturalis.profile.MobProfileRegistry.getSmellStrength(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }

        if ("minecraft".equals(morphId.getNamespace())) {
            int vanilla = getVanillaSmellStrength(morphId.getPath());
            if (vanilla >= 0) {
                return vanilla;
            }
        }

        String path = morphId.getPath();
        Integer mapped = SMELL_STRENGTH.get(path);
        if (mapped != null) {
            return mapped;
        }

        if (matchesAny(path, "wolf", "fox", "dog", "hound", "cat", "ocelot", "feline", "bear", "hyena")) {
            return 3;
        }
        if (matchesAny(path, "spider", "scorpion", "warden", "boar", "shark", "croc", "alligator", "gator")) {
            return 2;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null && type.getCategory() == MobCategory.MONSTER) {
            return 1;
        }

        return 0;
    }

    public static boolean hasSmellSense(ResourceLocation morphId) {
        return getSmellStrength(morphId) > 0;
    }

    public static boolean isNyctalopHostile(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getNyctalopHostile(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        return NYCTALOP_HOSTILE.contains(morphId.getPath());
    }

    public static Set<String> getHuntedPrey(ResourceLocation morphId) {
        if (isStaticMorph(morphId)) {
            return Set.of();
        }

        var profilePrey = dev.naturalis.profile.MobProfileRegistry.getHuntPrey(morphId);
        if (profilePrey.isPresent()) {
            return profilePrey.get();
        }

        if ("minecraft".equals(morphId.getNamespace())) {
            Set<String> vanilla = getVanillaHuntedPrey(morphId.getPath());
            if (vanilla != null) {
                return vanilla;
            }
        }

        Set<String> mapped = HUNT_PREY_BY_MORPH.get(morphId.getPath());
        if (mapped != null) {
            return mapped;
        }

        String path = morphId.getPath();
        if (matchesAny(path, "wolf", "fox", "cat", "ocelot", "feline", "canine", "raptor", "hawk", "eagle", "owl", "vulture", "shark", "croc")) {
            return Set.of("rabbit", "chicken", "cod", "salmon", "sheep", "frog");
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null && type.getCategory() == MobCategory.MONSTER) {
            return Set.of("rabbit", "chicken", "sheep");
        }

        return Set.of();
    }

    public static boolean isHunterMorph(ResourceLocation morphId) {
        if (isStaticMorph(morphId)) {
            return false;
        }
        return !getHuntedPrey(morphId).isEmpty();
    }

    public static boolean hunts(ResourceLocation hunterMorphId, ResourceLocation preyTypeId) {
        if (isStaticMorph(hunterMorphId)) {
            return false;
        }
        return getHuntedPrey(hunterMorphId).contains(preyTypeId.getPath());
    }

    private static boolean matchesAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static int getVanillaSmellStrength(String path) {
        return switch (path) {
            case "wolf", "fox", "warden" -> 3;
            case "cat", "ocelot", "spider", "cave_spider", "hoglin", "zoglin", "polar_bear", "ravager" -> 2;
            case "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged", "creeper",
                "enderman", "wither_skeleton", "piglin", "piglin_brute" -> 1;
            case "allay", "bat", "bee", "villager", "wandering_trader", "pillager", "vindicator", "evoker",
                "illusioner", "witch", "iron_golem", "snow_golem", "shulker", "breeze" -> 0;
            default -> -1;
        };
    }

    private static Set<String> getVanillaHuntedPrey(String path) {
        return switch (path) {
            case "wolf" -> Set.of("sheep", "rabbit", "chicken", "fox", "pig");
            case "fox" -> Set.of("rabbit", "chicken", "cod", "salmon", "tropical_fish");
            case "cat", "ocelot" -> Set.of("rabbit", "chicken", "cod", "salmon", "tropical_fish", "parrot");
            case "spider", "cave_spider" -> Set.of("rabbit", "chicken", "frog", "silverfish");
            case "enderman" -> Set.of("endermite", "silverfish");
            case "creeper" -> Set.of("chicken", "rabbit");
            case "skeleton", "stray", "bogged" -> Set.of("rabbit", "chicken", "fox");
            case "zombie", "husk", "drowned", "zombie_villager" -> Set.of("sheep", "rabbit", "chicken", "villager", "iron_golem");
            case "pillager", "vindicator", "evoker", "illusioner" -> Set.of("villager", "iron_golem", "wandering_trader");
            case "dolphin", "axolotl" -> Set.of("cod", "salmon", "tropical_fish", "squid", "glow_squid");
            case "iron_golem" -> Set.of("zombie", "zombie_villager", "husk", "drowned", "spider", "cave_spider");
            case "warden" -> Set.of("silverfish", "endermite");
            case "phantom" -> Set.of("cat", "ocelot");
            case "polar_bear" -> Set.of("salmon", "cod", "rabbit", "fox");
            case "hoglin", "zoglin", "ravager" -> Set.of("sheep", "cow", "pig", "villager", "iron_golem");
            case "guardian", "elder_guardian" -> Set.of("cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid");
            default -> null;
        };
    }
}
