package dev.naturalis.inventory;

import dev.naturalis.NaturalisMod;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class InventoryRestrictionEvents {

    private static final String ROOT_TAG = "naturalis_inventory_restriction";
    private static final String LAST_SELECTED_SLOT = "last_selected_slot";
    /** Player inventory main+hotbar size (excludes armor/offhand). */
    private static final int INVENTORY_SIZE = 36;

    private InventoryRestrictionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!NaturalisConfig.isInventoryRestrictionEnabled(player.level())) {
            var data = CompatAccess.getPersistentData(player);
            if (data.contains(ROOT_TAG)) {
                CompoundTag tag = CompatAccess.getCompound(data, ROOT_TAG);
                tag.remove(LAST_SELECTED_SLOT);
                data.put(ROOT_TAG, tag);
            }
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (!InventoryRestrictionManager.isQuadruped(morphId)) {
            var data = CompatAccess.getPersistentData(player);
            if (data.contains(ROOT_TAG)) {
                CompoundTag tag = CompatAccess.getCompound(data, ROOT_TAG);
                tag.remove(LAST_SELECTED_SLOT);
                data.put(ROOT_TAG, tag);
            }
            return;
        }

        int allowedSlots = MorphKnowledgeManager.getAllowedHotbarSlots(player, morphId);

        if (allowedSlots >= 9) {
            return; // handling fully unlocked: full hotbar, no restriction needed
        }

        // Pull items from locked hotbar / backpack into unlocked slots (mouth first).
        cascadeIntoUnlockedSlots(player.getInventory(), allowedSlots);

        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = CompatAccess.getCompound(root, ROOT_TAG);

        int current = getSelectedSlot(player);
        int previous = CompatAccess.contains(tag, LAST_SELECTED_SLOT) ? CompatAccess.getInt(tag, LAST_SELECTED_SLOT) : current;
        int maxSlotIndex = allowedSlots - 1;

        if (current > maxSlotIndex) {
            current = resolveRestrictedSlot(previous, current, maxSlotIndex);
            setSelectedSlot(player, current);
        }

        tag.putInt(LAST_SELECTED_SLOT, current);
        root.put(ROOT_TAG, tag);
    }

    /**
     * When unlocked slots (mouth = 0, then paws) are empty but later inventory has items,
     * shift those items forward so pickups that landed in locked slots remain usable.
     */
    private static void cascadeIntoUnlockedSlots(Inventory inventory, int allowedSlots) {
        for (int dest = 0; dest < allowedSlots; dest++) {
            if (!inventory.getItem(dest).isEmpty()) {
                continue;
            }
            for (int src = dest + 1; src < INVENTORY_SIZE; src++) {
                ItemStack srcStack = inventory.getItem(src);
                if (srcStack.isEmpty()) {
                    continue;
                }
                inventory.setItem(dest, srcStack.copy());
                inventory.setItem(src, ItemStack.EMPTY);
                break;
            }
        }
    }

    private static int resolveRestrictedSlot(int previousSlot, int attemptedSlot, int maxSlotIndex) {
        if (attemptedSlot <= maxSlotIndex) {
            return attemptedSlot;
        }

        if (previousSlot == maxSlotIndex && attemptedSlot == maxSlotIndex + 1) {
            return 0;
        }
        if (previousSlot == 0 && attemptedSlot == 8) {
            return maxSlotIndex;
        }

        int forwardStep = Math.floorMod(attemptedSlot - previousSlot, 9);
        int backwardStep = Math.floorMod(previousSlot - attemptedSlot, 9);
        if (forwardStep == 1) {
            return 0;
        }
        if (backwardStep == 1) {
            return maxSlotIndex;
        }

        return maxSlotIndex;
    }

    private static int getSelectedSlot(ServerPlayer player) {
        try {
            Object raw = player.getInventory().getClass().getField("selected").get(player.getInventory());
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = player.getInventory().getClass().getMethod("getSelectedSlot").invoke(player.getInventory());
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return 0;
    }

    private static void setSelectedSlot(ServerPlayer player, int slot) {
        try {
            player.getInventory().getClass().getField("selected").set(player.getInventory(), slot);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            player.getInventory().getClass().getMethod("setSelectedSlot", int.class).invoke(player.getInventory(), slot);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }
}
