package dev.naturalis.item;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 1.21.8 compatibility implementation.
 * Keeps tier/NBT behavior and a lightweight item type until armor APIs are migrated.
 */
public class MorphArmorItem extends Item {

    private static final String TAG_TIER = "ArmorTier";

    public MorphArmorItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static void setTier(ItemStack stack, MorphArmorTier tier) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_TIER, tier.id));
    }

    public static MorphArmorTier getTier(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_TIER)) {
            return MorphArmorTier.fromId(CompatAccess.getString(tag, TAG_TIER));
        }
        return MorphArmorTier.IRON;
    }

    public static ItemAttributeModifiers buildModifiers(MorphArmorTier tier) {
        // Attribute wiring is temporarily disabled in this compatibility class.
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        String mobId = MorphDataUtil.getMobId(stack);
        MorphArmorTier tier = getTier(stack);

        if (mobId != null) {
            tooltipAdder.accept(Component.translatable("tooltip.naturalis.morph_armor.morph", mobId));
        }
        tooltipAdder.accept(Component.translatable("tooltip.naturalis.morph_armor.tier." + tier.id));
        tooltipAdder.accept(Component.translatable("tooltip.naturalis.morph_armor.stats", tier.armor, tier.toughness));
    }
}
