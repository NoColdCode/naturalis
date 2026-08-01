package dev.naturalis.item;

import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Forge 1.20.1 — uses plain MobEffect (not Holder<MobEffect>) and the 1.20.1 item APIs.
 */
public class MorphOnlyToolItem extends Item {

    private final MobEffect grantedEffect;
    private final int durationTicks;
    private final int cooldownTicks;

    public MorphOnlyToolItem(Properties properties, MobEffect grantedEffect, int durationTicks, int cooldownTicks) {
        super(properties);
        this.grantedEffect = grantedEffect;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        if (CurrentMorphUtil.getCurrentMorphId(serverPlayer) == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.morph_tool.require_morph")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.addEffect(new MobEffectInstance(grantedEffect, durationTicks, 0));
        // 1.20.1: addCooldown(Item item, int ticks)
        serverPlayer.getCooldowns().addCooldown(this, cooldownTicks);
        // 1.20.1: hurtAndBreak(int, LivingEntity, Consumer<LivingEntity>)
        stack.hurtAndBreak(1, serverPlayer, p ->
            p.broadcastBreakEvent(usedHand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.naturalis.morph_tool.only")
            .withStyle(ChatFormatting.GRAY));
    }
}
