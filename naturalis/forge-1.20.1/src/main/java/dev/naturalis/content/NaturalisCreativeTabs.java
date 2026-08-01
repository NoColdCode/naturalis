package dev.naturalis.content;

import dev.naturalis.Naturalis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisCreativeTabs {

    private NaturalisCreativeTabs() {
    }

    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Naturalis.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NATURALIS_TAB =
        CREATIVE_MODE_TABS.register("naturalis", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.naturalis.main"))
            .icon(() -> new ItemStack(NaturalisBlocks.ECHO_FORGE.get()))
            .displayItems((parameters, output) -> {
                output.accept(NaturalisItems.ECHO_COLLECTOR.get());
                output.accept(NaturalisItems.EMPTY_ECHO_VIAL.get());
                output.accept(NaturalisItems.FILLED_ECHO_VIAL.get());
                output.accept(NaturalisItems.MORPH_ORB.get());
                output.accept(NaturalisItems.BREWED_MORPH_POTION.get());
                output.accept(NaturalisItems.BREWED_MORPH_SPLASH_POTION.get());
                output.accept(NaturalisItems.BREWED_MORPH_LINGERING_POTION.get());
                output.accept(NaturalisItems.MORPH_BINDING_POTION.get());
                output.accept(NaturalisItems.MORPH_BINDING_SPLASH_POTION.get());
                output.accept(NaturalisItems.MORPH_BINDING_LINGERING_POTION.get());
                output.accept(NaturalisItems.MEMORY_TOKEN.get());
                output.accept(NaturalisItems.HUMANITY_TOKEN_5.get());
                output.accept(NaturalisItems.HUMANITY_TOKEN_10.get());
                output.accept(NaturalisItems.REHUMANIZER.get());
                output.accept(NaturalisItems.TRANSLATION_CORE.get());
                output.accept(NaturalisItems.KNOWLEDGE_RESET_TOTEM.get());
                output.accept(NaturalisItems.APEX_ELIXIR.get());
                output.accept(NaturalisItems.GROWTH_SEED.get());
                output.accept(NaturalisItems.NATURAL_SIGIL_KEY.get());
                output.accept(NaturalisItems.HUMAN_AMULET.get());
                output.accept(NaturalisItems.NATURAL_STAR.get());
                output.accept(NaturalisItems.SOVEREIGN_AMULET.get());
                output.accept(NaturalisItems.ECHO_MORPH_BLADE.get());
                output.accept(NaturalisItems.ECHO_MORPH_PICK.get());
                output.accept(NaturalisItems.ECHO_MORPH_AXE.get());
                output.accept(NaturalisItems.ECHO_MORPH_SHOVEL.get());
                output.accept(NaturalisItems.ECHO_FORGE_ITEM.get());
                output.accept(NaturalisItems.MORPH_ARMOR.get());
                output.accept(NaturalisItems.MORPH_ARMOR_FORGE_ITEM.get());
                output.accept(NaturalisItems.NATURAL_PORTAL_FRAME_ITEM.get());
                output.accept(NaturalisItems.ECHO_BLOCK_ITEM.get());
                output.accept(NaturalisItems.MORPH_BEACON_ITEM.get());
            })
            .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
