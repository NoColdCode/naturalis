package dev.naturalis.gameplay;

import dev.naturalis.NaturalisMod;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.network.MorphLevelPayload;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import dev.naturalis.network.PlayToClientSender;

/**
 * Loader-neutral knowledge health modifier + morph-level sync.
 */
public final class KnowledgeLevelLogic {

    private static final ResourceLocation KNOWLEDGE_HEALTH_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "knowledge_health_bonus");

    private KnowledgeLevelLogic() {
    }

    public static void tick(ServerPlayer player) {
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
                PlayToClientSender.send(player, new MorphLevelPayload(0, 3, false));
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
            PlayToClientSender.send(player, new MorphLevelPayload(morphLevel, slots, inventoryUnlocked, utilitiesRank));
            MorphQuickSlotBridge.sync(player);
        }
    }

    private static void applyHealthModifier(ServerPlayer player, ResourceLocation morphId, double previousMax, double previousHealth, boolean wasFull) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }

        health.removeModifier(KNOWLEDGE_HEALTH_MODIFIER_ID);

        double bonusPercent = MorphKnowledgeManager.getHealthBonusPercent(player, morphId);
        double flatBonus = Math.floor(health.getBaseValue() * bonusPercent);
        if (flatBonus > 0.0D) {
            health.addTransientModifier(new AttributeModifier(
            KNOWLEDGE_HEALTH_MODIFIER_ID, flatBonus, AttributeModifier.Operation.ADD_VALUE));
        }

        rescaleCurrentHealth(player, previousMax, previousHealth, wasFull);
    }

    private static void removeHealthModifier(ServerPlayer player, double previousMax, double previousHealth, boolean wasFull) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(KNOWLEDGE_HEALTH_MODIFIER_ID);
            rescaleCurrentHealth(player, previousMax, previousHealth, wasFull);
        }
    }

    private static void rescaleCurrentHealth(ServerPlayer player, double previousMax, double previousHealth, boolean wasFull) {
        // Guard against accidental revive while the death screen is active.
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
}
