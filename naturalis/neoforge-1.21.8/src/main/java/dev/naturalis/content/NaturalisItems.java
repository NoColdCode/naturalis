package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.item.BrewedMorphPotionItem;
import dev.naturalis.item.FixedNameLingeringPotionItem;
import dev.naturalis.item.FixedNamePotionItem;
import dev.naturalis.item.FixedNameSplashPotionItem;
import dev.naturalis.item.FilledEchoVialItem;
import dev.naturalis.item.HabitChrysalisItem;
import dev.naturalis.item.KnowledgeElixirItem;
import dev.naturalis.item.KnowledgeSeedItem;
import dev.naturalis.item.MorphOnlyToolItem;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphOrbItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisItems {

    private NaturalisItems() {
    }

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Naturalis.MOD_ID);

    public static final DeferredItem<Item> ECHO_COLLECTOR = ITEMS.registerSimpleItem("echo_collector",
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> EMPTY_ECHO_VIAL = ITEMS.registerSimpleItem("empty_echo_vial",
        new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> FILLED_ECHO_VIAL = ITEMS.registerItem("filled_echo_vial",
        FilledEchoVialItem::new,
        new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> MORPH_ORB = ITEMS.registerItem("morph_orb",
        MorphOrbItem::new,
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> BREWED_MORPH_POTION = ITEMS.registerItem("brewed_morph_potion",
        BrewedMorphPotionItem::new,
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> BREWED_MORPH_SPLASH_POTION = ITEMS.registerItem("brewed_morph_splash_potion",
        properties -> new FixedNameSplashPotionItem(properties, "item.naturalis.brewed_morph_splash_potion"),
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> BREWED_MORPH_LINGERING_POTION = ITEMS.registerItem("brewed_morph_lingering_potion",
        properties -> new FixedNameLingeringPotionItem(properties, "item.naturalis.brewed_morph_lingering_potion"),
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> MORPH_BINDING_POTION = ITEMS.registerItem("morph_binding_potion",
        properties -> new FixedNamePotionItem(properties, "item.naturalis.morph_binding_potion"),
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> MORPH_BINDING_SPLASH_POTION = ITEMS.registerItem("morph_binding_splash_potion",
        properties -> new FixedNameSplashPotionItem(properties, "item.naturalis.morph_binding_splash_potion"),
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> MORPH_BINDING_LINGERING_POTION = ITEMS.registerItem("morph_binding_lingering_potion",
        properties -> new FixedNameLingeringPotionItem(properties, "item.naturalis.morph_binding_lingering_potion"),
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> MEMORY_TOKEN = ITEMS.registerSimpleItem("memory_token",
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> HUMANITY_TOKEN_5 = ITEMS.registerSimpleItem("humanity_token_5",
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> HUMANITY_TOKEN_10 = ITEMS.registerSimpleItem("humanity_token_10",
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> REHUMANIZER = ITEMS.registerSimpleItem("rehumanizer",
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> TRANSLATION_CORE = ITEMS.registerSimpleItem("translation_core",
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> KNOWLEDGE_RESET_TOTEM = ITEMS.registerItem("knowledge_reset_totem",
        Item::new,
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> APEX_ELIXIR = ITEMS.registerItem("apex_elixir",
        KnowledgeElixirItem::new,
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> GROWTH_SEED = ITEMS.registerItem("growth_seed",
        KnowledgeSeedItem::new,
        new Item.Properties().stacksTo(32));

    public static final DeferredItem<Item> NATURAL_SIGIL_KEY = ITEMS.registerSimpleItem("natural_sigil_key",
        new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> HUMAN_AMULET = ITEMS.registerSimpleItem("human_amulet",
        new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> NATURAL_STAR = ITEMS.registerSimpleItem("natural_star",
        new Item.Properties().stacksTo(64).rarity(Rarity.RARE).fireResistant());

    public static final DeferredItem<Item> SOVEREIGN_AMULET = ITEMS.registerSimpleItem("sovereign_amulet",
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<Item> HABIT_CHRYSALIS = ITEMS.registerItem("habit_chrysalis",
        HabitChrysalisItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<Item> ECHO_MORPH_BLADE = ITEMS.registerItem("echo_morph_blade",
        properties -> new MorphOnlyToolItem(properties, MobEffects.STRENGTH, 200, 120),
        new Item.Properties().stacksTo(1).durability(768));

    public static final DeferredItem<Item> ECHO_MORPH_PICK = ITEMS.registerItem("echo_morph_pick",
        properties -> new MorphOnlyToolItem(properties, MobEffects.HASTE, 240, 120),
        new Item.Properties().stacksTo(1).durability(768));

    public static final DeferredItem<Item> ECHO_MORPH_AXE = ITEMS.registerItem("echo_morph_axe",
        properties -> new MorphOnlyToolItem(properties, MobEffects.STRENGTH, 160, 100),
        new Item.Properties().stacksTo(1).durability(768));

    public static final DeferredItem<Item> ECHO_MORPH_SHOVEL = ITEMS.registerItem("echo_morph_shovel",
        properties -> new MorphOnlyToolItem(properties, MobEffects.SPEED, 180, 100),
        new Item.Properties().stacksTo(1).durability(768));

    public static final DeferredItem<BlockItem> ECHO_FORGE_ITEM = ITEMS.registerSimpleBlockItem(NaturalisBlocks.ECHO_FORGE);

    public static final DeferredItem<MorphArmorItem> MORPH_ARMOR = ITEMS.registerItem("morph_armor",
        MorphArmorItem::new,
        new Item.Properties());

    public static final DeferredItem<BlockItem> MORPH_ARMOR_FORGE_ITEM = ITEMS.registerSimpleBlockItem(NaturalisBlocks.MORPH_ARMOR_FORGE);
    public static final DeferredItem<BlockItem> NATURAL_PORTAL_FRAME_ITEM = ITEMS.registerSimpleBlockItem(NaturalisBlocks.NATURAL_PORTAL_FRAME);
    public static final DeferredItem<BlockItem> ECHO_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(NaturalisBlocks.ECHO_BLOCK);
    public static final DeferredItem<BlockItem> MORPH_BEACON_ITEM = ITEMS.registerSimpleBlockItem(NaturalisBlocks.MORPH_BEACON);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
