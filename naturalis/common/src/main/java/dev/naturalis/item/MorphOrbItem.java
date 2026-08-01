package dev.naturalis.item;

import dev.naturalis.util.MorphAcquisition;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MorphOrbItem extends Item {

    public MorphOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobIdText == null || mobIdText.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation mobId = MorphDataUtil.resolveMobId(stack);
            if (mobId == null) {
                serverPlayer.displayClientMessage(Component.translatable("command.naturalis.invalid_id", mobIdText), true);
                return InteractionResultHolder.fail(stack);
            }

            boolean acquired = MorphAcquisition.acquire(serverPlayer, stack);
            if (acquired) {
                if (!serverPlayer.isCreative()) {
                    stack.shrink(1);
                }
                serverPlayer.displayClientMessage(MorphAcquisition.formatAcquireSuccess(mobId), true);
                return InteractionResultHolder.success(stack);
            }

            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.contains", mobName));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.mob_id", mobIdText).withColor(0x8AA3C3));
        }
    }
}
