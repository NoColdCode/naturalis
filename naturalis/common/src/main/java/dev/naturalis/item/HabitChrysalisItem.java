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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Rare Survival-as relic: imprint a living creature, then shed into that form.
 * Changing form resets diet / circadian habits — you must relearn how to live.
 */
public class HabitChrysalisItem extends Item {

    public HabitChrysalisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!SurvivalAsWorldStorage.isEnabled() || !SurvivalAsWorldStorage.isLocked()) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.survival_as_only"), true);
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation target = MorphDataUtil.resolveMobId(stack);
        if (target == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.need_imprint"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (target.equals(SurvivalAsWorldStorage.getMorphId())) {
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.already"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!SurvivalAsRuntime.changeIdentity(serverPlayer, target)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.naturalis.habit_chrysalis.lore"));
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.contains", mobName));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.habit_chrysalis.empty"));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.mob_id", mobIdText).withColor(0x8AA3C3));
        }
        tooltipComponents.add(Component.translatable("tooltip.naturalis.habit_chrysalis.imprint_hint"));
    }
}
