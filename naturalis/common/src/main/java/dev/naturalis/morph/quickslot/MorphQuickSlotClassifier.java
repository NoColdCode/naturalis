package dev.naturalis.morph.quickslot;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.instinct.InstinctManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Set;

final class MorphQuickSlotClassifier {

    private static final Set<String> AQUATIC_SPECIAL = Set.of(
        "turtle", "axolotl", "drowned", "frog",
        "orca", "giant_squid", "lobster", "seal", "hammerhead", "catfish", "blobfish",
        "mimic_octopus", "flying_fish", "cosmic_cod", "cachalot_whale",
        "alligator_snapping_turtle", "crocodilian", "platypus", "stradpole", "straddler",
        "dolphin", "guardian", "elder_guardian", "squid", "glow_squid", "cod", "salmon", "tropical_fish", "pufferfish"
    );

    private static final Set<String> FLYING_SPECIAL = Set.of(
        "bat", "bee", "parrot", "phantom", "ender_dragon", "ghast", "blaze", "allay", "vex", "happy_ghast",
        "crow", "bald_eagle", "toucan", "sunbird", "hummingbird", "flutter",
        "spectre", "void_worm", "warped_mosco", "tarantula_hawk",
        "blue_jay", "seagull"
    );

    private static final Set<String> NETHER_SPECIAL = Set.of(
        "blaze", "ghast", "magma_cube", "piglin", "piglin_brute", "zombified_piglin",
        "hoglin", "zoglin", "strider", "wither_skeleton", "wither"
    );

    private MorphQuickSlotClassifier() {
    }

    static boolean matches(ResourceLocation morphId, MorphQuickSlotCategory category) {
        if (morphId == null) {
            return false;
        }

        var profileCategories = dev.naturalis.profile.MobProfileRegistry.getQuickSlotCategories(morphId);
        if (profileCategories.isPresent()) {
            return profileCategories.get().contains(category);
        }

        return switch (category) {
            case GROUND -> isGround(morphId);
            case AERIAL -> isAerial(morphId);
            case AQUATIC -> isAquatic(morphId);
            case NETHER -> isNether(morphId);
            case HOSTILE -> isHostile(morphId);
            case HIGH_DAMAGE -> isHighDamage(morphId);
        };
    }

    static MorphQuickSlotCategory primaryCategory(ResourceLocation morphId) {
        var profilePrimary = dev.naturalis.profile.MobProfileRegistry.getQuickSlotPrimary(morphId);
        if (profilePrimary.isPresent()) {
            return profilePrimary.get();
        }

        if (isHighDamage(morphId)) {
            return MorphQuickSlotCategory.HIGH_DAMAGE;
        }
        if (isHostile(morphId)) {
            return MorphQuickSlotCategory.HOSTILE;
        }
        if (isNether(morphId)) {
            return MorphQuickSlotCategory.NETHER;
        }
        if (isAquatic(morphId)) {
            return MorphQuickSlotCategory.AQUATIC;
        }
        if (isAerial(morphId)) {
            return MorphQuickSlotCategory.AERIAL;
        }
        return MorphQuickSlotCategory.GROUND;
    }

    private static boolean isGround(ResourceLocation morphId) {
        return !isAerial(morphId) && !isAquatic(morphId) && !isNether(morphId);
    }

    private static boolean isAerial(ResourceLocation morphId) {
        String path = morphId.getPath();
        if (FLYING_SPECIAL.contains(path) || InstinctManager.isFlightOnly(morphId)) {
            return true;
        }
        return containsAny(path, "bird", "fly", "wing", "hawk", "eagle", "owl", "vulture", "harpy", "wyvern", "drake");
    }

    private static boolean isAquatic(ResourceLocation morphId) {
        String path = morphId.getPath();
        if (AQUATIC_SPECIAL.contains(path)) {
            return true;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.WATER_AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AXOLOTLS) {
                return true;
            }
        }
        return containsAny(path, "fish", "shark", "whale", "dolphin", "squid", "axolotl", "frog", "turtle", "aquatic", "salmon", "cod");
    }

    private static boolean isNether(ResourceLocation morphId) {
        String path = morphId.getPath();
        if (NETHER_SPECIAL.contains(path)) {
            return true;
        }
        return containsAny(path, "piglin", "hoglin", "strider", "blaze", "ghast", "wither", "zoglin", "nether");
    }

    private static boolean isHostile(ResourceLocation morphId) {
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null && type.getCategory() == MobCategory.MONSTER) {
            return true;
        }
        return InstinctManager.isNyctalopHostile(morphId) || InstinctManager.isHunterMorph(morphId);
    }

    private static boolean isHighDamage(ResourceLocation morphId) {
        String path = morphId.getPath();
        if (containsAny(path, "warden", "ravager", "iron_golem", "bear", "tiger", "dragon", "wither",
            "guardian", "elder", "hoglin", "piglin_brute", "zoglin", "crocodile", "croc", "rhino", "elephant")) {
            return true;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        return type != null && type.getCategory() == MobCategory.MONSTER
            && containsAny(path, "boss", "golem", "brute", "king", "queen");
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
