package dev.naturalis.client.perception;

import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.gameplay.MorphAnimalInteraction;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client prediction for animal primary-action input (block mining suppressed until Utilities tools).
 */
public final class MorphAnimalInteractionClient {

    private MorphAnimalInteractionClient() {
    }

    public static boolean shouldSuppressBlockMining(Minecraft mc) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!MorphAnimalInteraction.usesSpeciesPrimaryAction(morphId)) {
            return false;
        }
        if (isEchoTool(mc.player.getMainHandItem())) {
            return false;
        }
        return MorphLevelClientCache.getUtilitiesRank() < NaturalisConfig.knowledgeUtilitiesRankToMine();
    }

    public static boolean shouldSuppressBlockPlacement(Minecraft mc) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!MorphAnimalInteraction.usesSpeciesPrimaryAction(morphId)) {
            return false;
        }
        if (isEchoTool(mc.player.getMainHandItem())) {
            return false;
        }
        return MorphLevelClientCache.getUtilitiesRank() < NaturalisConfig.knowledgeUtilitiesRankToPlace();
    }

    private static boolean isEchoTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = stack.getItem().toString().toLowerCase();
        return path.contains("echo_morph");
    }
}
