package dev.naturalis.gameplay;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;

/**
 * Animal morphs use species-appropriate primary actions until Utilities knowledge unlocks human tool use.
 */
public final class MorphAnimalInteraction {

    private static final Set<String> HUMANOID_TOOL_USERS = Set.of(
        "player", "villager", "zombie", "skeleton", "drowned", "husk", "stray",
        "wither_skeleton", "bogged", "pillager", "vindicator", "evoker", "witch", "illusioner",
        "piglin", "piglin_brute", "zombified_piglin", "enderman"
    );

    private MorphAnimalInteraction() {
    }

    public static boolean usesSpeciesPrimaryAction(ResourceLocation morphId) {
        return morphId != null && !isHumanoidMorph(morphId) && InventoryRestrictionManager.isQuadruped(morphId);
    }

    public static boolean canMineBlocksAsMorph(ServerPlayer player, ResourceLocation morphId, boolean echoTool) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return true;
        }
        if (morphId == null || echoTool || isHumanoidMorph(morphId)) {
            return true;
        }
        if (!usesSpeciesPrimaryAction(morphId)) {
            return true;
        }
        return MorphKnowledgeManager.canUseToolsAsMorph(player, morphId);
    }

    public static boolean canPlaceBlocksAsMorph(ServerPlayer player, ResourceLocation morphId, boolean echoTool) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return true;
        }
        if (morphId == null || echoTool || isHumanoidMorph(morphId)) {
            return true;
        }
        if (!usesSpeciesPrimaryAction(morphId)) {
            return true;
        }
        return MorphKnowledgeManager.canPlaceBlocksAsMorph(player, morphId);
    }

    public static boolean isHumanoidMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        if (HUMANOID_TOOL_USERS.contains(path)) {
            return true;
        }
        return path.contains("villager")
            || path.contains("zombie")
            || path.contains("skeleton")
            || path.contains("piglin")
            || path.contains("illager");
    }

    public static boolean isCanineCarrierMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        return "wolf".equals(path) || "fox".equals(path);
    }

    /** Morphs that can pick up loose items with the mouth (right click). */
    public static boolean usesMouthSecondary(ResourceLocation morphId) {
        if (morphId == null || isHumanoidMorph(morphId)) {
            return false;
        }
        if (isCanineCarrierMorph(morphId)) {
            return true;
        }
        return usesSpeciesPrimaryAction(morphId) || InventoryRestrictionManager.isQuadruped(morphId);
    }
}
