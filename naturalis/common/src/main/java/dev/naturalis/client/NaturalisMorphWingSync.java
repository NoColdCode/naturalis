package dev.naturalis.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Syncs morph shape wing / grounded state for mods that use custom ground flags
 * (Aether {@code NotGrounded} / {@code WingedBird}) instead of vanilla {@code onGround}.
 * <p>
 * Walkers only copies vanilla ground state and never ticks shape {@code aiStep}, so Moa/Cockatrice
 * wings freeze in the vertical airborne pose without this.
 */
public final class NaturalisMorphWingSync {

    private static boolean resolved;
    private static Class<?> notGroundedClass;
    private static Class<?> wingedBirdClass;
    private static Method setEntityOnGround;
    private static Method animateWings;

    private NaturalisMorphWingSync() {
    }

    /**
     * @return {@code true} when the morph should use the grounded (folded) wing pose
     */
    public static boolean groundedForWings(Player player) {
        if (player == null) {
            return true;
        }
        if (player.getAbilities().flying) {
            return false;
        }
        return player.onGround();
    }

    public static void syncFromPlayer(Player player, LivingEntity shape) {
        if (player == null || shape == null) {
            return;
        }
        resolve();

        boolean grounded = groundedForWings(player);
        shape.setOnGround(grounded);

        if (notGroundedClass != null && setEntityOnGround != null && notGroundedClass.isInstance(shape)) {
            try {
                setEntityOnGround.invoke(shape, grounded);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    /** Advance flap fields once per client tick (matches Aether {@code aiStep} cadence). */
    public static void tickWingAnimation(LivingEntity shape) {
        if (shape == null) {
            return;
        }
        resolve();
        if (wingedBirdClass == null || animateWings == null || !wingedBirdClass.isInstance(shape)) {
            return;
        }
        try {
            animateWings.invoke(shape);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void syncAndAnimate(Player player, LivingEntity shape) {
        syncFromPlayer(player, shape);
        tickWingAnimation(shape);
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            notGroundedClass = Class.forName("com.aetherteam.aether.entity.NotGrounded");
            setEntityOnGround = notGroundedClass.getMethod("setEntityOnGround", boolean.class);
        } catch (ReflectiveOperationException ignored) {
            notGroundedClass = null;
            setEntityOnGround = null;
        }
        try {
            wingedBirdClass = Class.forName("com.aetherteam.aether.entity.WingedBird");
            animateWings = wingedBirdClass.getMethod("animateWings");
        } catch (ReflectiveOperationException ignored) {
            wingedBirdClass = null;
            animateWings = null;
        }
    }
}
