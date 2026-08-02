package dev.naturalis.fabric;

import dev.naturalis.fabric.block.MorphBeaconFabricBlock;
import dev.naturalis.world.EchoForgeBlock;
import dev.naturalis.world.MorphArmorForgeBlock;
import dev.naturalis.item.BrewedMorphPotionItem;
import dev.naturalis.item.FilledEchoVialItem;
import dev.naturalis.item.FixedNameLingeringPotionItem;
import dev.naturalis.item.FixedNameSplashPotionItem;
import dev.naturalis.item.HabitChrysalisItem;
import dev.naturalis.item.MorphBindingPotionItem;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphOnlyToolItem;
import dev.naturalis.item.MorphOrbItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class FabricNaturalisItems {

    private static final String MOD_ID = "naturalis";

    // Block registrations
    public static final Block ECHO_FORGE_BLOCK = registerBlock("echo_forge",
        new EchoForgeBlock(blockProperties("echo_forge").mapColor(MapColor.METAL)
            .strength(3.5F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));
    public static final Block MORPH_ARMOR_FORGE_BLOCK = registerBlock("morph_armor_forge",
        new MorphArmorForgeBlock(blockProperties("morph_armor_forge").mapColor(MapColor.METAL)
            .strength(4.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));
    public static final Block NATURAL_PORTAL_FRAME_BLOCK = registerBlock("natural_portal_frame",
        new Block(blockProperties("natural_portal_frame").mapColor(MapColor.COLOR_GREEN)
            .strength(4.0F, 1200.0F).sound(SoundType.BASALT).requiresCorrectToolForDrops()));
    public static final Block ECHO_BLOCK_BLOCK = registerBlock("echo_block",
        new Block(blockProperties("echo_block").mapColor(MapColor.COLOR_CYAN)
            .strength(2.2F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops()));
    public static final Block MORPH_BEACON_BLOCK = registerBlock("morph_beacon",
        new MorphBeaconFabricBlock(blockProperties("morph_beacon").mapColor(MapColor.COLOR_CYAN)
            .strength(4.5F).sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 12).requiresCorrectToolForDrops()));

    public static final Item ECHO_COLLECTOR = register("echo_collector", new Item(properties("echo_collector").stacksTo(1)));
    public static final Item EMPTY_ECHO_VIAL = register("empty_echo_vial", new Item(properties("empty_echo_vial").stacksTo(64)));
    public static final Item FILLED_ECHO_VIAL = register("filled_echo_vial", new FilledEchoVialItem(properties("filled_echo_vial").stacksTo(64)));
    public static final Item MORPH_ORB = register("morph_orb", new MorphOrbItem(properties("morph_orb").stacksTo(16)));

    public static final Item BREWED_MORPH_POTION = register("brewed_morph_potion", new BrewedMorphPotionItem(properties("brewed_morph_potion").stacksTo(1)));
    public static final Item BREWED_MORPH_SPLASH_POTION = register("brewed_morph_splash_potion", new FixedNameSplashPotionItem(properties("brewed_morph_splash_potion").stacksTo(1), "item.naturalis.brewed_morph_splash_potion"));
    public static final Item BREWED_MORPH_LINGERING_POTION = register("brewed_morph_lingering_potion", new FixedNameLingeringPotionItem(properties("brewed_morph_lingering_potion").stacksTo(1), "item.naturalis.brewed_morph_lingering_potion"));

    public static final Item MORPH_BINDING_POTION = register("morph_binding_potion", new MorphBindingPotionItem(properties("morph_binding_potion").stacksTo(1)));
    public static final Item MORPH_BINDING_SPLASH_POTION = register("morph_binding_splash_potion", new FixedNameSplashPotionItem(properties("morph_binding_splash_potion").stacksTo(1), "item.naturalis.morph_binding_splash_potion"));
    public static final Item MORPH_BINDING_LINGERING_POTION = register("morph_binding_lingering_potion", new FixedNameLingeringPotionItem(properties("morph_binding_lingering_potion").stacksTo(1), "item.naturalis.morph_binding_lingering_potion"));

    public static final Item MEMORY_TOKEN = register("memory_token", new Item(properties("memory_token").stacksTo(16)));
    public static final Item HUMANITY_TOKEN_5 = register("humanity_token_5", new Item(properties("humanity_token_5").stacksTo(16)));
    public static final Item HUMANITY_TOKEN_10 = register("humanity_token_10", new Item(properties("humanity_token_10").stacksTo(16)));
    public static final Item REHUMANIZER = register("rehumanizer", new Item(properties("rehumanizer").stacksTo(16)));
    public static final Item TRANSLATION_CORE = register("translation_core", new Item(properties("translation_core").stacksTo(1)));
    public static final Item KNOWLEDGE_RESET_TOTEM = register("knowledge_reset_totem", new Item(properties("knowledge_reset_totem").stacksTo(16)));
    public static final Item APEX_ELIXIR = register("apex_elixir", new Item(properties("apex_elixir").stacksTo(16)));
    public static final Item GROWTH_SEED = register("growth_seed", new Item(properties("growth_seed").stacksTo(32)));
    public static final Item NATURAL_SIGIL_KEY = register("natural_sigil_key", new Item(properties("natural_sigil_key").stacksTo(16)));
    public static final Item HUMAN_AMULET = register("human_amulet", new Item(properties("human_amulet").stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Item NATURAL_STAR = register("natural_star", new Item(properties("natural_star").stacksTo(64).rarity(Rarity.RARE).fireResistant()));
    public static final Item SOVEREIGN_AMULET = register("sovereign_amulet", new Item(properties("sovereign_amulet").stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final Item HABIT_CHRYSALIS = register("habit_chrysalis", new HabitChrysalisItem(properties("habit_chrysalis").stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final Item ECHO_MORPH_BLADE = register("echo_morph_blade", new MorphOnlyToolItem(properties("echo_morph_blade").stacksTo(1).durability(768), MobEffects.DAMAGE_BOOST, 200, 120));
    public static final Item ECHO_MORPH_PICK = register("echo_morph_pick", new MorphOnlyToolItem(properties("echo_morph_pick").stacksTo(1).durability(768), MobEffects.DIG_SPEED, 240, 120));
    public static final Item ECHO_MORPH_AXE = register("echo_morph_axe", new MorphOnlyToolItem(properties("echo_morph_axe").stacksTo(1).durability(768), MobEffects.DAMAGE_BOOST, 160, 100));
    public static final Item ECHO_MORPH_SHOVEL = register("echo_morph_shovel", new MorphOnlyToolItem(properties("echo_morph_shovel").stacksTo(1).durability(768), MobEffects.MOVEMENT_SPEED, 180, 100));

    public static final Item ECHO_FORGE_ITEM = register("echo_forge", new BlockItem(ECHO_FORGE_BLOCK, properties("echo_forge")));
    public static final Item MORPH_ARMOR = register("morph_armor", new MorphArmorItem(properties("morph_armor")));
    public static final Item MORPH_ARMOR_FORGE_ITEM = register("morph_armor_forge", new BlockItem(MORPH_ARMOR_FORGE_BLOCK, properties("morph_armor_forge")));
    public static final Item NATURAL_PORTAL_FRAME_ITEM = register("natural_portal_frame", new BlockItem(NATURAL_PORTAL_FRAME_BLOCK, properties("natural_portal_frame")));
    public static final Item ECHO_BLOCK_ITEM = register("echo_block", new BlockItem(ECHO_BLOCK_BLOCK, properties("echo_block")));
    public static final Item MORPH_BEACON_ITEM = register("morph_beacon", new BlockItem(MORPH_BEACON_BLOCK, properties("morph_beacon").rarity(Rarity.RARE)));

    public static final CreativeModeTab NATURALIS_TAB = net.minecraft.core.Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "naturalis"),
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.naturalis.main"))
            .icon(() -> new ItemStack(ECHO_COLLECTOR))
            .displayItems((parameters, output) -> {
                output.accept(ECHO_COLLECTOR);
                output.accept(EMPTY_ECHO_VIAL);
                output.accept(FILLED_ECHO_VIAL);
                output.accept(MORPH_ORB);
                output.accept(BREWED_MORPH_POTION);
                output.accept(BREWED_MORPH_SPLASH_POTION);
                output.accept(BREWED_MORPH_LINGERING_POTION);
                output.accept(MORPH_BINDING_POTION);
                output.accept(MORPH_BINDING_SPLASH_POTION);
                output.accept(MORPH_BINDING_LINGERING_POTION);
                output.accept(MEMORY_TOKEN);
                output.accept(HUMANITY_TOKEN_5);
                output.accept(HUMANITY_TOKEN_10);
                output.accept(REHUMANIZER);
                output.accept(TRANSLATION_CORE);
                output.accept(KNOWLEDGE_RESET_TOTEM);
                output.accept(APEX_ELIXIR);
                output.accept(GROWTH_SEED);
                output.accept(NATURAL_SIGIL_KEY);
                output.accept(HUMAN_AMULET);
                output.accept(NATURAL_STAR);
                output.accept(SOVEREIGN_AMULET);
                output.accept(HABIT_CHRYSALIS);
                output.accept(ECHO_MORPH_BLADE);
                output.accept(ECHO_MORPH_PICK);
                output.accept(ECHO_MORPH_AXE);
                output.accept(ECHO_MORPH_SHOVEL);
                output.accept(ECHO_FORGE_ITEM);
                output.accept(MORPH_ARMOR);
                output.accept(MORPH_ARMOR_FORGE_ITEM);
                output.accept(NATURAL_PORTAL_FRAME_ITEM);
                output.accept(ECHO_BLOCK_ITEM);
                output.accept(MORPH_BEACON_ITEM);
            })
            .build());

    private FabricNaturalisItems() {
    }

    public static void register() {
        // Class-load side effects register items and creative tab.
    }

    private static Item register(String id, Item item) {
        return Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, id),
            item
        );
    }

    private static Block registerBlock(String id, Block block) {
        return Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, id),
            block
        );
    }

    private static Item.Properties properties(String id) {
        return new Item.Properties();
    }

    private static BlockBehaviour.Properties blockProperties(String id) {
        return BlockBehaviour.Properties.of();
    }
}
