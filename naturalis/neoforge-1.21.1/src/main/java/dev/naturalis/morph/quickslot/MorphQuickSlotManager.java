package dev.naturalis.morph.quickslot;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.network.MorphQuickSlotPayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import tocraft.remorphed.impl.PlayerMorph;
import tocraft.walkers.api.PlayerAbilities;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.api.PlayerShapeChanger;
import tocraft.walkers.api.variant.ShapeType;
import tocraft.walkers.impl.PlayerDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class MorphQuickSlotManager {

    private static final String ROOT_TAG = "naturalis_knowledge";
    private static final String QUICK_SLOTS_TAG = "quick_slots";
    private static final String MANUAL_SLOTS_TAG = "manual";
    private static final String USAGE_TICKS_TAG = "usage_ticks";

    static {
        MorphQuickSlotBridge.register(
            MorphQuickSlotManager::syncToClient,
            MorphQuickSlotManager::recordUsageTick,
            new MorphQuickSlotBridge.MorphQuickSlotActions() {
                @Override
                public void handleSelect(ServerPlayer player, int slotIndex, ResourceLocation morphId) {
                    MorphQuickSlotManager.handleSelection(player, slotIndex, morphId);
                }

                @Override
                public void handleAssign(ServerPlayer player, int slotIndex, ResourceLocation morphId) {
                    MorphQuickSlotManager.assignFromGui(player, slotIndex, morphId);
                }
            }
        );
    }

    private MorphQuickSlotManager() {
    }

    public static int getUnlockedSlotCount(ServerPlayer player) {
        return getUnlockedSlotCount(MorphKnowledgeManager.getEffectiveGlobalXp(player));
    }

    public static int getUnlockedSlotCount(int globalXp) {
        int count = 0;
        for (int threshold : MorphQuickSlotCategory.UNLOCK_XP) {
            if (globalXp >= threshold) {
                count++;
            }
        }
        return count;
    }

    public static void recordUsageTick(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null) {
            return;
        }
        CompoundTag usage = getUsageTicks(player);
        String key = morphId.toString();
        usage.putInt(key, CompatAccess.getInt(usage, key) + 1);
    }

    public static int getUsageTicks(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null) {
            return 0;
        }
        return CompatAccess.getInt(getUsageTicks(player), morphId.toString());
    }

    @Nullable
    public static ResourceLocation getManualSlot(ServerPlayer player, int slotIndex) {
        ListTag manual = getManualSlots(player);
        if (slotIndex < 0 || slotIndex >= manual.size()) {
            return null;
        }
        String raw = manual.getString(slotIndex);
        return parseMorphId(raw == null || raw.isBlank() ? null : raw);
    }

    public static void setManualSlot(ServerPlayer player, int slotIndex, @Nullable ResourceLocation morphId) {
        if (slotIndex < 0 || slotIndex >= MorphQuickSlotCategory.SLOT_COUNT) {
            return;
        }
        ListTag manual = getManualSlots(player);
        while (manual.size() <= slotIndex) {
            manual.add(StringTag.valueOf(""));
        }
        manual.set(slotIndex, StringTag.valueOf(morphId == null ? "" : morphId.toString()));
        syncToClient(player);
    }

    @Nullable
    public static ResourceLocation getResolvedSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MorphQuickSlotCategory.SLOT_COUNT) {
            return null;
        }
        if (slotIndex >= getUnlockedSlotCount(player)) {
            return null;
        }

        ResourceLocation manual = getManualSlot(player, slotIndex);
        if (manual != null && isMorphAvailable(player, manual)) {
            return manual;
        }

        MorphQuickSlotCategory category = MorphQuickSlotCategory.byIndex(slotIndex);
        if (category == null) {
            return null;
        }
        return findBestDefaultForCategory(player, category);
    }

    public static List<ResourceLocation> getResolvedSlots(ServerPlayer player) {
        List<ResourceLocation> slots = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            slots.add(getResolvedSlot(player, i));
        }
        return slots;
    }

    public static void handleSelection(ServerPlayer player, int slotIndex, @Nullable ResourceLocation clientMorphId) {
        ResourceLocation target = clientMorphId;
        if (target == null && slotIndex >= 0 && slotIndex < getUnlockedSlotCount(player)) {
            target = getResolvedSlot(player, slotIndex);
        }

        if (target != null) {
            MorphQuickSlotMorphUtil.applyMorph(player, target);
            return;
        }

        if (slotIndex == -1) {
            toggleDefault(player);
        }
    }

    public static void assignFromGui(ServerPlayer player, int slotIndex, ResourceLocation morphId) {
        if (slotIndex < 0 || slotIndex >= MorphQuickSlotCategory.SLOT_COUNT) {
            return;
        }
        if (slotIndex >= getUnlockedSlotCount(player)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("gui.naturalis.quick_slot.assign_locked",
                    MorphQuickSlotCategory.byIndex(slotIndex).label(),
                    MorphQuickSlotCategory.UNLOCK_XP[slotIndex]),
                true
            );
            return;
        }
        if (!isMorphAvailable(player, morphId)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.naturalis.quick_slot.assign_unavailable"),
                true
            );
            return;
        }
        setManualSlot(player, slotIndex, morphId);
    }

    public static void syncToClient(ServerPlayer player) {
        int globalXp = MorphKnowledgeManager.getEffectiveGlobalXp(player);
        PlayToClientSender.send(player, new MorphQuickSlotPayload(
            getUnlockedSlotCount(globalXp),
            globalXp,
            getResolvedSlots(player)
        ));
    }

    private static void toggleDefault(ServerPlayer player) {
        LivingEntity current = PlayerShape.getCurrentShape(player);
        if (current != null) {
            unmorph(player);
            return;
        }

        PlayerDataProvider provider = (PlayerDataProvider) player;
        ShapeType<?> secondShape = provider.walkers$get2ndShape();
        if (secondShape == null) {
            return;
        }

        LivingEntity created = secondShape.create(player.level(), player);
        if (created == null) {
            return;
        }

        if (PlayerShape.updateShapes(player, created)) {
            PlayerShape.sync(player);
            player.refreshDimensions();
        }
    }

    private static void unmorph(ServerPlayer player) {
        if (PlayerShape.getCurrentShape(player) == null) {
            return;
        }
        try {
            if (!PlayerShape.updateShapes(player, null)) {
                PlayerDataProvider provider = (PlayerDataProvider) player;
                provider.walkers$updateShapes(null);
            }
            player.refreshDimensions();
            PlayerShape.sync(player);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static ResourceLocation findBestDefaultForCategory(ServerPlayer player, MorphQuickSlotCategory category) {
        ResourceLocation best = null;
        int bestUsage = -1;

        for (ResourceLocation morphId : listAvailableMorphs(player)) {
            if (!MorphQuickSlotClassifier.matches(morphId, category)) {
                continue;
            }
            int usage = getUsageTicks(player, morphId);
            if (usage > bestUsage) {
                bestUsage = usage;
                best = morphId;
            }
        }

        if (best != null) {
            return best;
        }

        for (ResourceLocation morphId : listAvailableMorphs(player)) {
            if (MorphQuickSlotClassifier.primaryCategory(morphId) == category) {
                return morphId;
            }
        }

        return null;
    }

    private static List<ResourceLocation> listAvailableMorphs(ServerPlayer player) {
        List<ResourceLocation> morphs = new ArrayList<>();
        Map<ShapeType<? extends LivingEntity>, Integer> unlocked = PlayerMorph.getUnlockedShapes(player);
        for (ShapeType<? extends LivingEntity> shape : unlocked.keySet()) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(shape.getEntityType());
            if (id != null) {
                morphs.add(id);
            }
        }

        for (String key : getXpMorphKeys(player)) {
            ResourceLocation parsed = ResourceLocation.tryParse(key);
            if (parsed != null && !morphs.contains(parsed)) {
                morphs.add(parsed);
            }
        }

        return morphs;
    }

    private static Iterable<String> getXpMorphKeys(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            return List.of();
        }
        CompoundTag knowledge = CompatAccess.getCompound(root, ROOT_TAG);
        if (!knowledge.contains("xp_map")) {
            return List.of();
        }
        return getTagKeys(CompatAccess.getCompound(knowledge, "xp_map"));
    }

    @SuppressWarnings("unchecked")
    private static Iterable<String> getTagKeys(CompoundTag tag) {
        try {
            Object raw = tag.getClass().getMethod("getAllKeys").invoke(tag);
            if (raw instanceof Iterable<?>) {
                return (Iterable<String>) raw;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = tag.getClass().getMethod("keySet").invoke(tag);
            if (raw instanceof Iterable<?>) {
                return (Iterable<String>) raw;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return List.of();
    }

    private static boolean isMorphAvailable(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
            return false;
        }
        Map<ShapeType<? extends LivingEntity>, Integer> unlocked = PlayerMorph.getUnlockedShapes(player);
        for (ShapeType<? extends LivingEntity> shape : unlocked.keySet()) {
            if (morphId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(shape.getEntityType()))) {
                return true;
            }
        }
        return MorphKnowledgeManager.getXp(player, morphId) > 0;
    }

    private static CompoundTag getUsageTicks(ServerPlayer player) {
        CompoundTag cache = MorphKnowledgeManager.getStatCache(player);
        if (!cache.contains(USAGE_TICKS_TAG)) {
            cache.put(USAGE_TICKS_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(cache, USAGE_TICKS_TAG);
    }

    private static ListTag getManualSlots(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag knowledge = CompatAccess.getCompound(root, ROOT_TAG);
        if (!knowledge.contains(QUICK_SLOTS_TAG)) {
            knowledge.put(QUICK_SLOTS_TAG, new CompoundTag());
        }
        CompoundTag quickSlots = CompatAccess.getCompound(knowledge, QUICK_SLOTS_TAG);
        if (!quickSlots.contains(MANUAL_SLOTS_TAG)) {
            quickSlots.put(MANUAL_SLOTS_TAG, new ListTag());
        }
        return quickSlots.getList(MANUAL_SLOTS_TAG, Tag.TAG_STRING);
    }

    @Nullable
    private static ResourceLocation parseMorphId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }
}
