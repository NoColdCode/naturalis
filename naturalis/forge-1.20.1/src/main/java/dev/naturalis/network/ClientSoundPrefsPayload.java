package dev.naturalis.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server → Client: toggles morph perception UI sounds (see config/naturalis-client.json). */
public final class ClientSoundPrefsPayload {

    private final boolean muteMorphPerceptionSounds;

    public ClientSoundPrefsPayload(boolean muteMorphPerceptionSounds) {
        this.muteMorphPerceptionSounds = muteMorphPerceptionSounds;
    }

    public static ClientSoundPrefsPayload decode(FriendlyByteBuf buf) {
        return new ClientSoundPrefsPayload(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(muteMorphPerceptionSounds);
    }

    public boolean muteMorphPerceptionSounds() {
        return muteMorphPerceptionSounds;
    }
}
