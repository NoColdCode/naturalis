package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: syncs humanity level and resonance-active flag. */
public final class HumanityPayload {

    private final int humanity;
    private final boolean active;

    public HumanityPayload(int humanity, boolean active) {
        this.humanity = humanity;
        this.active = active;
    }

    public static HumanityPayload decode(FriendlyByteBuf buf) {
        return new HumanityPayload(buf.readVarInt(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(humanity);
        buf.writeBoolean(active);
    }

    public int humanity() { return humanity; }
    public boolean active() { return active; }
}
