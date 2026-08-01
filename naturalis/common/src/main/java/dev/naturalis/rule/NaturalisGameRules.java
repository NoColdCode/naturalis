package dev.naturalis.rule;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class NaturalisGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE_COLOR_FILTER = CompatAccess.registerBooleanGameRule(
        "naturalisEnableColorFilter",
        GameRules.Category.PLAYER,
        true
    );

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE_INVENTORY_RESTRICTION = CompatAccess.registerBooleanGameRule(
        "naturalisEnableInventoryRestriction",
        GameRules.Category.PLAYER,
        true
    );

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE_INSTINCTS = CompatAccess.registerBooleanGameRule(
        "naturalisEnableInstincts",
        GameRules.Category.PLAYER,
        true
    );

    private NaturalisGameRules() {
    }

    public static void init() {
        // No-op. Calling this ensures class loading and gamerule registration.
    }

    public static boolean isColorFilterEnabled(Level level) {
        return CompatAccess.getGameRuleBoolean(level, ENABLE_COLOR_FILTER, true);
    }

    public static boolean isInventoryRestrictionEnabled(Level level) {
        return CompatAccess.getGameRuleBoolean(level, ENABLE_INVENTORY_RESTRICTION, true);
    }

    public static boolean isInstinctsEnabled(Level level) {
        return CompatAccess.getGameRuleBoolean(level, ENABLE_INSTINCTS, true);
    }
}