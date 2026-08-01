package dev.naturalis.profile;

import dev.naturalis.Naturalis;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class MobProfileForgeEvents {

    private MobProfileForgeEvents() {
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(MobProfileReloadListener.INSTANCE);
    }
}
