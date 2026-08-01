package dev.naturalis.item;

import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MorphOnlyToolItem extends Item {

    private final Holder<net.minecraft.world.effect.MobEffect> grantedEffect;
    private final int durationTicks;
    private final int cooldownTicks;

    public MorphOnlyToolItem(Properties properties, Holder<net.minecraft.world.effect.MobEffect> grantedEffect, int durationTicks, int cooldownTicks) {
        super(properties);
        this.grantedEffect = grantedEffect;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (CurrentMorphUtil.getCurrentMorphId(serverPlayer) == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.naturalis.morph_tool.require_morph").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.addEffect(new MobEffectInstance(grantedEffect, durationTicks, 0));
        serverPlayer.getCooldowns().addCooldown(this, cooldownTicks);
        stack.hurtAndBreak(1, serverPlayer, usedHand == InteractionHand.MAIN_HAND
            ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
            : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.naturalis.morph_tool.only").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
