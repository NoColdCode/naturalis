package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.world.EchoForgeBlock;
import dev.naturalis.world.MorphBeaconBlock;
import dev.naturalis.world.MorphArmorForgeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisBlocks {

    private NaturalisBlocks() {
    }

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Naturalis.MOD_ID);

    public static final RegistryObject<Block> ECHO_FORGE = BLOCKS.register("echo_forge",
        () -> new EchoForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.5F)
            .sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MORPH_ARMOR_FORGE = BLOCKS.register("morph_armor_forge",
        () -> new MorphArmorForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(4.0F)
            .sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NATURAL_PORTAL_FRAME = BLOCKS.register("natural_portal_frame",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
            .strength(4.0F, 1200.0F)
            .sound(SoundType.BASALT)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NATURAL_PORTAL = BLOCKS.register("natural_portal",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
            .strength(-1.0F, 3600000.0F)
            .sound(SoundType.GLASS)
            .noCollission()
            .noLootTable()
            .lightLevel(state -> 11)));

    public static final RegistryObject<Block> ECHO_BLOCK = BLOCKS.register("echo_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(2.2F)
            .sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MORPH_BEACON = BLOCKS.register("morph_beacon",
        () -> new MorphBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(4.5F)
            .sound(SoundType.AMETHYST_CLUSTER)
            .lightLevel(state -> 12)
            .requiresCorrectToolForDrops()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
