package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

public final class MorphQuickSlotSessionPayload {

    private final boolean active;

    public MorphQuickSlotSessionPayload(boolean active) {
        this.active = active;
    }

    public boolean active() {
        return active;
    }

    public static MorphQuickSlotSessionPayload decode(FriendlyByteBuf buf) {
        return new MorphQuickSlotSessionPayload(buf.readBoolean());
    }

    public static void encode(MorphQuickSlotSessionPayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.active);
    }
}
