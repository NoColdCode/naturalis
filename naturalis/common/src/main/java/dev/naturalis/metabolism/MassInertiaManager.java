package dev.naturalis.metabolism;

import net.minecraft.util.Mth;

/**
 * Maps morph {@linkplain MetabolismManager#getMass mass} to movement feel.
 * Heavier morphs walk slower, fall harder, resist knockback, and jump lower;
 * light morphs are springier with higher jumps and floatier airtime.
 */
public final class MassInertiaManager {

    private MassInertiaManager() {
    }

    public static double getKnockbackResistance(double mass) {
        double resistance = mass / (mass + 3.2D);
        return Mth.clamp(resistance, 0.0D, 0.92D);
    }

    public static double getMovementSpeedMultiplier(double mass) {
        double t = Mth.clamp((mass - 0.45D) / 11.5D, 0.0D, 1.0D);
        return Mth.clamp(1.14D - t * 0.76D, 0.34D, 1.18D);
    }

    public static double getStepHeightMultiplier(double mass) {
        if (mass >= 9.0D) {
            return 0.78D;
        }
        if (mass > 6.5D) {
            return 0.84D;
        }
        if (mass > 4.5D) {
            return 0.94D;
        }
        if (mass < 0.85D) {
            return 1.12D;
        }
        return 1.0D;
    }

    public static double getFallDamageMultiplier(double mass) {
        double multiplier = 1.0D + (mass * 0.18D);
        return Mth.clamp(multiplier, 1.0D, 4.0D);
    }

    /**
     * Values &gt; 1 feel “heavier” (faster terminal settling). Light morphs get floatier airtime.
     */
    public static double getGravityMultiplier(double mass) {
        double t = Mth.clamp((mass - 0.35D) / 11.25D, 0.0D, 1.0D);
        return Mth.clamp(0.78D + t * 0.62D, 0.78D, 1.55D);
    }

    /**
     * Jump height scales with mass: light springboard, heavy squat hop.
     * Only partially compensates gravity so heavy bodies still feel grounded.
     */
    public static double getJumpStrengthMultiplier(double mass) {
        double t = Mth.clamp((mass - 0.40D) / 11.5D, 0.0D, 1.0D);
        // mass 0.4 → ~1.28, mass 2.5 → ~1.05, mass 6 → ~0.82, mass 12 → ~0.58
        return Mth.clamp(1.28D - t * 0.72D, 0.52D, 1.35D);
    }

    /**
     * Extra downward velocity per tick while airborne when GRAVITY attribute is unavailable.
     */
    public static double getSyntheticGravityTickPull(double mass) {
        double g = getGravityMultiplier(mass);
        return (g - 1.0D) * 0.055D;
    }

    /**
     * Multiplier applied to upward velocity right after a jump (primary jump-height path).
     */
    public static double getSyntheticJumpMotionMultiplier(double mass) {
        return getJumpStrengthMultiplier(mass);
    }
}
