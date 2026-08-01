package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.world.EchoForgeBlockEntity;
import dev.naturalis.world.MorphBeaconBlockEntity;
import dev.naturalis.world.MorphArmorForgeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisBlockEntities {

    private NaturalisBlockEntities() {
    }

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Naturalis.MOD_ID);

    public static final RegistryObject<BlockEntityType<EchoForgeBlockEntity>> ECHO_FORGE =
        BLOCK_ENTITIES.register("echo_forge", () ->
            CompatAccess.createBlockEntityType(EchoForgeBlockEntity::new, resolveBlock("echo_forge"))
        );

    public static final RegistryObject<BlockEntityType<MorphArmorForgeBlockEntity>> MORPH_ARMOR_FORGE =
        BLOCK_ENTITIES.register("morph_armor_forge", () ->
            CompatAccess.createBlockEntityType(MorphArmorForgeBlockEntity::new, resolveBlock("morph_armor_forge"))
        );

    public static final RegistryObject<BlockEntityType<MorphBeaconBlockEntity>> MORPH_BEACON =
        BLOCK_ENTITIES.register("morph_beacon", () ->
            CompatAccess.createBlockEntityType(MorphBeaconBlockEntity::new, resolveBlock("morph_beacon"))
        );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private static Block resolveBlock(String name) {
        ResourceLocation id = new ResourceLocation(Naturalis.MOD_ID, name);
        Block block = CompatAccess.getBlock(id);
        if (block == null) {
            throw new IllegalStateException("Missing block for block entity registration: " + id);
        }
        return block;
    }
}
