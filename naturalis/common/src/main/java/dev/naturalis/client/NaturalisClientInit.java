package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.profile.MobProfileClientRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only mod bootstrap (config UI, client pref sync). Loaded only on the physical client
 * via reflection from {@link dev.naturalis.Naturalis} so dedicated servers never touch {@code Screen}.
 */
public final class NaturalisClientInit {

    private NaturalisClientInit() {
    }

    public static void register(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (minecraft, parent) -> new ConfigurationScreen(container, parent)
        );
        modEventBus.addListener(NaturalisClientInit::onConfigLoad);
        modEventBus.addListener(MobProfileClientRegistration::registerClientReloadListeners);
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (!NaturalisMod.ID.equals(event.getConfig().getModId())) {
            return;
        }
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            NaturalisClientPrefs.syncFromModConfig();
        }
    }
}
