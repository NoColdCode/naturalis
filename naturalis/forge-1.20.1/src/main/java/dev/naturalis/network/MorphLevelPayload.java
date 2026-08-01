package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: syncs morph level, hotbar slots, inventory access, utilities rank, and global XP. */
public final class MorphLevelPayload {

    private final int level;
    private final int hotbarSlots;
    private final boolean inventoryUnlocked;
    private final int utilitiesRank;
    private final int globalXp;

    public MorphLevelPayload(int level, int hotbarSlots, boolean inventoryUnlocked) {
        this(level, hotbarSlots, inventoryUnlocked, 0, 0);
    }

    public MorphLevelPayload(int level, int hotbarSlots, boolean inventoryUnlocked, int utilitiesRank) {
        this(level, hotbarSlots, inventoryUnlocked, utilitiesRank, 0);
    }

    public MorphLevelPayload(int level, int hotbarSlots, boolean inventoryUnlocked, int utilitiesRank, int globalXp) {
        this.level = level;
        this.hotbarSlots = hotbarSlots;
        this.inventoryUnlocked = inventoryUnlocked;
        this.utilitiesRank = utilitiesRank;
        this.globalXp = Math.max(0, globalXp);
    }

    public static MorphLevelPayload decode(FriendlyByteBuf buf) {
        return new MorphLevelPayload(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarInt(hotbarSlots);
        buf.writeBoolean(inventoryUnlocked);
        buf.writeVarInt(utilitiesRank);
        buf.writeVarInt(globalXp);
    }

    public int level() {
        return level;
    }

    public int hotbarSlots() {
        return hotbarSlots;
    }

    public boolean inventoryUnlocked() {
        return inventoryUnlocked;
    }

    public int utilitiesRank() {
        return utilitiesRank;
    }

    public int globalXp() {
        return globalXp;
    }
}
