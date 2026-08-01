package dev.naturalis.item;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import tocraft.walkers.api.events.ShapeEvents;

/**
 * Handles automatic equipping and unequipping of {@link MorphArmorItem} when
 * the player morphs or unmorphs.
 *
 * <p>On morph: if the player has a MorphArmor item in their inventory whose
 * stored mob ID matches the morph target, it is moved to the chest slot
 * (only if the chest slot is empty or already contains a MorphArmorItem for
 * a different morph).
 *
 * <p>On unmorph: if the player has a MorphArmorItem in the chest slot, it is
 * returned to the first available inventory slot.
 */
public final class MorphArmorEvents {

    private static boolean registered = false;
    private static final String ARMOR_SWAP_TAG_ROOT = "naturalis_morph_armor_swap";
    private static final String ARMOR_SWAP_TICK = "tick";
    private static final String ARMOR_SWAP_TARGET = "target";

    private MorphArmorEvents() {}

    public static void register() {
        if (registered) return;
        registered = true;

        ShapeEvents.SWAP_SHAPE.register((player, to) -> {
            ServerPlayer serverPlayer = (ServerPlayer) player;

            if (isDuplicateSwapEvent(serverPlayer, to)) {
                return InteractionResult.PASS;
            }

            if (to != null) {
                // Morphing â€” try to auto-equip the matching armor
                ResourceLocation morphId = BuiltInRegistries.ENTITY_TYPE.getKey(to.getType());
                if (morphId != null) {
                    tryAutoEquipArmor(serverPlayer, morphId.toString());
                }
            } else {
                // Unmorphing â€” return any MorphArmor from chest to inventory
                tryReturnArmorToInventory(serverPlayer);
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isDuplicateSwapEvent(ServerPlayer player, net.minecraft.world.entity.LivingEntity to) {
        var root = player.getPersistentData();
        if (!root.contains(ARMOR_SWAP_TAG_ROOT)) {
            root.put(ARMOR_SWAP_TAG_ROOT, new net.minecraft.nbt.CompoundTag());
        }

        var tag = CompatAccess.getCompound(root, ARMOR_SWAP_TAG_ROOT);
        long now = player.level().getGameTime();
        String target = to == null ? "<none>" : BuiltInRegistries.ENTITY_TYPE.getKey(to.getType()).toString();

        long lastTick = CompatAccess.getLong(tag, ARMOR_SWAP_TICK);
        String lastTarget = CompatAccess.getString(tag, ARMOR_SWAP_TARGET);
        boolean duplicate = (lastTick == now) && target.equals(lastTarget);

        if (!duplicate) {
            tag.putLong(ARMOR_SWAP_TICK, now);
            tag.putString(ARMOR_SWAP_TARGET, target);
        }

        return duplicate;
    }

    // â”€â”€ Equip on morph â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static void tryAutoEquipArmor(ServerPlayer player, String morphIdStr) {
        ItemStack current = player.getItemBySlot(EquipmentSlot.CHEST);

        // Already wearing the right armor â€” nothing to do
        if (current.getItem() instanceof MorphArmorItem) {
            String existingMob = MorphDataUtil.getMobId(current);
            if (morphIdStr.equals(existingMob)) return;
        }

        // Scan inventory for a matching MorphArmorItem
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof MorphArmorItem)) continue;

            String storedMob = MorphDataUtil.getMobId(stack);
            if (!morphIdStr.equals(storedMob)) continue;

            // Found a match â€” equip it
            ItemStack toEquip = stack.split(1);
            inventory.setItem(i, stack); // put remainder (empty) back

            // If chest is occupied, stash the displaced item
            if (!current.isEmpty()) {
                if (!inventory.add(current)) {
                    // No room; drop displaced item
                    player.drop(current, false);
                }
            }

            player.setItemSlot(EquipmentSlot.CHEST, toEquip);
            return;
        }
    }

    // â”€â”€ Return to inventory on unmorph â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static void tryReturnArmorToInventory(ServerPlayer player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof MorphArmorItem)) return;

        ItemStack toReturn = chestStack.copy();
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        if (!player.getInventory().add(toReturn)) {
            // No inventory space â€” drop it at the player
            player.drop(toReturn, false);
        }
    }
}
