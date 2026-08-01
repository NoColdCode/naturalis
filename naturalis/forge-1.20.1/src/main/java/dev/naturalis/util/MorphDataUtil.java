package dev.naturalis.util;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Forge 1.20.1 version — uses NBT (getOrCreateTag) instead of the 1.21 DataComponents API.
 */
public final class MorphDataUtil {

    private static final String TAG_MOB_ID       = "MobId";
    private static final String TAG_MOB_ID_LEGACY = "mob_id";
    private static final String TAG_MOB_ID_ALT   = "MobID";
    private static final String TAG_ENTITY_ID_ALT = "EntityId";
    private static final String TAG_SHAPE_DATA = "ShapeData";

    private MorphDataUtil() {
    }

    public static void setMobId(ItemStack stack, String mobId) {
        if (mobId == null || mobId.isBlank()) {
            return;
        }
        String normalized = normalizeMobIdText(mobId);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_MOB_ID, normalized);
        tag.putString(TAG_MOB_ID_LEGACY, normalized);
        tag.putString(TAG_MOB_ID_ALT, normalized);
        tag.putString(TAG_ENTITY_ID_ALT, normalized);
    }

    public static String getMobId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        String rawMobId = readFirstMobId(tag);
        if (rawMobId == null || rawMobId.isEmpty()) {
            return null;
        }
        return normalizeMobIdText(rawMobId);
    }

    public static ResourceLocation resolveMobId(ItemStack stack) {
        return parseMobId(getMobId(stack));
    }

    public static void setShapeData(ItemStack stack, CompoundTag shapeData) {
        if (shapeData == null || shapeData.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().put(TAG_SHAPE_DATA, shapeData.copy());
    }

    public static CompoundTag getShapeData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return new CompoundTag();
        }
        return CompatAccess.getCompound(tag, TAG_SHAPE_DATA);
    }

    public static void copyShapeData(ItemStack from, ItemStack to) {
        CompoundTag shapeData = getShapeData(from);
        if (!shapeData.isEmpty()) {
            setShapeData(to, shapeData);
        }
    }

    public static Component getMobDisplayName(ItemStack stack) {
        String mobId = getMobId(stack);
        if (mobId == null || mobId.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(mobId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return Component.literal(mobId);
        }
        var type = CompatAccess.getEntityType(id);
        return type != null ? type.getDescription() : Component.literal(mobId);
    }

    private static String readFirstMobId(CompoundTag tag) {
        if (tag.contains(TAG_MOB_ID)) {
            return CompatAccess.getString(tag, TAG_MOB_ID);
        }
        if (tag.contains(TAG_MOB_ID_LEGACY)) {
            return CompatAccess.getString(tag, TAG_MOB_ID_LEGACY);
        }
        if (tag.contains(TAG_MOB_ID_ALT)) {
            return CompatAccess.getString(tag, TAG_MOB_ID_ALT);
        }
        if (tag.contains(TAG_ENTITY_ID_ALT)) {
            return CompatAccess.getString(tag, TAG_ENTITY_ID_ALT);
        }
        return null;
    }

    private static String normalizeMobIdText(String mobIdText) {
        return mobIdText.trim().toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation parseMobId(String mobIdText) {
        if (mobIdText == null || mobIdText.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(mobIdText);
        if (id != null) {
            return id;
        }
        return ResourceLocation.tryParse("minecraft:" + mobIdText);
    }
}
