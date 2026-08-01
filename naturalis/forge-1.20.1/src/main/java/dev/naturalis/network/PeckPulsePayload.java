package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: beak lunge when the player pecks. */
public final class PeckPulsePayload {

    private final boolean struckEntity;
    private final boolean struckBlock;

    public PeckPulsePayload(boolean struckEntity, boolean struckBlock) {
        this.struckEntity = struckEntity;
        this.struckBlock = struckBlock;
    }

    public static PeckPulsePayload decode(FriendlyByteBuf buf) {
        return new PeckPulsePayload(buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(struckEntity);
        buf.writeBoolean(struckBlock);
    }

    public boolean struckEntity() { return struckEntity; }
    public boolean struckBlock() { return struckBlock; }
}
