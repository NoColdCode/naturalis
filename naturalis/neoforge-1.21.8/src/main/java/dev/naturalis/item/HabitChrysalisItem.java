package dev.naturalis.item;

import dev.naturalis.survivalas.SurvivalAsRuntime;
import dev.naturalis.survivalas.SurvivalAsWorldStorage;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** 1.21.8 Habit Chrysalis (InteractionResult item API). */
public class HabitChrysalisItem extends Item {

    public HabitChrysalisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!SurvivalAsWorldStorage.isEnabled() || !SurvivalAsWorldStorage.isLocked()) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.survival_as_only"), true);
            return InteractionResult.FAIL;
        }

        ResourceLocation target = MorphDataUtil.resolveMobId(stack);
        if (target == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.need_imprint"), true);
            return InteractionResult.FAIL;
        }

        if (target.equals(SurvivalAsWorldStorage.getMorphId())) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.already"), true);
            return InteractionResult.FAIL;
        }

        if (!SurvivalAsRuntime.changeIdentity(serverPlayer, target)) {
            return InteractionResult.FAIL;
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(
        ItemStack stack,
        Player player,
        LivingEntity target,
        InteractionHand hand
    ) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (MorphDataUtil.resolveMobId(stack) != null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.already_imprinted"), true);
            return InteractionResult.FAIL;
        }
        if (target instanceof Player) {
            return InteractionResult.FAIL;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id == null) {
            return InteractionResult.FAIL;
        }

        MorphDataUtil.setMobId(stack, id.toString());
        serverPlayer.displayClientMessage(
            Component.translatable("message.naturalis.habit_chrysalis.imprinted", target.getType().getDescription()),
            true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        tooltipAdder.accept(Component.translatable("tooltip.naturalis.habit_chrysalis.lore"));
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.contains", mobName));
        } else {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.habit_chrysalis.empty"));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.mob_id", mobIdText).withColor(0x8AA3C3));
        }
        tooltipAdder.accept(Component.translatable("tooltip.naturalis.habit_chrysalis.imprint_hint"));
    }
}
