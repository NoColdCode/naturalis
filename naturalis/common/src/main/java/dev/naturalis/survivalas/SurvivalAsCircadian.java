package dev.naturalis.survivalas;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/**
 * Nerfed day/night identity rhythm for Survival-as worlds.
 * Mild comfort in the natural phase, mild stress out of phase — never as harsh as full sunburn.
 */
public final class SurvivalAsCircadian {

    public enum Rhythm {
        NOCTURNAL,
        DIURNAL,
        NONE
    }

    private static final int TICK_INTERVAL = 40;
    private static final int EFFECT_DURATION = 50;

    private SurvivalAsCircadian() {
    }

    public static void tick(ServerPlayer player) {
        if (!SurvivalAsWorldStorage.isEnabled()) {
            return;
        }
        if (player.tickCount % TICK_INTERVAL != 0) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        Rhythm rhythm = classify(morphId);
        if (rhythm == Rhythm.NONE) {
            return;
        }

        Level level = player.level();
        long dayTime = level.getDayTime() % 24000L;
        boolean daytime = dayTime < 12000L;
        boolean nighttime = dayTime >= 13000L && dayTime < 23000L;
        boolean openDay = daytime
            && EnvironmentalSusceptibilityManager.isClearSunnyExposure(level, player.blockPosition());

        switch (rhythm) {
            case NOCTURNAL -> {
                if (openDay) {
                    // Skip stacking with Walkers daylight fire — shade still feels wrong.
                    if (!player.isOnFire()) {
                        applyStress(player);
                    }
                } else if (nighttime) {
                    applyComfort(player);
                }
            }
            case DIURNAL -> {
                if (nighttime) {
                    applyStress(player);
                } else if (daytime) {
                    applyComfort(player);
                }
            }
            default -> {
            }
        }
    }

    public static Rhythm classify(ResourceLocation morphId) {
        if (morphId == null) {
            return Rhythm.NONE;
        }
        String path = morphId.getPath();

        if (InstinctManager.isPhotophobic(morphId)
            || InstinctManager.isNyctalopHostile(morphId)
            || EnvironmentalSusceptibilityManager.isSunlightSensitive(morphId)
            || path.contains("bat")
            || path.contains("owl")
            || path.contains("phantom")
            || path.contains("spider")
            || path.contains("enderman")) {
            return Rhythm.NOCTURNAL;
        }

        if (path.contains("bee")
            || path.contains("parrot")
            || path.contains("chicken")
            || path.contains("cow")
            || path.contains("sheep")
            || path.contains("pig")
            || path.contains("horse")
            || path.contains("villager")
            || path.contains("iron_golem")
            || path.contains("sniffer")) {
            return Rhythm.DIURNAL;
        }

        var type = CompatAccess.getEntityType(morphId);
        if (type != null && type.getCategory() == MobCategory.CREATURE) {
            return Rhythm.DIURNAL;
        }
        if (type != null && type.getCategory() == MobCategory.MONSTER) {
            return Rhythm.NOCTURNAL;
        }
        return Rhythm.NONE;
    }

    private static void applyStress(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
            CompatAccess.resolveMobEffect("SLOWNESS", "MOVEMENT_SLOWDOWN"),
            EFFECT_DURATION, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, false, true));
        player.causeFoodExhaustion(0.15F);
    }

    private static void applyComfort(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
            CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED"),
            EFFECT_DURATION, 0, false, false, true));
    }
}
