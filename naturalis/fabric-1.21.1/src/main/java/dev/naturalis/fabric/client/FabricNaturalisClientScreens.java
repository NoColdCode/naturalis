package dev.naturalis.fabric.client;

import dev.naturalis.client.screen.EchoForgeScreen;
import dev.naturalis.client.screen.MorphArmorForgeScreen;
import dev.naturalis.client.screen.MorphKnowledgeScreen;
import dev.naturalis.fabric.FabricNaturalisMenus;
import dev.naturalis.fabric.client.screen.MorphBeaconFabricScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public final class FabricNaturalisClientScreens {

    private FabricNaturalisClientScreens() {
    }

    public static void register() {
        MenuScreens.register(FabricNaturalisMenus.ECHO_FORGE, EchoForgeScreen::new);
        MenuScreens.register(FabricNaturalisMenus.MORPH_ARMOR_FORGE, MorphArmorForgeScreen::new);
        MenuScreens.register(FabricNaturalisMenus.MORPH_BEACON, MorphBeaconFabricScreen::new);
        MenuScreens.register(FabricNaturalisMenus.MORPH_KNOWLEDGE, MorphKnowledgeScreen::new);
    }
}
