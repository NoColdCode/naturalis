package dev.naturalis.profile;

import dev.naturalis.NaturalisMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class MobProfileNeoForgeEvents {

    private MobProfileNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        event.addListener(MobProfileReloadListener.INSTANCE);
    }
}
