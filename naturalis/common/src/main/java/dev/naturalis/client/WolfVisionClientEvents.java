package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class WolfVisionClientEvents {

    private static final ResourceLocation WOLF_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "wolf_vision");
    private static final ResourceLocation MAMMAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "mammal_vision");
    private static final ResourceLocation AVIAN_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "avian_vision");
    private static final ResourceLocation AQUATIC_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "aquatic_vision");
    private static final ResourceLocation REPTILE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "reptile_vision");
    private static final ResourceLocation UNDEAD_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "undead_vision");
    private static final ResourceLocation NETHER_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "nether_vision");
    private static final ResourceLocation ARCANE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "arcane_vision");
    private static final ResourceLocation INSECT_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "insect_vision");
    private static final ResourceLocation CEPHALOPOD_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "cephalopod_vision");
    private static final ResourceLocation ABYSSAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "abyssal_vision");
    private static final ResourceLocation FUNGAL_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "fungal_vision");
    private static final ResourceLocation CRYSTALLINE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "crystalline_vision");
    private static final ResourceLocation FERROUS_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "ferrous_vision");
    private static final ResourceLocation FAE_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "fae_vision");
    private static final ResourceLocation TEMPEST_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "tempest_vision");
    private static final ResourceLocation VISCOUS_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "viscous_vision");
    private static final ResourceLocation VOID_VISION_SHADER =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "void_vision");

    private static final ResourceLocation CREEPER_SHADER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");
    /** Vanilla Minecraft spider post chain (blur / spiderclip) — keep stock multi-eye treatment. */
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
        "villager",
        "wandering_trader",
        "pillager",
        "vindicator",
        "evoker",
        "illusioner",
        "witch"
    );

    private static boolean shaderActive = false;
    private static CameraType lastCameraType = null;
    private static ResourceLocation activeShader = null;

    private WolfVisionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameRenderer == null) {
            disableShader(mc);
            lastCameraType = null;
            activeShader = null;
            return;
        }

        if (!NaturalisConfig.isClientVisionEnabled() || !RuleFlagsClientCache.isColorFilterEnabled()) {
            disableShader(mc);
            lastCameraType = mc.options.getCameraType();
            activeShader = null;
            return;
        }

        // Post chains tank the render thread — drop them while any GUI is open so menus stay usable.
        if (mc.screen != null) {
            disableShader(mc);
            lastCameraType = mc.options.getCameraType();
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
        if (!MorphPostEffectHelper.apply(mc, shader)) {
            loadLegacyPostChain(mc, shader);
        }
    }

    /** Pre-1.21.8 / fallback: loadEffect with legacy shaders/post/*.json */
    private static void loadLegacyPostChain(Minecraft mc, ResourceLocation shader) {
        Object renderer = mc.gameRenderer;
        if (renderer == null) {
            return;
        }
        ResourceLocation legacyPath = ResourceLocation.fromNamespaceAndPath(
            shader.getNamespace(),
            "shaders/post/" + shader.getPath() + ".json"
        );
        try {
            renderer.getClass().getMethod("loadEffect", ResourceLocation.class).invoke(renderer, legacyPath);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    private static void stopShader(Minecraft mc) {
        MorphPostEffectHelper.clear(mc);
        Object renderer = mc.gameRenderer;
        if (renderer == null) {
            return;
        }

        try {
            renderer.getClass().getMethod("shutdownEffect").invoke(renderer);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            renderer.getClass().getMethod("clearPostEffect").invoke(renderer);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    /** Mojmap 1.21.1 uses {@code currentEffect()}; some versions expose {@code getPostEffect()}. */
    private static boolean hasActivePostEffect(Minecraft mc) {
        Object renderer = mc.gameRenderer;
        if (renderer == null) {
            return false;
        }
        for (String name : new String[] {"currentEffect", "getPostEffect", "getPostProcessor"}) {
            try {
                Object raw = renderer.getClass().getMethod(name).invoke(renderer);
                if (raw instanceof Optional<?> opt) {
                    raw = opt.orElse(null);
                }
                if (raw != null) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try next name.
            }
        }
        return false;
    }

    private static ResourceLocation pickShaderForMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return null;
        }

        var profileShader = dev.naturalis.profile.MobProfileRegistry.getVisionShader(morphId);
        if (profileShader.isPresent()) {
            return profileShader.get();
        }

        String id = morphId.getPath();

        if ("minecraft".equals(morphId.getNamespace())) {
            return pickVanillaShader(id);
        }

        if ("alexsmobs".equals(morphId.getNamespace())) {
            return pickAlexsMobsShader(id);
        }

        // Humanoid morphs keep a natural first-person perception.
        if (HUMANOID_NORMAL_VISION.contains(id)) {
            return null;
        }

        // Requested built-ins.
        if ("creeper".equals(id)) {
            return CREEPER_SHADER;
        }
        if ("spider".equals(id) || "cave_spider".equals(id)) {
            return VANILLA_SPIDER_SHADER;
        }
        if ("enderman".equals(id)) {
            return INVERT_SHADER;
        }

        // Smaller specialized palettes before broad buckets.
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

        // Real-animal inspired groups.
        if (WOLF_LIKE.contains(id)) {
            return NaturalisConfig.clientVisionUseWolfLegacyShader() ? WOLF_VISION_SHADER : MAMMAL_VISION_SHADER;
        }
        if (BIRDS.contains(id)
            || matchesAny(id, "bird", "crow", "raven", "eagle", "hawk", "falcon", "owl", "vulture", "gull", "duck", "goose", "swan")) {
            return AVIAN_VISION_SHADER;
        }
        if (AQUATIC.contains(id)
            || matchesAny(id, "fish", "shark", "whale", "dolphin", "ray", "jelly", "eel")) {
            return AQUATIC_VISION_SHADER;
        }
        if (REPTILES_AMPHIBIANS.contains(id)
            || matchesAny(id, "snake", "serpent", "lizard", "gecko", "iguana", "croc", "alligator", "gator", "toad", "newt", "salamander")) {
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

        // Fictional groups.
        if (UNDEAD.contains(id)) {
            return UNDEAD_VISION_SHADER;
        }
        if (NETHER.contains(id)
            || matchesAny(id, "imp", "demon", "hell", "infernal", "nether")) {
            return NETHER_VISION_SHADER;
        }

        if (matchesAny(id, "arcane", "rune", "enchant", "spectral", "mana")) {
            return ARCANE_VISION_SHADER;
        }

        // Ensure every morph still gets an effect.
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

            case "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged", "wither_skeleton", "phantom", "zombified_piglin", "skeleton_horse", "zombie_horse", "wither" -> UNDEAD_VISION_SHADER;

            case "blaze", "ghast", "magma_cube", "piglin", "piglin_brute", "strider", "hoglin", "zoglin" -> NETHER_VISION_SHADER;

            // Remaining vanilla creatures use neutral mammal-ish perception.
            default -> MAMMAL_VISION_SHADER;
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
}
