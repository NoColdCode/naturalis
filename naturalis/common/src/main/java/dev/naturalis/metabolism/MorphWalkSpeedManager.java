package dev.naturalis.metabolism;

import dev.naturalis.profile.MobProfileRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.Map;

/**
 * Species gait: walk-speed multiplier independent of mass inertia.
 * Light snails must not inherit the "light = fast" inertia buff.
 */
public final class MorphWalkSpeedManager {

    /** 1.0 = normal player gait. Values below ~0.5 also suppress sprint. */
    private static final Map<String, Double> GAIT_BY_PATH = Map.ofEntries(
        Map.entry("snail", 0.18D),
        Map.entry("slug", 0.20D),
        Map.entry("sea_bunny", 0.22D),
        Map.entry("caterpillar", 0.28D),
        Map.entry("roly_poly", 0.38D),
        Map.entry("tortoise", 0.32D),
        Map.entry("turtle", 0.40D),
        Map.entry("giant_tortoise", 0.28D),
        Map.entry("slime", 0.48D),
        Map.entry("magma_cube", 0.48D),
        Map.entry("shulker", 0.05D),
        Map.entry("aechor_plant", 0.05D),
        Map.entry("snow_golem", 0.55D),
        Map.entry("strider", 0.70D),
        Map.entry("panda", 0.72D),
        Map.entry("cow", 0.78D),
        Map.entry("mooshroom", 0.78D),
        Map.entry("sheep", 0.82D),
        Map.entry("pig", 0.85D),
        Map.entry("villager", 0.88D),
        Map.entry("wandering_trader", 0.88D)
    );

    private MorphWalkSpeedManager() {
    }

    /**
     * Multiplier applied on top of mass inertia for {@link net.minecraft.world.entity.ai.attributes.Attributes#MOVEMENT_SPEED}.
     */
    public static double getGaitMultiplier(ResourceLocation morphId) {
        if (morphId == null) {
            return 1.0D;
        }
        var profile = MobProfileRegistry.getWalkSpeed(morphId);
        if (profile.isPresent()) {
            return Mth.clamp(profile.get(), 0.05D, 1.75D);
        }
        return Mth.clamp(heuristicGait(morphId.getPath().toLowerCase(Locale.ROOT)), 0.05D, 1.75D);
    }

    public static boolean canSprint(ResourceLocation morphId) {
        return getGaitMultiplier(morphId) >= 0.52D;
    }

    /** Extra jump scaling for crawl-gait morphs (snails barely hop). */
    public static double getJumpGaitMultiplier(ResourceLocation morphId) {
        double gait = getGaitMultiplier(morphId);
        if (gait >= 0.85D) {
            return 1.0D;
        }
        if (gait <= 0.25D) {
            return 0.28D;
        }
        if (gait <= 0.40D) {
            return 0.55D;
        }
        return Mth.clamp(0.55D + (gait - 0.40D) * 0.9D, 0.55D, 1.0D);
    }

    private static double heuristicGait(String path) {
        Double exact = GAIT_BY_PATH.get(path);
        if (exact != null) {
            return exact;
        }
        for (var entry : GAIT_BY_PATH.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (path.contains("snail") || path.contains("slug")) {
            return 0.18D;
        }
        if (path.contains("tortoise") || path.contains("turtle")) {
            return 0.35D;
        }
        if (path.contains("caterpillar") || path.contains("larva") || path.contains("grub")) {
            return 0.30D;
        }
        if (path.contains("golem") && !path.contains("iron")) {
            return 0.65D;
        }
        return 1.0D;
    }
}
