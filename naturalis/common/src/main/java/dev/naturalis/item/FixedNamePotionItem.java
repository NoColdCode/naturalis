package dev.naturalis.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

public class FixedNamePotionItem extends PotionItem {

    private final String translationKey;

    public FixedNamePotionItem(Item.Properties properties, String translationKey) {
        super(properties);
        this.translationKey = translationKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(translationKey);
    }
}
