package dev.naturalis.item;

import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class MorphOnlyToolItem extends Item {

    private final Holder<MobEffect> grantedEffect;
    private final int durationTicks;
    private final int cooldownTicks;

    public MorphOnlyToolItem(Properties properties, Holder<MobEffect> grantedEffect, int durationTicks, int cooldownTicks) {
        super(properties);
        this.grantedEffect = grantedEffect;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (CurrentMorphUtil.getCurrentMorphId(serverPlayer) == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.naturalis.morph_tool.require_morph").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        serverPlayer.addEffect(new MobEffectInstance(grantedEffect, durationTicks, 0));
        serverPlayer.getCooldowns().addCooldown(stack, cooldownTicks);
        stack.hurtAndBreak(1, serverPlayer, usedHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        tooltipAdder.accept(Component.translatable("tooltip.naturalis.morph_tool.only").withStyle(ChatFormatting.GRAY));
    }
}
