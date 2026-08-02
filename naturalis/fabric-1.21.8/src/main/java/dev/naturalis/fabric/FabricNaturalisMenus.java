package dev.naturalis.fabric;

import dev.naturalis.fabric.menu.MorphBeaconFabricMenu;
import dev.naturalis.world.menu.EchoForgeMenu;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import dev.naturalis.world.menu.MorphKnowledgeMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class FabricNaturalisMenus {

    private static final String MOD_ID = "naturalis";

    public static final ExtendedScreenHandlerType<EchoForgeMenu, BlockPos> ECHO_FORGE = Registry.register(
        BuiltInRegistries.MENU,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "echo_forge"),
        new ExtendedScreenHandlerType<>((syncId, inv, pos) -> new EchoForgeMenu(syncId, inv, pos), BlockPos.STREAM_CODEC)
    );

    public static final ExtendedScreenHandlerType<MorphArmorForgeMenu, BlockPos> MORPH_ARMOR_FORGE = Registry.register(
        BuiltInRegistries.MENU,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "morph_armor_forge"),
        new ExtendedScreenHandlerType<>((syncId, inv, pos) -> new MorphArmorForgeMenu(syncId, inv, pos), BlockPos.STREAM_CODEC)
    );

    public static final ExtendedScreenHandlerType<MorphBeaconFabricMenu, BlockPos> MORPH_BEACON = Registry.register(
        BuiltInRegistries.MENU,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "morph_beacon"),
        new ExtendedScreenHandlerType<>((syncId, inv, pos) -> new MorphBeaconFabricMenu(syncId, inv, pos), BlockPos.STREAM_CODEC)
    );

    /** Opened via {@code SimpleMenuProvider} from {@code /morph knowledge}; no extra sync payload. */
    public static final MenuType<MorphKnowledgeMenu> MORPH_KNOWLEDGE = Registry.register(
        BuiltInRegistries.MENU,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "morph_knowledge"),
        new MenuType<>((syncId, inv) -> new MorphKnowledgeMenu(syncId, inv, (FriendlyByteBuf) null), FeatureFlags.VANILLA_SET)
    );

    private FabricNaturalisMenus() {
    }

    public static void register() {
        // Class-load side effects register menu types.
    }
}
