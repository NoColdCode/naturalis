package dev.naturalis.item;

import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 1.21.8 compatibility implementation.
 * Basic item behavior is retained while use-action APIs are migrated.
 */
public class BrewedMorphPotionItem extends Item {

    public BrewedMorphPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        Component mobName = MorphDataUtil.getMobDisplayName(stack);
        String mobIdText = MorphDataUtil.getMobId(stack);
        if (mobName != null) {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.contains", mobName));
        }
        if (mobIdText != null && !mobIdText.isEmpty()) {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.mob_id", mobIdText).withColor(0x8AA3C3));
        }
    }
}
