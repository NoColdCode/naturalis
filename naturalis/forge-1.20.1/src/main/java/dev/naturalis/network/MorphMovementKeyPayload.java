package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client → Server: primal movement key pressed/released state. */
public final class MorphMovementKeyPayload {

    private final boolean pressed;

    public MorphMovementKeyPayload(boolean pressed) {
        this.pressed = pressed;
    }

    public static MorphMovementKeyPayload decode(FriendlyByteBuf buf) {
        return new MorphMovementKeyPayload(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(pressed);
    }

    public boolean pressed() { return pressed; }
}
