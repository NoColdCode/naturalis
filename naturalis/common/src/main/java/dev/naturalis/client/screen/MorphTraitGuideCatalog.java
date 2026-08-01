package dev.naturalis.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog of Walkers / Naturalis shape traits shown in the Remorphed traits guide
 * and Survival-as permanent nature popup.
 *
 * @see <a href="https://github.com/ToCraft/woodwalkers-mod/wiki/Traits">Walkers Traits wiki</a>
 */
public final class MorphTraitGuideCatalog {

    public record Entry(Item icon, String titleKey, String bodyKey) {
        public Component title() {
            return Component.translatable(titleKey);
        }

        public Component body() {
            return Component.translatable(bodyKey);
        }
    }

    private static final List<Entry> ENTRIES = List.of(
        // Naturalis
        new Entry(Items.SHULKER_SHELL, "gui.naturalis.trait_guide.static.title", "gui.naturalis.trait_guide.static.body"),
        new Entry(Items.LILY_PAD, "gui.naturalis.trait_guide.floating.title", "gui.naturalis.trait_guide.floating.body"),
        new Entry(Items.BONE, "gui.naturalis.trait_guide.scentbound.title", "gui.naturalis.trait_guide.scentbound.body"),
        new Entry(Items.INK_SAC, "gui.naturalis.trait_guide.photophobic.title", "gui.naturalis.trait_guide.photophobic.body"),
        new Entry(Items.LEATHER_BOOTS, "gui.naturalis.trait_guide.wanderer.title", "gui.naturalis.trait_guide.wanderer.body"),
        new Entry(Items.COOKED_BEEF, "gui.naturalis.trait_guide.hunter.title", "gui.naturalis.trait_guide.hunter.body"),
        new Entry(Items.SPYGLASS, "gui.naturalis.trait_guide.nyctalop.title", "gui.naturalis.trait_guide.nyctalop.body"),
        new Entry(Items.SADDLE, "gui.naturalis.trait_guide.quadruped.title", "gui.naturalis.trait_guide.quadruped.body"),
        // Walkers wiki traits
        new Entry(Items.ELYTRA, "gui.naturalis.trait_guide.flying.title", "gui.naturalis.trait_guide.flying.body"),
        new Entry(Items.COD, "gui.naturalis.trait_guide.aquatic.title", "gui.naturalis.trait_guide.aquatic.body"),
        new Entry(Items.LADDER, "gui.naturalis.trait_guide.climb.title", "gui.naturalis.trait_guide.climb.body"),
        new Entry(Items.FEATHER, "gui.naturalis.trait_guide.slow_falling.title", "gui.naturalis.trait_guide.slow_falling.body"),
        new Entry(Items.CLOCK, "gui.naturalis.trait_guide.nocturnal.title", "gui.naturalis.trait_guide.nocturnal.body"),
        new Entry(Items.MAGMA_BLOCK, "gui.naturalis.trait_guide.burn_daylight.title", "gui.naturalis.trait_guide.burn_daylight.body"),
        new Entry(Items.FIRE_CHARGE, "gui.naturalis.trait_guide.temperature.title", "gui.naturalis.trait_guide.temperature.body"),
        new Entry(Items.IRON_BARS, "gui.naturalis.trait_guide.cant_interact.title", "gui.naturalis.trait_guide.cant_interact.body"),
        new Entry(Items.HEART_OF_THE_SEA, "gui.naturalis.trait_guide.undrownable.title", "gui.naturalis.trait_guide.undrownable.body"),
        new Entry(Items.POWDER_SNOW_BUCKET, "gui.naturalis.trait_guide.powder_snow.title", "gui.naturalis.trait_guide.powder_snow.body"),
        new Entry(Items.SNOWBALL, "gui.naturalis.trait_guide.cant_freeze.title", "gui.naturalis.trait_guide.cant_freeze.body"),
        new Entry(Items.WATER_BUCKET, "gui.naturalis.trait_guide.cant_swim.title", "gui.naturalis.trait_guide.cant_swim.body"),
        new Entry(Items.LAVA_BUCKET, "gui.naturalis.trait_guide.stand_on_fluid.title", "gui.naturalis.trait_guide.stand_on_fluid.body"),
        new Entry(Items.ROTTEN_FLESH, "gui.naturalis.trait_guide.attack_for_health.title", "gui.naturalis.trait_guide.attack_for_health.body"),
        new Entry(Items.GLASS, "gui.naturalis.trait_guide.no_physics.title", "gui.naturalis.trait_guide.no_physics.body"),
        new Entry(Items.PLAYER_HEAD, "gui.naturalis.trait_guide.humanoid.title", "gui.naturalis.trait_guide.humanoid.body"),
        new Entry(Items.POTION, "gui.naturalis.trait_guide.mob_effect.title", "gui.naturalis.trait_guide.mob_effect.body"),
        new Entry(Items.MILK_BUCKET, "gui.naturalis.trait_guide.immunity.title", "gui.naturalis.trait_guide.immunity.body"),
        new Entry(Items.LIGHTNING_ROD, "gui.naturalis.trait_guide.instant_die.title", "gui.naturalis.trait_guide.instant_die.body"),
        new Entry(Items.SHIELD, "gui.naturalis.trait_guide.reinforcements.title", "gui.naturalis.trait_guide.reinforcements.body"),
        new Entry(Items.BONE_MEAL, "gui.naturalis.trait_guide.prey.title", "gui.naturalis.trait_guide.prey.body"),
        new Entry(Items.IRON_SWORD, "gui.naturalis.trait_guide.feared.title", "gui.naturalis.trait_guide.feared.body"),
        new Entry(Items.MINECART, "gui.naturalis.trait_guide.rider.title", "gui.naturalis.trait_guide.rider.body"),
        // Survival-as specific
        new Entry(Items.GOLDEN_APPLE, "gui.naturalis.survival_as.trait.undead_healing.title", "gui.naturalis.survival_as.trait.undead_healing.body"),
        new Entry(Items.EMERALD, "gui.naturalis.survival_as.trait.villager_prey.title", "gui.naturalis.survival_as.trait.villager_prey.body"),
        new Entry(Items.WATER_BUCKET, "gui.naturalis.survival_as.trait.water_fear.title", "gui.naturalis.survival_as.trait.water_fear.body"),
        // Future / design ideas (documented for players)
        new Entry(Items.SLIME_BALL, "gui.naturalis.trait_guide.idea_sticky.title", "gui.naturalis.trait_guide.idea_sticky.body"),
        new Entry(Items.GLOW_INK_SAC, "gui.naturalis.trait_guide.idea_bioluminescent.title", "gui.naturalis.trait_guide.idea_bioluminescent.body"),
        new Entry(Items.GOAT_HORN, "gui.naturalis.trait_guide.idea_pack.title", "gui.naturalis.trait_guide.idea_pack.body"),
        new Entry(Items.ECHO_SHARD, "gui.naturalis.trait_guide.idea_echolocation.title", "gui.naturalis.trait_guide.idea_echolocation.body")
    );

    private static final Map<String, Entry> BY_TRAIT_ID = buildTraitIdIndex();

    private MorphTraitGuideCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Optional<Entry> findByTraitId(String traitId) {
        if (traitId == null || traitId.isBlank()) {
            return Optional.empty();
        }
        Entry direct = BY_TRAIT_ID.get(traitId.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return Optional.of(direct);
        }
        String path = traitId.contains(":") ? traitId.substring(traitId.indexOf(':') + 1) : traitId;
        return Optional.ofNullable(BY_TRAIT_ID.get(path.toLowerCase(Locale.ROOT)));
    }

    public static Entry resolve(String traitId) {
        return findByTraitId(traitId).orElseGet(() -> new Entry(
            Items.PAPER,
            fallbackTitleKey(traitId),
            fallbackBodyKey(traitId)
        ));
    }

    private static String fallbackTitleKey(String traitId) {
        String path = traitId.contains(":") ? traitId.substring(traitId.indexOf(':') + 1) : traitId;
        return "gui.naturalis.trait_guide." + path.replace('.', '_') + ".title";
    }

    private static String fallbackBodyKey(String traitId) {
        String path = traitId.contains(":") ? traitId.substring(traitId.indexOf(':') + 1) : traitId;
        return "gui.naturalis.trait_guide." + path.replace('.', '_') + ".body";
    }

    private static Map<String, Entry> buildTraitIdIndex() {
        Map<String, Entry> map = new HashMap<>();
        index(map, "naturalis:static", ENTRIES.get(0));
        index(map, "naturalis:floating", ENTRIES.get(1));
        index(map, "naturalis:scentbound", ENTRIES.get(2));
        index(map, "naturalis:photophobic", ENTRIES.get(3));
        index(map, "naturalis:wanderer", ENTRIES.get(4));
        index(map, "naturalis:hunter", ENTRIES.get(5));
        index(map, "naturalis:nyctalop", ENTRIES.get(6));
        index(map, "naturalis:quadruped", ENTRIES.get(7));
        index(map, "walkers:flying", ENTRIES.get(8));
        index(map, "walkers:aquatic", ENTRIES.get(9));
        index(map, "walkers:climb_blocks", ENTRIES.get(10));
        index(map, "walkers:climb_on_blocks", ENTRIES.get(10));
        index(map, "walkers:slow_falling", ENTRIES.get(11));
        index(map, "walkers:nocturnal", ENTRIES.get(12));
        index(map, "walkers:burn_in_daylight", ENTRIES.get(13));
        index(map, "walkers:temperature", ENTRIES.get(14));
        index(map, "walkers:cant_interact", ENTRIES.get(15));
        index(map, "walkers:undrownable", ENTRIES.get(16));
        index(map, "walkers:walk_on_powder_snow", ENTRIES.get(17));
        index(map, "walkers:cant_freeze", ENTRIES.get(18));
        index(map, "walkers:cant_swim", ENTRIES.get(19));
        index(map, "walkers:stand_on_fluid", ENTRIES.get(20));
        index(map, "walkers:attack_for_health", ENTRIES.get(21));
        index(map, "walkers:no_physics", ENTRIES.get(22));
        index(map, "walkers:humanoid", ENTRIES.get(23));
        index(map, "walkers:mob_effect", ENTRIES.get(24));
        index(map, "walkers:immunity", ENTRIES.get(25));
        index(map, "walkers:instant_die_on_damage_msg", ENTRIES.get(26));
        index(map, "walkers:reinforcements", ENTRIES.get(27));
        index(map, "walkers:prey", ENTRIES.get(28));
        index(map, "walkers:feared", ENTRIES.get(29));
        index(map, "walkers:rider", ENTRIES.get(30));
        index(map, "naturalis:survival_as.undead_healing", ENTRIES.get(31));
        index(map, "naturalis:survival_as.villager_prey", ENTRIES.get(32));
        index(map, "naturalis:survival_as.water_fear", ENTRIES.get(33));
        return map;
    }

    private static void index(Map<String, Entry> map, String traitId, Entry entry) {
        map.put(traitId.toLowerCase(Locale.ROOT), entry);
        if (traitId.contains(":")) {
            map.put(traitId.substring(traitId.indexOf(':') + 1).toLowerCase(Locale.ROOT), entry);
        }
    }
}
