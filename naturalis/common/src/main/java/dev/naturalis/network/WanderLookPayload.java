package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → client: AFK wander is steering look — client must apply camera rotation locally. */
public final class WanderLookPayload {

    private final boolean active;
    private final float yaw;
    private final float pitch;
    private final float bodyYaw;

    public WanderLookPayload(boolean active, float yaw, float pitch, float bodyYaw) {
        this.active = active;
        this.yaw = yaw;
        this.pitch = pitch;
        this.bodyYaw = bodyYaw;
    }

    public static WanderLookPayload decode(FriendlyByteBuf buf) {
        return new WanderLookPayload(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(bodyYaw);
    }

    public boolean active() {
        return active;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float bodyYaw() {
        return bodyYaw;
    }
}
