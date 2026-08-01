package dev.naturalis.profile;

import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class MobProfileNeoForgeEvents {

    private MobProfileNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onServerReload(AddServerReloadListenersEvent event) {
        event.addListener(
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "mob_profiles"),
            MobProfileReloadListener.INSTANCE
        );
    }
}
