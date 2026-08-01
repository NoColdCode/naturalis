package dev.naturalis.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum MorphArmorTier {

    LEATHER("leather",   Items.LEATHER,        1.0,  7,  0, 0),
    CHAINMAIL("chainmail", Items.IRON_NUGGET,   2.0, 12,  0, 0),
    IRON("iron",         Items.IRON_INGOT,      1.0, 15,  0, 0),
    GOLD("gold",         Items.GOLD_INGOT,      1.0, 11,  0, 0),
    DIAMOND("diamond",   Items.DIAMOND,         1.5, 20,  8, 0),
    NETHERITE("netherite", Items.NETHERITE_INGOT, 2.0, 20, 12, 1),
    ECHO("echo",         Items.ECHO_SHARD,      2.4, 21, 13, 2);

    /** Tier identifier stored in item NBT. */
    public final String id;
    /** Crafting material consumed in the forge. */
    public final Item material;
    /** Multiplier applied to mass-based cost. */
    public final double costMultiplier;
    /** Total armor points (full set equivalent in one item). */
    public final int armor;
    /** Armor toughness. */
    public final int toughness;
    /** Knockback resistance in tenths of a unit (1 = 0.1). */
    public final int knockbackResistanceTenths;

    MorphArmorTier(String id, Item material, double costMultiplier,
                   int armor, int toughness, int knockbackResistanceTenths) {
        this.id = id;
        this.material = material;
        this.costMultiplier = costMultiplier;
        this.armor = armor;
        this.toughness = toughness;
        this.knockbackResistanceTenths = knockbackResistanceTenths;
    }

    /**
     * Compute the number of material items required for the given mob mass.
     * Minimum is always 4 (lightest mobs like rabbit or bee).
     */
    public int computeCost(double mass) {
        int base = Math.max(4, (int) Math.ceil(mass));
        return (int) Math.ceil(base * costMultiplier);
    }

    public static MorphArmorTier fromId(String id) {
        for (MorphArmorTier t : values()) {
            if (t.id.equals(id)) return t;
        }
        return IRON;
    }

    /** Ordinal-safe index for ContainerData transport. */
    public static MorphArmorTier fromIndex(int index) {
        MorphArmorTier[] values = values();
        if (index < 0 || index >= values.length) return IRON;
        return values[index];
    }
}
