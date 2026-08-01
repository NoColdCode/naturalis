package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: syncs game-rule flags that affect client rendering/logic. */
public final class RuleFlagsPayload {

    private final boolean colorFilterEnabled;
    private final boolean inventoryRestrictionEnabled;
    private final boolean instinctsEnabled;

    public RuleFlagsPayload(boolean colorFilterEnabled, boolean inventoryRestrictionEnabled, boolean instinctsEnabled) {
        this.colorFilterEnabled = colorFilterEnabled;
        this.inventoryRestrictionEnabled = inventoryRestrictionEnabled;
        this.instinctsEnabled = instinctsEnabled;
    }

    public static RuleFlagsPayload decode(FriendlyByteBuf buf) {
        return new RuleFlagsPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(colorFilterEnabled);
        buf.writeBoolean(inventoryRestrictionEnabled);
        buf.writeBoolean(instinctsEnabled);
    }

    public boolean colorFilterEnabled() { return colorFilterEnabled; }
    public boolean inventoryRestrictionEnabled() { return inventoryRestrictionEnabled; }
    public boolean instinctsEnabled() { return instinctsEnabled; }
}
