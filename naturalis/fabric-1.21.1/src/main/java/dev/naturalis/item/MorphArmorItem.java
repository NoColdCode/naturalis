package dev.naturalis.item;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Fabric 1.21.1 variant: plain {@link Item} with tier NBT/tooltips (no NeoForge armor hooks).
 */
public class MorphArmorItem extends Item {

    private static final String TAG_TIER = "ArmorTier";

    public MorphArmorItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static void setTier(ItemStack stack, MorphArmorTier tier) {
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_TIER, tier.id));
    }

    public static MorphArmorTier getTier(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
            stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_TIER)) {
            return MorphArmorTier.fromId(CompatAccess.getString(tag, TAG_TIER));
        }
        return MorphArmorTier.IRON;
    }

    public static net.minecraft.world.item.component.ItemAttributeModifiers buildModifiers(MorphArmorTier tier) {
        return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String mobId = MorphDataUtil.getMobId(stack);
        MorphArmorTier tier = getTier(stack);

        if (mobId != null) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.morph_armor.morph", mobId));
        }
        tooltipComponents.add(Component.translatable("tooltip.naturalis.morph_armor.tier." + tier.id));
        tooltipComponents.add(Component.translatable(
            "tooltip.naturalis.morph_armor.stats", tier.armor, tier.toughness));
    }
}
