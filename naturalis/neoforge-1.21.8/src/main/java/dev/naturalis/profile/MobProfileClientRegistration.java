package dev.naturalis.profile;

import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public final class MobProfileClientRegistration {

    private MobProfileClientRegistration() {
    }

    public static void registerClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "mob_profiles"),
            MobProfileReloadListener.INSTANCE
        );
    }
}
