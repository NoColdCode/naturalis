package dev.naturalis.environment;

import dev.naturalis.NaturalisMod;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.ForceHumanBridge;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.worldgen.NaturalBiomeSuitability;
import dev.naturalis.worldgen.NaturalDimensionKeys;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class EnvironmentalSusceptibilityLogic {

    private static final int EFFECT_REAPPLY_INTERVAL = 60; // 3 seconds
    private static final int EFFECT_DURATION = 80; // 4 seconds (will be reapplied before expiring)
    private static final int NYCTALOP_NIGHT_VISION_DURATION = 520; // Keep comfortably above the fade threshold to avoid blinking.

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root");
    private static final ResourceLocation ADV_COLD_HARDY = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "environment/cold_hardy");
    private static final ResourceLocation ADV_HEAT_RESISTANT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "environment/heat_resistant");
    private static final ResourceLocation ADV_WATERBORNE = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "environment/waterborne");
    private static final ResourceLocation ADV_DEHYDRATED = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "environment/dehydrated");
    private static final ResourceLocation ADV_NIGHT_STALKER = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "environment/night_stalker");

    private static final String ENV_DATA_PREFIX = NaturalisMod.ID + ":env_";
    private static final String ENV_COLD_TIMER = ENV_DATA_PREFIX + "cold_timer";
    private static final String ENV_HOT_TIMER = ENV_DATA_PREFIX + "hot_timer";
    private static final String ENV_WET_TIMER = ENV_DATA_PREFIX + "wet_timer";
    private static final String ENV_DRY_TIMER = ENV_DATA_PREFIX + "dry_timer";
    private static final String ENV_CURRENT_CONDITION = ENV_DATA_PREFIX + "condition";

    private EnvironmentalSusceptibilityLogic() {
    }

    public static void tick(ServerPlayer player) {
        // Fast-path: enforce forbidden biome revert every 5 ticks for quick response.
        if (player.tickCount % 5 == 0) {
            applyForbiddenBiomeRevert(player);
            stripMorphNightVisionUnderPhotophobia(player);
        }

        // Only check every EFFECT_REAPPLY_INTERVAL ticks
        if (player.tickCount % EFFECT_REAPPLY_INTERVAL != 0) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            // Clear all environmental effects if no longer morphed
            clearEnvironmentalEffects(player);
            return;
        }

        applyNyctalopVision(player, morphId);
        applyNaturalBiomeSuitability(player, morphId);
        applyEnvironmentalEffects(player, morphId);
    }

    private static void stripMorphNightVisionUnderPhotophobia(ServerPlayer player) {
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }
        if (!EnvironmentalSusceptibilityManager.shouldSuppressMorphNightVision(player.level(), player.blockPosition(), morphId)) {
            return;
        }
        player.removeEffect(MobEffects.NIGHT_VISION);
    }

    private static void applyForbiddenBiomeRevert(ServerPlayer player) {
        if (!player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }
        var biomeHolder = player.level().getBiome(player.blockPosition());
        NaturalBiomeSuitability.Suitability suitability = NaturalBiomeSuitability.evaluate(morphId, biomeHolder);
        if (suitability == NaturalBiomeSuitability.Suitability.FORBIDDEN) {
            ForceHumanBridge.forceHuman(player);
            if (player.tickCount % 40 == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.natural_dimension.unsuited"), true);
            }
        }
    }

    private static void applyNaturalBiomeSuitability(ServerPlayer player, ResourceLocation morphId) {
        if (!player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }

        var biomeHolder = player.level().getBiome(player.blockPosition());
        NaturalBiomeSuitability.Suitability suitability = NaturalBiomeSuitability.evaluate(morphId, biomeHolder);

        switch (suitability) {
            case HARSH -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, false, true));
                player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("SLOWNESS", "MOVEMENT_SLOWDOWN"), EFFECT_DURATION, 0, false, false, true));
            }
            case HOSTILE -> player.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION, 0, false, false, true));
            default -> {
            }
        }
    }

    private static void applyNyctalopVision(ServerPlayer player, ResourceLocation morphId) {
        if (!EnvironmentalSusceptibilityManager.isNyctalopHostile(morphId)) {
            return;
        }

        int skyAndBlockLight = player.level().getMaxLocalRawBrightness(player.blockPosition());
        if (skyAndBlockLight > 7) {
            return;
        }

        // Reapplied well before expiration and above the vanilla fade threshold.
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NYCTALOP_NIGHT_VISION_DURATION, 0, false, false, true));
        grantAdvancement(player, ADV_NIGHT_STALKER);
    }

    private static void applyEnvironmentalEffects(ServerPlayer player, ResourceLocation morphId) {
        Vec3 position = player.position();
        
        // Check base environment
        EnvironmentalSusceptibilityManager.EnvironmentType environment = 
            EnvironmentalSusceptibilityManager.getEnvironmentType(player.level(), position, player.isInWater());

        CompoundTag data = CompatAccess.getPersistentData(player);
        String currentCondition = "";

        // Check cold vulnerability
        if (environment == EnvironmentalSusceptibilityManager.EnvironmentType.COLD) {
            var vulnerability = EnvironmentalSusceptibilityManager.checkVulnerability(morphId, environment);
            if (vulnerability == EnvironmentalSusceptibilityManager.EnvironmentalVulnerability.COLD_VULNERABLE) {
                player.addEffect(new MobEffectInstance(
                    CompatAccess.resolveMobEffect("SLOWNESS", "MOVEMENT_SLOWDOWN"),
                    EFFECT_DURATION,
                    2,
                    false,
                    false
                ));
                currentCondition = "cold";
                incrementTimer(data, ENV_COLD_TIMER, 1);
                if (CompatAccess.getInt(data, ENV_COLD_TIMER) >= 20) { // 20 * 3 seconds = 60 seconds exposure
                    grantAdvancement(player, ADV_COLD_HARDY);
                }
                return;
            }
        }

        // Check hot vulnerability
        if (environment == EnvironmentalSusceptibilityManager.EnvironmentType.HOT) {
            var vulnerability = EnvironmentalSusceptibilityManager.checkVulnerability(morphId, environment);
            if (vulnerability == EnvironmentalSusceptibilityManager.EnvironmentalVulnerability.HOT_VULNERABLE) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 1, false, false));
                currentCondition = "hot";
                incrementTimer(data, ENV_HOT_TIMER, 1);
                if (CompatAccess.getInt(data, ENV_HOT_TIMER) >= 20) { // 20 * 3 seconds = 60 seconds exposure
                    grantAdvancement(player, ADV_HEAT_RESISTANT);
                }
                return;
            }
        }

        // Check wet vulnerability
        if (environment == EnvironmentalSusceptibilityManager.EnvironmentType.WET) {
            var vulnerability = EnvironmentalSusceptibilityManager.checkVulnerability(morphId, environment);
            if (vulnerability == EnvironmentalSusceptibilityManager.EnvironmentalVulnerability.WET_VULNERABLE) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 1, false, false));
                currentCondition = "wet";
                incrementTimer(data, ENV_WET_TIMER, 1);
                return;
            }
        }

        // Check dry vulnerability (aquatic creatures away from water)
        if (EnvironmentalSusceptibilityManager.isDrySuffering(morphId, player.level(), position)) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_DURATION, 1, false, false));
            currentCondition = "dry";
            incrementTimer(data, ENV_DRY_TIMER, 1);
            if (CompatAccess.getInt(data, ENV_DRY_TIMER) >= 3) { // 3 * 3 seconds = 10 seconds away from water
                grantAdvancement(player, ADV_DEHYDRATED);
            }
            return;
        }

        // Check waterborne (aquatic creatures in water)
        if (player.isInWater()) {
            var vulnerability = EnvironmentalSusceptibilityManager.checkVulnerability(morphId, EnvironmentalSusceptibilityManager.EnvironmentType.WET);
            if (!vulnerability.equals(EnvironmentalSusceptibilityManager.EnvironmentalVulnerability.WET_VULNERABLE)) {
                // Not wet-vulnerable, check if aquatic
                if (EnvironmentalSusceptibilityManager.isDryVulnerable(morphId)) {
                    currentCondition = "water";
                    grantAdvancement(player, ADV_WATERBORNE);
                }
            }
        }

        // No vulnerability - clear timer data
        if (currentCondition.isEmpty()) {
            data.remove(ENV_COLD_TIMER);
            data.remove(ENV_HOT_TIMER);
            data.remove(ENV_WET_TIMER);
            data.remove(ENV_DRY_TIMER);
        }

        data.putString(ENV_CURRENT_CONDITION, currentCondition);
    }

    private static void incrementTimer(CompoundTag data, String key, int amount) {
        int current = CompatAccess.getInt(data, key);
        data.putInt(key, current + amount);
    }

    private static void clearEnvironmentalEffects(ServerPlayer player) {
        CompoundTag data = CompatAccess.getPersistentData(player);
        data.remove(ENV_COLD_TIMER);
        data.remove(ENV_HOT_TIMER);
        data.remove(ENV_WET_TIMER);
        data.remove(ENV_DRY_TIMER);
        data.remove(ENV_CURRENT_CONDITION);
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        AdvancementHolder root = player.getServer().getAdvancements().get(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }
}
