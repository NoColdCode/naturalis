package dev.naturalis.profile;

import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public final class MobProfileClientRegistration {

    private MobProfileClientRegistration() {
    }

    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MobProfileReloadListener.INSTANCE);
    }
}
