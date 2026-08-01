package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client → Server: toggles the feral curl-sleep state. */
public final class CurlSleepTogglePayload {

    public CurlSleepTogglePayload() {
    }

    public static CurlSleepTogglePayload decode(FriendlyByteBuf buf) {
        return new CurlSleepTogglePayload();
    }

    public void encode(FriendlyByteBuf buf) {
        // No payload data.
    }
}
