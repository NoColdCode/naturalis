package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ViewportEvent;

import java.util.Set;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphFovClientEvents {

    // Keep multipliers noticeable but playable, even on high user FOV.
    private static final double HORSE_FOV_MULTIPLIER = 1.40D;
    private static final double CANINE_FOV_MULTIPLIER = 1.20D;
    private static final double FELINE_FOV_MULTIPLIER = 1.16D;
    private static final double PREY_FOV_MULTIPLIER = 1.12D;
    private static final double SPIDER_FOV_MULTIPLIER = 1.14D;
    private static final double AQUATIC_FOV_MULTIPLIER = 1.10D;
    private static final double AVIAN_FOV_MULTIPLIER = 1.18D;

    // Very high world FOV can expose the full first-person arm; keep it below that threshold.
    private static final double MAX_MORPH_FOV = 150.0D;

    private static final Set<String> CANINE_MORPHS = Set.of(
        "wolf",
        "fox"
    );

    private static final Set<String> HORSE_MORPHS = Set.of(
        "horse",
        "skeleton_horse",
        "zombie_horse"
    );

    private static final Set<String> FELINE_MORPHS = Set.of(
        "cat",
        "ocelot"
    );

    private static final Set<String> PREY_MORPHS = Set.of(
        "rabbit",
        "deer",
        "goat",
        "sheep",
        "llama",
        "camel"
    );

    private static final Set<String> SPIDER_MORPHS = Set.of(
        "spider",
        "cave_spider"
    );

    private static final Set<String> AQUATIC_MORPHS = Set.of(
        "dolphin",
        "cod",
        "salmon",
        "tropical_fish",
        "pufferfish",
        "axolotl"
    );

    private static final Set<String> AVIAN_MORPHS = Set.of(
        "chicken",
        "parrot",
        "phantom",
        "bat"
    );

    private MorphFovClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getCameraEntity() != mc.player) {
            return;
        }

        double multiplier = getActiveMorphFovMultiplier(mc);
        if (Math.abs(multiplier - 1.0D) < 1.0E-6D) {
            return;
        }

        double baseFov = event.getFOV();
        // Multiply on top of user FOV so Quake Pro still gets a visible morph shift.
        double adjustedFov = baseFov * multiplier;
        event.setFOV((float) Math.max(30.0D, Math.min(MAX_MORPH_FOV, adjustedFov)));
    }

    static double getActiveMorphFovMultiplier(Minecraft mc) {
        if (mc.player == null || mc.getCameraEntity() != mc.player) {
            return 1.0D;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (morphId == null) {
            return 1.0D;
        }

        return getMorphFovMultiplier(morphId);
    }

    private static double getMorphFovMultiplier(ResourceLocation morphId) {
        if ("minecraft".equals(morphId.getNamespace())) {
            Double vanilla = getVanillaFovMultiplier(morphId.getPath());
            if (vanilla != null) {
                return vanilla;
            }
        }

        String path = morphId.getPath();
        if (AVIAN_MORPHS.contains(path)
            || matchesAny(path, "bird", "crow", "raven", "eagle", "hawk", "falcon", "owl", "vulture", "gull", "duck", "goose", "swan")) {
            return AVIAN_FOV_MULTIPLIER;
        }
        if (HORSE_MORPHS.contains(path)) {
            return HORSE_FOV_MULTIPLIER;
        }
        if (CANINE_MORPHS.contains(path)) {
            return CANINE_FOV_MULTIPLIER;
        }
        if (FELINE_MORPHS.contains(path)) {
            return FELINE_FOV_MULTIPLIER;
        }
        if (SPIDER_MORPHS.contains(path)) {
            return SPIDER_FOV_MULTIPLIER;
        }
        if (AQUATIC_MORPHS.contains(path)) {
            return AQUATIC_FOV_MULTIPLIER;
        }
        if (PREY_MORPHS.contains(path)) {
            return PREY_FOV_MULTIPLIER;
        }
        return 1.0D;
    }

    private static Double getVanillaFovMultiplier(String path) {
        return switch (path) {
            case "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner", "witch" -> 1.00D;

            case "horse", "skeleton_horse", "zombie_horse", "donkey", "mule", "camel", "llama", "trader_llama" -> HORSE_FOV_MULTIPLIER;
            case "wolf", "fox" -> CANINE_FOV_MULTIPLIER;
            case "cat", "ocelot" -> FELINE_FOV_MULTIPLIER;
            case "spider", "cave_spider", "silverfish", "endermite" -> SPIDER_FOV_MULTIPLIER;
            case "axolotl", "cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin", "guardian", "elder_guardian", "tadpole" -> AQUATIC_FOV_MULTIPLIER;
            case "bat", "bee", "chicken", "parrot", "phantom", "allay", "vex", "blaze", "ghast", "ender_dragon" -> AVIAN_FOV_MULTIPLIER;
            case "rabbit", "sheep", "goat", "cow", "mooshroom", "pig", "frog", "turtle", "sniffer", "armadillo" -> PREY_FOV_MULTIPLIER;

            case "ravager", "warden", "iron_golem", "snow_golem", "hoglin", "zoglin", "polar_bear", "wither", "shulker", "breeze" -> 1.08D;
            case "enderman", "creeper", "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged", "wither_skeleton", "piglin", "piglin_brute", "zombified_piglin", "magma_cube", "slime", "strider" -> 1.05D;

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
}
