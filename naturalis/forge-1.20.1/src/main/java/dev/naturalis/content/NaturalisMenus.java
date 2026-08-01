package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.world.menu.EchoForgeMenu;
import dev.naturalis.world.menu.MorphBeaconMenu;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import dev.naturalis.world.menu.MorphKnowledgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisMenus {

    private NaturalisMenus() {
    }

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Naturalis.MOD_ID);

    public static final RegistryObject<MenuType<EchoForgeMenu>> ECHO_FORGE = MENUS.register("echo_forge",
        () -> IForgeMenuType.create(EchoForgeMenu::new));

    public static final RegistryObject<MenuType<MorphKnowledgeMenu>> MORPH_KNOWLEDGE = MENUS.register("morph_knowledge",
        () -> IForgeMenuType.create(MorphKnowledgeMenu::new));

    public static final RegistryObject<MenuType<MorphArmorForgeMenu>> MORPH_ARMOR_FORGE = MENUS.register("morph_armor_forge",
        () -> IForgeMenuType.create(MorphArmorForgeMenu::new));

    public static final RegistryObject<MenuType<MorphBeaconMenu>> MORPH_BEACON = MENUS.register("morph_beacon",
        () -> IForgeMenuType.create(MorphBeaconMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
