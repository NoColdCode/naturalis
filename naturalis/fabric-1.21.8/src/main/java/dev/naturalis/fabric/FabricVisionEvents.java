package dev.naturalis.fabric;

import dev.naturalis.client.MorphPostEffectHelper;
import dev.naturalis.client.MorphVisionPaletteUniforms;
import dev.naturalis.util.CurrentMorphUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class FabricVisionEvents {

    private static final ResourceLocation WOLF_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "wolf_vision");
    private static final ResourceLocation MAMMAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "mammal_vision");
    private static final ResourceLocation AVIAN_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "avian_vision");
    private static final ResourceLocation AQUATIC_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "aquatic_vision");
    private static final ResourceLocation REPTILE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "reptile_vision");
    private static final ResourceLocation UNDEAD_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "undead_vision");
    private static final ResourceLocation NETHER_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "nether_vision");
    private static final ResourceLocation ARCANE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "arcane_vision");
    private static final ResourceLocation INSECT_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "insect_vision");
    private static final ResourceLocation CEPHALOPOD_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "cephalopod_vision");
    private static final ResourceLocation ABYSSAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "abyssal_vision");
    private static final ResourceLocation FUNGAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "fungal_vision");
    private static final ResourceLocation CRYSTALLINE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "crystalline_vision");
    private static final ResourceLocation FERROUS_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "ferrous_vision");
    private static final ResourceLocation FAE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "fae_vision");
    private static final ResourceLocation TEMPEST_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "tempest_vision");
    private static final ResourceLocation VISCOUS_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "viscous_vision");
    private static final ResourceLocation VOID_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath("naturalis", "void_vision");

    private static final ResourceLocation CREEPER_SHADER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");
    private static final ResourceLocation VANILLA_SPIDER_SHADER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "spider");
    private static final ResourceLocation INVERT_SHADER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "invert");

    private static final Set<String> WOLF_LIKE = Set.of("wolf", "fox", "cat", "ocelot");
    private static final Set<String> REAL_MAMMALS = Set.of(
        "armadillo", "bat", "camel", "cow", "donkey", "horse", "llama", "trader_llama", "mule",
        "panda", "pig", "polar_bear", "rabbit", "sheep", "sniffer", "goat", "hoglin", "zoglin"
    );
    private static final Set<String> BIRDS = Set.of("chicken", "parrot");
    private static final Set<String> AQUATIC = Set.of(
        "axolotl", "cod", "salmon", "tropical_fish", "pufferfish", "dolphin", "tadpole"
    );
    private static final Set<String> REPTILES_AMPHIBIANS = Set.of("frog", "sniffer", "turtle", "tadpole", "axolotl");
    private static final Set<String> UNDEAD = Set.of(
        "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged",
        "wither_skeleton", "phantom", "zombified_piglin", "skeleton_horse", "zombie_horse", "wither"
    );
    private static final Set<String> NETHER = Set.of(
        "blaze", "ghast", "magma_cube", "piglin", "piglin_brute", "strider", "hoglin", "zoglin"
    );
    private static final Set<String> HUMANOID_NORMAL_VISION = Set.of(
        "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner", "witch"
    );

    private static boolean shaderActive = false;
    private static CameraType lastCameraType = null;
    private static ResourceLocation activeShader = null;

    private FabricVisionEvents() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricVisionEvents::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.gameRenderer == null) {
            disableShader(mc);
            lastCameraType = null;
            activeShader = null;
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        ResourceLocation desiredShader = pickShaderForMorph(morphId);
        CameraType currentCameraType = mc.options.getCameraType();
        boolean cameraChanged = currentCameraType != lastCameraType;
        boolean shaderChanged = desiredShader == null ? activeShader != null : !desiredShader.equals(activeShader);

        if (desiredShader != null) {
            enableShader(mc, desiredShader, cameraChanged || shaderChanged);
        } else {
            disableShader(mc);
        }

        lastCameraType = currentCameraType;

        if (shaderActive && morphId != null) {
            MorphVisionPaletteUniforms.tick(mc, morphId, activeShader);
        }
    }

    private static ResourceLocation pickAlexsMobsShader(String id) {
        if (matchesAny(id, "rattlesnake", "crocodile", "komodo", "anaconda", "terrapin")
            || matchesAny(id, "snake", "lizard", "gecko", "iguana", "toad", "salamander", "frog")) {
            return REPTILE_VISION_SHADER;
        }
        if (matchesAny(id, "squid", "octopus", "cuttlefish", "nautilus")) {
            return CEPHALOPOD_VISION_SHADER;
        }
        if (matchesAny(id, "blobfish", "angler", "frilled") || matchesAny(id, "shark")) {
            return ABYSSAL_VISION_SHADER;
        }
        if (matchesAny(id, "centipede", "mosquito", "fly", "cockroach", "ant", "leafcutter", "tarantula_hawk",
            "tarantula", "jumping_spider")) {
            return INSECT_VISION_SHADER;
        }
        if (matchesAny(id, "mungus", "fungus")) {
            return FUNGAL_VISION_SHADER;
        }
        if (matchesAny(id, "warped_mosco", "warped_toad", "straddler", "stradpole", "dropbear", "soul_vulture")) {
            return NETHER_VISION_SHADER;
        }
        if (matchesAny(id, "skelewag", "bone_serpent", "mummy", "zombie", "undead")) {
            return UNDEAD_VISION_SHADER;
        }
        if (matchesAny(id, "hummingbird", "crow", "jay", "roadrunner", "emu", "cassowary", "toucan",
            "shoebill", "cockatoo", "sunbird", "booby", "manakin", "skua", "tropicbird")) {
            return AVIAN_VISION_SHADER;
        }
        if (matchesAny(id, "orca", "lobster", "crab", "whale", "seal")
            || matchesAny(id, "fish", "mantis_shrimp", "shrimp", "jelly")) {
            return AQUATIC_VISION_SHADER;
        }
        return MAMMAL_VISION_SHADER;
    }

    private static void enableShader(Minecraft mc, ResourceLocation shader, boolean forceReload) {
        if (shaderActive && !forceReload) {
            return;
        }
        if (shaderActive) {
            stopShader(mc);
        }
        startShader(mc, shader);
        shaderActive = true;
        activeShader = shader;
    }

    private static void disableShader(Minecraft mc) {
        if (!shaderActive || mc.gameRenderer == null) {
            return;
        }
        stopShader(mc);
        shaderActive = false;
        activeShader = null;
    }

    private static void startShader(Minecraft mc, ResourceLocation shader) {
        MorphPostEffectHelper.apply(mc, shader);
    }

    private static void stopShader(Minecraft mc) {
        MorphPostEffectHelper.clear(mc);
    }

    private static ResourceLocation pickShaderForMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return null;
        }

        String id = morphId.getPath();

        if ("minecraft".equals(morphId.getNamespace())) {
            return pickVanillaShader(id);
        }

        if ("alexsmobs".equals(morphId.getNamespace())) {
            return pickAlexsMobsShader(id);
        }

        if (HUMANOID_NORMAL_VISION.contains(id)) {
            return null;
        }
        if ("creeper".equals(id)) {
            return CREEPER_SHADER;
        }
        if ("spider".equals(id) || "cave_spider".equals(id)) {
            return VANILLA_SPIDER_SHADER;
        }
        if ("enderman".equals(id)) {
            return INVERT_SHADER;
        }
        if ("bee".equals(id) || "silverfish".equals(id)
            || matchesAny(id, "beetle", "wasp", "hornet", "moth", "termite", "dragonfly", "firefly", "locust", "cricket", "aphid")) {
            return INSECT_VISION_SHADER;
        }
        if ("squid".equals(id) || "glow_squid".equals(id)
            || matchesAny(id, "octopus", "cuttlefish", "nautilus")) {
            return CEPHALOPOD_VISION_SHADER;
        }
        if ("guardian".equals(id) || "elder_guardian".equals(id)
            || matchesAny(id, "blobfish", "angler", "frilled")) {
            return ABYSSAL_VISION_SHADER;
        }
        if ("mooshroom".equals(id) || matchesAny(id, "fungus", "mycel", "spore", "mushroom")) {
            return FUNGAL_VISION_SHADER;
        }

        if (WOLF_LIKE.contains(id)) {
            return WOLF_VISION_SHADER;
        }
        if (BIRDS.contains(id) || matchesAny(id, "bird", "crow", "raven", "eagle", "hawk", "falcon", "owl", "vulture", "gull", "duck", "goose", "swan")) {
            return AVIAN_VISION_SHADER;
        }
        if (AQUATIC.contains(id) || matchesAny(id, "fish", "shark", "whale", "dolphin", "ray", "jelly", "eel")) {
            return AQUATIC_VISION_SHADER;
        }
        if (REPTILES_AMPHIBIANS.contains(id) || matchesAny(id, "snake", "serpent", "lizard", "gecko", "iguana", "croc", "alligator", "gator", "toad", "newt", "salamander")) {
            return REPTILE_VISION_SHADER;
        }

        if ("snow_golem".equals(id) || matchesAny(id, "ice_golem", "frost", "crystalline")) {
            return CRYSTALLINE_VISION_SHADER;
        }
        if ("iron_golem".equals(id) || "ravager".equals(id)
            || matchesAny(id, "copper_golem", "steel", "construct")) {
            return FERROUS_VISION_SHADER;
        }
        if ("allay".equals(id) || "vex".equals(id)
            || matchesAny(id, "pixie", "sprite", "fairy")) {
            return FAE_VISION_SHADER;
        }
        if ("breeze".equals(id) || matchesAny(id, "storm", "tempest", "volt")) {
            return TEMPEST_VISION_SHADER;
        }
        if ("slime".equals(id) || matchesAny(id, "ooze", "gelatin")) {
            return VISCOUS_VISION_SHADER;
        }
        if ("endermite".equals(id) || "shulker".equals(id) || "warden".equals(id) || "ender_dragon".equals(id)
            || matchesAny(id, "voidling", "enderman_aspect")) {
            return VOID_VISION_SHADER;
        }

        if (REAL_MAMMALS.contains(id)) {
            return MAMMAL_VISION_SHADER;
        }
        if (UNDEAD.contains(id)) {
            return UNDEAD_VISION_SHADER;
        }
        if (NETHER.contains(id) || matchesAny(id, "imp", "demon", "hell", "infernal", "nether")) {
            return NETHER_VISION_SHADER;
        }
        if (matchesAny(id, "arcane", "rune", "enchant", "spectral", "mana")) {
            return ARCANE_VISION_SHADER;
        }

        return MAMMAL_VISION_SHADER;
    }

    private static ResourceLocation pickVanillaShader(String id) {
        return switch (id) {
            case "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner", "witch" -> null;

            case "creeper" -> CREEPER_SHADER;
            case "spider", "cave_spider" -> VANILLA_SPIDER_SHADER;
            case "enderman" -> INVERT_SHADER;

            case "wolf", "fox", "cat", "ocelot" -> WOLF_VISION_SHADER;

            case "bee", "silverfish" -> INSECT_VISION_SHADER;

            case "squid", "glow_squid" -> CEPHALOPOD_VISION_SHADER;

            case "guardian", "elder_guardian" -> ABYSSAL_VISION_SHADER;

            case "mooshroom" -> FUNGAL_VISION_SHADER;

            case "snow_golem" -> CRYSTALLINE_VISION_SHADER;

            case "iron_golem", "ravager" -> FERROUS_VISION_SHADER;

            case "allay", "vex" -> FAE_VISION_SHADER;

            case "breeze" -> TEMPEST_VISION_SHADER;

            case "slime" -> VISCOUS_VISION_SHADER;

            case "endermite", "shulker", "warden", "ender_dragon" -> VOID_VISION_SHADER;

            case "chicken", "parrot" -> AVIAN_VISION_SHADER;

            case "bat" -> MAMMAL_VISION_SHADER;

            case "axolotl", "cod", "salmon", "tropical_fish", "pufferfish", "dolphin", "tadpole" -> AQUATIC_VISION_SHADER;

            case "frog", "turtle", "sniffer" -> REPTILE_VISION_SHADER;

            case "armadillo", "camel", "cow", "donkey", "horse", "llama", "trader_llama",
                 "mule", "panda", "pig", "polar_bear", "rabbit", "sheep",
                 "goat" -> MAMMAL_VISION_SHADER;

            case "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged",
                 "wither_skeleton", "phantom", "zombified_piglin", "skeleton_horse",
                 "zombie_horse", "wither" -> UNDEAD_VISION_SHADER;

            case "blaze", "ghast", "magma_cube", "piglin", "piglin_brute",
                 "strider", "hoglin", "zoglin" -> NETHER_VISION_SHADER;

            default -> MAMMAL_VISION_SHADER;
        };
    }

    private static boolean matchesAny(String id, String... keywords) {
        for (String kw : keywords) {
            if (id.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
