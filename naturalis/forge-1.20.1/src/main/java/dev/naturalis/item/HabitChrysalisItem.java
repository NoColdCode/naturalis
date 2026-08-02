package dev.naturalis.item;

import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Forge 1.20.1: item exists for registry parity; Survival-as imprint/shed is 1.21+ only.
 */
public class HabitChrysalisItem extends Item {

    public HabitChrysalisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            player.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.survival_as_only"), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(
        ItemStack stack,
        Player player,
        LivingEntity target,
        InteractionHand hand
    ) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(
                Component.translatable("message.naturalis.habit_chrysalis.survival_as_only"), true);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.naturalis.habit_chrysalis.lore"));
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        if (mobName != null) {
            tooltip.add(Component.translatable("tooltip.naturalis.contains", mobName));
        } else {
            tooltip.add(Component.translatable("tooltip.naturalis.habit_chrysalis.empty"));
        }
        tooltip.add(Component.translatable("message.naturalis.habit_chrysalis.survival_as_only"));
    }
}
