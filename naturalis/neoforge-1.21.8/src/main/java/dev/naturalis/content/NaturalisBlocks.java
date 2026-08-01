package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.world.EchoForgeBlock;
import dev.naturalis.world.MorphBeaconBlock;
import dev.naturalis.world.MorphArmorForgeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisBlocks {

    private NaturalisBlocks() {
    }

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Naturalis.MOD_ID);

    public static final DeferredBlock<Block> ECHO_FORGE = BLOCKS.registerBlock("echo_forge",
        EchoForgeBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.5F)
            .sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> MORPH_ARMOR_FORGE = BLOCKS.registerBlock("morph_armor_forge",
        MorphArmorForgeBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(4.0F)
            .sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> NATURAL_PORTAL_FRAME = BLOCKS.registerSimpleBlock("natural_portal_frame",
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
            .strength(4.0F, 1200.0F)
            .sound(SoundType.BASALT)
            .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> NATURAL_PORTAL = BLOCKS.registerSimpleBlock("natural_portal",
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
            .strength(-1.0F, 3600000.0F)
            .sound(SoundType.GLASS)
            .noCollission()
            .noLootTable()
            .lightLevel(state -> 11));

    public static final DeferredBlock<Block> ECHO_BLOCK = BLOCKS.registerSimpleBlock("echo_block",
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(2.2F)
            .sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> MORPH_BEACON = BLOCKS.registerBlock("morph_beacon",
        MorphBeaconBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(4.5F)
            .sound(SoundType.AMETHYST_CLUSTER)
            .lightLevel(state -> 12)
            .requiresCorrectToolForDrops());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
