package dev.naturalis.item;

import dev.naturalis.util.MorphDataUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single-slot armor item representing a full morph armor set.
 * Extends {@link ArmorItem} with {@link ArmorItem.Type#CHESTPLATE} so that
 * the vanilla {@code HumanoidArmorLayer} applies it to the player model
 * (including when Woodwalkers changes the visual form).
 * The actual protection stats come from the {@link MorphArmorTier}, not the material.
 */
public class MorphArmorItem extends ArmorItem {

    private static final String TAG_TIER = "ArmorTier";

    public MorphArmorItem(Properties props) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, props.stacksTo(1));
    }

    // ── Armor texture (per-tier) ─────────────────────────────────────────────

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                             ArmorMaterial.Layer layer, boolean innerModel) {
        MorphArmorTier tier = getTier(stack);
        // layer_1 = outer (head/chest/feet), layer_2 = inner (legs) — we use only layer_1
        return ResourceLocation.fromNamespaceAndPath("naturalis",
                "textures/models/armor/morph_armor_" + tier.id + "_layer_1.png");
    }

    // ── Tier NBT helpers ────────────────────────────────────────────────────

    public static void setTier(ItemStack stack, MorphArmorTier tier) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_TIER, tier.id));
    }

    public static MorphArmorTier getTier(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_TIER)) {
            return MorphArmorTier.fromId(tag.getString(TAG_TIER));
        }
        return MorphArmorTier.IRON;
    }

    // ── Attribute modifiers ─────────────────────────────────────────────────

    /**
     * Build an ItemAttributeModifiers for a given tier.
     * Called when crafting is complete (stored as DataComponent on the stack)
     * and used as fallback for items created without the component (e.g. creative).
     */
    public static ItemAttributeModifiers buildModifiers(MorphArmorTier tier) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // All stats packed into one chestplate slot – represents the full armor set.
        builder.add(
            Attributes.ARMOR,
            new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("naturalis", "morph_armor_" + tier.id),
                tier.armor,
                AttributeModifier.Operation.ADD_VALUE
            ),
            EquipmentSlotGroup.CHEST
        );

        if (tier.toughness > 0) {
            builder.add(
                Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("naturalis", "morph_toughness_" + tier.id),
                    tier.toughness,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.CHEST
            );
        }

        if (tier.knockbackResistanceTenths > 0) {
            builder.add(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("naturalis", "morph_kb_" + tier.id),
                    tier.knockbackResistanceTenths * 0.1,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.CHEST
            );
        }

        return builder.build();
    }

    /**
     * NeoForge per-stack attribute override.
     * Reads tier from NBT so that creative-spawned stacks also have correct stats.
     */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers stored = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (stored != null) {
            return stored;
        }
        return buildModifiers(getTier(stack));
    }

    // ── Tooltip ─────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String mobId = MorphDataUtil.getMobId(stack);
        MorphArmorTier tier = getTier(stack);

        if (mobId != null) {
            tooltipComponents.add(Component.translatable("tooltip.naturalis.morph_armor.morph", mobId));
        }
        tooltipComponents.add(Component.translatable(
            "tooltip.naturalis.morph_armor.tier." + tier.id));
        tooltipComponents.add(Component.translatable(
            "tooltip.naturalis.morph_armor.stats", tier.armor, tier.toughness));
    }
}
