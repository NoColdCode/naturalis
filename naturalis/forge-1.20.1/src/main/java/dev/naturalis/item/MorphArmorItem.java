package dev.naturalis.item;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Forge 1.20.1 — uses plain NBT (getOrCreateTag) instead of the 1.21 DataComponents API.
 * Attribute wiring is handled via normal armor attribute mechanics in ItemArmorTier; the
 * per-item ATTRIBUTE_MODIFIERS component is not available here.
 */
public class MorphArmorItem extends Item {

    private static final String TAG_TIER = "ArmorTier";

    public MorphArmorItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static void setTier(ItemStack stack, MorphArmorTier tier) {
        stack.getOrCreateTag().putString(TAG_TIER, tier.id);
    }

    public static MorphArmorTier getTier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_TIER)) {
            return MorphArmorTier.fromId(CompatAccess.getString(tag, TAG_TIER));
        }
        return MorphArmorTier.IRON;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String mobId = MorphDataUtil.getMobId(stack);
        MorphArmorTier tier = getTier(stack);

        if (mobId != null) {
            tooltip.add(Component.translatable("tooltip.naturalis.morph_armor.morph", mobId));
        }
        tooltip.add(Component.translatable("tooltip.naturalis.morph_armor.tier." + tier.id));
        tooltip.add(Component.translatable("tooltip.naturalis.morph_armor.stats", tier.armor, tier.toughness));
    }
}
