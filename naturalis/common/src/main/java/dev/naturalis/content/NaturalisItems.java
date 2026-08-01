package dev.naturalis.content;

import dev.naturalis.NaturalisMod;
import dev.naturalis.item.BrewedMorphPotionItem;
import dev.naturalis.item.FixedNameLingeringPotionItem;
import dev.naturalis.item.FixedNamePotionItem;
import dev.naturalis.item.FixedNameSplashPotionItem;
import dev.naturalis.item.FilledEchoVialItem;
import dev.naturalis.item.HabitChrysalisItem;
import dev.naturalis.item.MorphOnlyToolItem;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphOrbItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisItems {

    private NaturalisItems() {
    }

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NaturalisMod.ID);

    public static final DeferredItem<Item> ECHO_COLLECTOR = ITEMS.register("echo_collector",
        () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> EMPTY_ECHO_VIAL = ITEMS.register("empty_echo_vial",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> FILLED_ECHO_VIAL = ITEMS.register("filled_echo_vial",
        () -> new FilledEchoVialItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> MORPH_ORB = ITEMS.register("morph_orb",
        () -> new MorphOrbItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BREWED_MORPH_POTION = ITEMS.register("brewed_morph_potion",
        () -> new BrewedMorphPotionItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BREWED_MORPH_SPLASH_POTION = ITEMS.register("brewed_morph_splash_potion",
        () -> new FixedNameSplashPotionItem(new Item.Properties().stacksTo(1), "item.naturalis.brewed_morph_splash_potion"));

    public static final DeferredItem<Item> BREWED_MORPH_LINGERING_POTION = ITEMS.register("brewed_morph_lingering_potion",
        () -> new FixedNameLingeringPotionItem(new Item.Properties().stacksTo(1), "item.naturalis.brewed_morph_lingering_potion"));

    public static final DeferredItem<Item> MORPH_BINDING_POTION = ITEMS.register("morph_binding_potion",
        () -> new FixedNamePotionItem(new Item.Properties().stacksTo(1), "item.naturalis.morph_binding_potion"));

    public static final DeferredItem<Item> MORPH_BINDING_SPLASH_POTION = ITEMS.register("morph_binding_splash_potion",
        () -> new FixedNameSplashPotionItem(new Item.Properties().stacksTo(1), "item.naturalis.morph_binding_splash_potion"));

    public static final DeferredItem<Item> MORPH_BINDING_LINGERING_POTION = ITEMS.register("morph_binding_lingering_potion",
        () -> new FixedNameLingeringPotionItem(new Item.Properties().stacksTo(1), "item.naturalis.morph_binding_lingering_potion"));

    public static final DeferredItem<Item> MEMORY_TOKEN = ITEMS.register("memory_token",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> HUMANITY_TOKEN_5 = ITEMS.register("humanity_token_5",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> HUMANITY_TOKEN_10 = ITEMS.register("humanity_token_10",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> REHUMANIZER = ITEMS.register("rehumanizer",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> TRANSLATION_CORE = ITEMS.register("translation_core",
        () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> KNOWLEDGE_RESET_TOTEM = ITEMS.register("knowledge_reset_totem",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> APEX_ELIXIR = ITEMS.register("apex_elixir",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> GROWTH_SEED = ITEMS.register("growth_seed",
        () -> new Item(new Item.Properties().stacksTo(32)));

    public static final DeferredItem<Item> NATURAL_SIGIL_KEY = ITEMS.register("natural_sigil_key",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> HUMAN_AMULET = ITEMS.register("human_amulet",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NATURAL_STAR = ITEMS.register("natural_star",
        () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<Item> SOVEREIGN_AMULET = ITEMS.register("sovereign_amulet",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<Item> HABIT_CHRYSALIS = ITEMS.register("habit_chrysalis",
        () -> new HabitChrysalisItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<Item> ECHO_MORPH_BLADE = ITEMS.register("echo_morph_blade",
        () -> new MorphOnlyToolItem(new Item.Properties().stacksTo(1).durability(768), MobEffects.DAMAGE_BOOST, 200, 120));

    public static final DeferredItem<Item> ECHO_MORPH_PICK = ITEMS.register("echo_morph_pick",
        () -> new MorphOnlyToolItem(new Item.Properties().stacksTo(1).durability(768), MobEffects.DIG_SPEED, 240, 120));

    public static final DeferredItem<Item> ECHO_MORPH_AXE = ITEMS.register("echo_morph_axe",
        () -> new MorphOnlyToolItem(new Item.Properties().stacksTo(1).durability(768), MobEffects.DAMAGE_BOOST, 160, 100));

    public static final DeferredItem<Item> ECHO_MORPH_SHOVEL = ITEMS.register("echo_morph_shovel",
        () -> new MorphOnlyToolItem(new Item.Properties().stacksTo(1).durability(768), MobEffects.MOVEMENT_SPEED, 180, 100));

    public static final DeferredItem<BlockItem> ECHO_FORGE_ITEM = ITEMS.register("echo_forge",
        () -> new BlockItem(NaturalisBlocks.ECHO_FORGE.get(), new Item.Properties()));

    public static final DeferredItem<MorphArmorItem> MORPH_ARMOR = ITEMS.register("morph_armor",
        () -> new MorphArmorItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> MORPH_ARMOR_FORGE_ITEM = ITEMS.register("morph_armor_forge",
        () -> new BlockItem(NaturalisBlocks.MORPH_ARMOR_FORGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> NATURAL_PORTAL_FRAME_ITEM = ITEMS.register("natural_portal_frame",
        () -> new BlockItem(NaturalisBlocks.NATURAL_PORTAL_FRAME.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ECHO_BLOCK_ITEM = ITEMS.register("echo_block",
        () -> new BlockItem(NaturalisBlocks.ECHO_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MORPH_BEACON_ITEM = ITEMS.register("morph_beacon",
        () -> new BlockItem(NaturalisBlocks.MORPH_BEACON.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
