package dev.naturalis.gameplay;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Persists primal-movement key state in player NBT; usable without NeoForge gameplay subscribers. */
public final class PrimalMovementState {

    static final String MOVEMENT_ROOT = "naturalis_movement";
    /** Movement-tag boolean written while primal-movement keybind is held (readable cross-package). */
    public static final String PRIMAL_KEY_DOWN = "primal_key_down";

    private PrimalMovementState() {
    }

    public static void setPrimalKeyDown(ServerPlayer player, boolean down) {
        movementTag(player).putBoolean(PRIMAL_KEY_DOWN, down);
    }

    public static CompoundTag movementTag(Player player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(MOVEMENT_ROOT)) {
            root.put(MOVEMENT_ROOT, new CompoundTag());
        }
        return CompatAccess.getCompound(root, MOVEMENT_ROOT);
    }
}
