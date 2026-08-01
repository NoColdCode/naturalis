package dev.naturalis.item;

import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FilledEchoVialItem extends Item {

    public FilledEchoVialItem(Properties properties) {
        super(properties);
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