package dev.naturalis.content;

import dev.naturalis.NaturalisMod;
import dev.naturalis.world.menu.EchoForgeMenu;
import dev.naturalis.world.menu.MorphBeaconMenu;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import dev.naturalis.world.menu.MorphKnowledgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisMenus {

    private NaturalisMenus() {
    }

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NaturalisMod.ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EchoForgeMenu>> ECHO_FORGE = MENUS.register("echo_forge",
        () -> IMenuTypeExtension.create(EchoForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MorphKnowledgeMenu>> MORPH_KNOWLEDGE = MENUS.register("morph_knowledge",
        () -> IMenuTypeExtension.create(MorphKnowledgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MorphArmorForgeMenu>> MORPH_ARMOR_FORGE = MENUS.register("morph_armor_forge",
        () -> IMenuTypeExtension.create(MorphArmorForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MorphBeaconMenu>> MORPH_BEACON = MENUS.register("morph_beacon",
        () -> IMenuTypeExtension.create(MorphBeaconMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
