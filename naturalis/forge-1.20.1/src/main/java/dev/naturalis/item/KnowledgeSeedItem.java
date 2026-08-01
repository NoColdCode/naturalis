package dev.naturalis.item;

import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Forge 1.20.1 — fixes appendHoverText signature.
 */
public class KnowledgeSeedItem extends Item {

    public KnowledgeSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(serverPlayer);
        if (currentMorph == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.knowledge_seed.no_morph"), true);
            return InteractionResultHolder.fail(stack);
        }

        int currentLevel = MorphKnowledgeManager.getLevel(serverPlayer, currentMorph);
        int maxLevel = MorphKnowledgeManager.getMaxLevel();
        if (currentLevel >= maxLevel) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.knowledge_seed.capped"), true);
            return InteractionResultHolder.fail(stack);
        }

        int newLevel = currentLevel + 1;
        MorphKnowledgeManager.setLevel(serverPlayer, currentMorph, newLevel);

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        serverPlayer.displayClientMessage(
            Component.translatable("message.naturalis.knowledge_seed.applied",
                currentMorph.toString(), newLevel), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.naturalis.knowledge_seed.effect"));
    }
}
