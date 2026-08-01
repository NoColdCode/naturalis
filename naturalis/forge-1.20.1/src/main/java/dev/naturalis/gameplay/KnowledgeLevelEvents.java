package dev.naturalis.gameplay;

import dev.naturalis.Naturalis;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.network.MorphLevelPayload;
import dev.naturalis.network.NaturalisNetwork;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Applies per-level knowledge perks to morphed players:
 *  – MAX_HEALTH bonus via attribute modifier
 *  – Syncs the current morph level to the client every 20 ticks
 */
@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class KnowledgeLevelEvents {

    private static final ResourceLocation KNOWLEDGE_HEALTH_MODIFIER_ID =
        new ResourceLocation(Naturalis.MOD_ID, "knowledge_health_bonus");

    private KnowledgeLevelEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Never mutate health attributes during death/respawn transition.
        if (player.isDeadOrDying()) {
            return;
        }

        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        double previousMax = healthAttribute != null ? healthAttribute.getValue() : player.getMaxHealth();
        double previousHealth = player.getHealth();
        boolean wasFull = previousHealth >= previousMax - 0.01D;

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);

        if (morphId == null) {
            removeHealthModifier(player, previousMax, previousHealth, wasFull);
            if (player.tickCount % 20 == 0) {
                int globalXp = MorphKnowledgeManager.getEffectiveGlobalXp(player);
                NaturalisNetwork.sendToPlayer(player, new MorphLevelPayload(0, 3, false, 0, globalXp));
                MorphQuickSlotBridge.sync(player);
            }
            return;
        }

        applyHealthModifier(player, morphId, previousMax, previousHealth, wasFull);

        int morphLevel = MorphKnowledgeManager.getLevel(player, morphId);

        if (player.tickCount % 20 == 0) {
            int slots = MorphKnowledgeManager.getAllowedHotbarSlots(player, morphId);
            boolean inventoryUnlocked = MorphKnowledgeManager.canOpenInventory(player, morphId);
            int utilitiesRank = MorphKnowledgeManager.getUtilitiesRank(player, morphId);
            int globalXp = MorphKnowledgeManager.getEffectiveGlobalXp(player);
            NaturalisNetwork.sendToPlayer(player, new MorphLevelPayload(morphLevel, slots, inventoryUnlocked, utilitiesRank, globalXp));
            MorphQuickSlotBridge.sync(player);
        }
    }

    private static void applyHealthModifier(ServerPlayer player, ResourceLocation morphId,
                                            double previousMax, double previousHealth, boolean wasFull) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }

        UUID uuid = toUUID(KNOWLEDGE_HEALTH_MODIFIER_ID);
        health.removeModifier(uuid);

        double bonusPercent = MorphKnowledgeManager.getHealthBonusPercent(player, morphId);
        double flatBonus = Math.floor(health.getBaseValue() * bonusPercent);
        if (flatBonus > 0.0D) {
            health.addTransientModifier(new AttributeModifier(
                uuid, KNOWLEDGE_HEALTH_MODIFIER_ID.toString(), flatBonus,
                AttributeModifier.Operation.ADDITION));
        }

        rescaleCurrentHealth(player, previousMax, previousHealth, wasFull);
    }

    private static void removeHealthModifier(ServerPlayer player, double previousMax,
                                             double previousHealth, boolean wasFull) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(toUUID(KNOWLEDGE_HEALTH_MODIFIER_ID));
            rescaleCurrentHealth(player, previousMax, previousHealth, wasFull);
        }
    }

    private static void rescaleCurrentHealth(ServerPlayer player, double previousMax,
                                             double previousHealth, boolean wasFull) {
        if (player.isDeadOrDying() || previousHealth <= 0.0D) {
            return;
        }

        double newMax = player.getMaxHealth();
        if (newMax <= 0.0D || previousMax <= 0.0D) {
            return;
        }

        if (wasFull) {
            player.setHealth((float) newMax);
            return;
        }

        double ratio = Math.max(0.0D, Math.min(1.0D, previousHealth / previousMax));
        double newHealth = Math.max(0.0D, Math.min(newMax, newMax * ratio));
        player.setHealth((float) newHealth);
    }

    private static UUID toUUID(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }
}
