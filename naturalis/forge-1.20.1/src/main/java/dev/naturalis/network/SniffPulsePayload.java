package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: active sniff pulse for scent vision and camera motion. */
public final class SniffPulsePayload {

    private final int intensity;
    private final int trailCount;
    private final int preyCount;
    private final int hostileCount;

    public SniffPulsePayload(int intensity, int trailCount, int preyCount, int hostileCount) {
        this.intensity = intensity;
        this.trailCount = trailCount;
        this.preyCount = preyCount;
        this.hostileCount = hostileCount;
    }

    public static SniffPulsePayload decode(FriendlyByteBuf buf) {
        return new SniffPulsePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(intensity);
        buf.writeVarInt(trailCount);
        buf.writeVarInt(preyCount);
        buf.writeVarInt(hostileCount);
    }

    public int intensity() { return intensity; }
    public int trailCount() { return trailCount; }
    public int preyCount() { return preyCount; }
    public int hostileCount() { return hostileCount; }
}
