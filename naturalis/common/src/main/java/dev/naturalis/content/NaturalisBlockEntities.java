package dev.naturalis.content;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.NaturalisMod;
import dev.naturalis.world.EchoForgeBlockEntity;
import dev.naturalis.world.MorphBeaconBlockEntity;
import dev.naturalis.world.MorphArmorForgeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisBlockEntities {

    private NaturalisBlockEntities() {
    }

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NaturalisMod.ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoForgeBlockEntity>> ECHO_FORGE = BLOCK_ENTITIES.register("echo_forge",
        () -> CompatAccess.createBlockEntityType(EchoForgeBlockEntity::new, NaturalisBlocks.ECHO_FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MorphArmorForgeBlockEntity>> MORPH_ARMOR_FORGE = BLOCK_ENTITIES.register("morph_armor_forge",
        () -> CompatAccess.createBlockEntityType(MorphArmorForgeBlockEntity::new, NaturalisBlocks.MORPH_ARMOR_FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MorphBeaconBlockEntity>> MORPH_BEACON = BLOCK_ENTITIES.register("morph_beacon",
        () -> CompatAccess.createBlockEntityType(MorphBeaconBlockEntity::new, NaturalisBlocks.MORPH_BEACON.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
