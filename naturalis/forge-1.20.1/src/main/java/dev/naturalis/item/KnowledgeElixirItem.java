package dev.naturalis.item;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.nbt.CompoundTag;
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
 * Forge 1.20.1 — uses plain NBT instead of 1.21 DataComponents.
 */
public class KnowledgeElixirItem extends Item {

    private static final int DEFAULT_DURATION_TICKS = 20 * 180;
    private static final int MIN_DURATION_TICKS = 20;

    public KnowledgeElixirItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        ResourceLocation targetMorph = resolveTargetMorph(serverPlayer, stack);
        if (targetMorph == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.knowledge_elixir.no_morph"), true);
            return InteractionResultHolder.fail(stack);
        }

        int durationTicks = readDurationTicks(stack);
        if (!MorphKnowledgeManager.applyTemporaryFullUnlock(serverPlayer, targetMorph, durationTicks)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        int seconds = durationTicks / 20;
        serverPlayer.displayClientMessage(
            Component.translatable("message.naturalis.knowledge_elixir.applied",
                targetMorph.toString(), seconds), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltip.add(Component.translatable("tooltip.naturalis.contains", mobName));
        } else {
            tooltip.add(Component.translatable("tooltip.naturalis.knowledge_elixir.target_current"));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.naturalis.mob_id", mobIdText)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.naturalis.knowledge_elixir.duration",
            readDurationTicks(stack) / 20));
    }

    private static ResourceLocation resolveTargetMorph(ServerPlayer player, ItemStack stack) {
        ResourceLocation fromStack = MorphDataUtil.resolveMobId(stack);
        if (fromStack != null) {
            return fromStack;
        }
        return CurrentMorphUtil.getCurrentMorphId(player);
    }

    private static int readDurationTicks(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return DEFAULT_DURATION_TICKS;
        if (tag.contains("KnowledgeBurstDuration")) {
            return Math.max(MIN_DURATION_TICKS, CompatAccess.getInt(tag, "KnowledgeBurstDuration"));
        }
        if (tag.contains("EffectDuration")) {
            return Math.max(MIN_DURATION_TICKS, CompatAccess.getInt(tag, "EffectDuration"));
        }
        return DEFAULT_DURATION_TICKS;
    }
}
