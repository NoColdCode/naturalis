package dev.naturalis.instinct;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge stub for shared wander-driver constants / NBT access.
 * Full instinct tick logic lives in {@link InstinctEvents}.
 */
public final class InstinctLogic {

    private static final String ROOT_TAG = "naturalis_instinct";

    public static final String WANDER_UNTIL_TICK = "wander_until_tick";
    public static final String WANDER_SYNC_YAW = "wander_sync_yaw";
    public static final String WANDER_SYNC_PITCH = "wander_sync_pitch";

    private InstinctLogic() {
    }

    public static CompoundTag getOrCreateInstinctTag(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }
}
