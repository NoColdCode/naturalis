package dev.naturalis.fabric;

import dev.naturalis.fabric.blockentity.MorphBeaconFabricBlockEntity;
import dev.naturalis.world.EchoForgeBlockEntity;
import dev.naturalis.world.MorphArmorForgeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FabricNaturalisBlockEntities {

    private static final String MOD_ID = "naturalis";

    public static final BlockEntityType<EchoForgeBlockEntity> ECHO_FORGE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "echo_forge"),
        FabricBlockEntityTypeBuilder.create(EchoForgeBlockEntity::new, FabricNaturalisItems.ECHO_FORGE_BLOCK).build()
    );

    public static final BlockEntityType<MorphArmorForgeBlockEntity> MORPH_ARMOR_FORGE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "morph_armor_forge"),
        FabricBlockEntityTypeBuilder.create(MorphArmorForgeBlockEntity::new, FabricNaturalisItems.MORPH_ARMOR_FORGE_BLOCK).build()
    );

    public static final BlockEntityType<MorphBeaconFabricBlockEntity> MORPH_BEACON = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "morph_beacon"),
        FabricBlockEntityTypeBuilder.create(MorphBeaconFabricBlockEntity::new, FabricNaturalisItems.MORPH_BEACON_BLOCK).build()
    );

    private FabricNaturalisBlockEntities() {
    }

    public static void register() {
        // Class-load side effects register block entity types.
    }
}
