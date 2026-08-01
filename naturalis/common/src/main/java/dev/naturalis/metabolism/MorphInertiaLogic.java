package dev.naturalis.metabolism;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Loader-neutral morph inertia: mass-based speed / knockback / gravity / jump feel.
 * Jump height is driven by motion scaling on jump (reliable across loaders); gravity uses
 * the GRAVITY attribute when present and a synthetic airborne pull as fallback.
 */
public final class MorphInertiaLogic {

    public static final ResourceLocation INERTIA_SPEED_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("naturalis", "inertia_speed");
    public static final ResourceLocation INERTIA_STEP_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("naturalis", "inertia_step");
    public static final ResourceLocation INERTIA_KNOCKBACK_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("naturalis", "inertia_knockback");
    public static final ResourceLocation INERTIA_GRAVITY_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("naturalis", "inertia_gravity");
    public static final ResourceLocation INERTIA_JUMP_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("naturalis", "inertia_jump");

    private MorphInertiaLogic() {
    }

    public static void tick(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null) {
            clear(player);
            return;
        }
        applyAttributeModifiers(player, morphId);
        applySyntheticAirGravity(player, morphId);

        if (!MorphWalkSpeedManager.canSprint(morphId) && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    public static void onJump(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        double mult = MassInertiaManager.getSyntheticJumpMotionMultiplier(MetabolismManager.getMass(morphId))
            * MorphWalkSpeedManager.getJumpGaitMultiplier(morphId);
        if (Math.abs(mult - 1.0D) < 1.0E-4D) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, v.y * mult, v.z);
    }

    public static void applyAttributeModifiers(ServerPlayer player, ResourceLocation morphId) {
        double mass = MetabolismManager.getMass(morphId);

        double speedMultiplier = MassInertiaManager.getMovementSpeedMultiplier(mass)
            * MorphWalkSpeedManager.getGaitMultiplier(morphId);
        double stepMultiplier = MassInertiaManager.getStepHeightMultiplier(mass);
        double knockbackResistance = MassInertiaManager.getKnockbackResistance(mass);
        double gravityMultiplier = MassInertiaManager.getGravityMultiplier(mass);

        upsert(player.getAttribute(Attributes.MOVEMENT_SPEED), INERTIA_SPEED_MODIFIER_ID,
            speedMultiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        upsert(player.getAttribute(Attributes.STEP_HEIGHT), INERTIA_STEP_MODIFIER_ID,
            stepMultiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        upsert(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), INERTIA_KNOCKBACK_MODIFIER_ID,
            knockbackResistance, AttributeModifier.Operation.ADD_VALUE);

        // Jump height is applied via MorphInertiaLogic.onJump (LivingJumpEvent) so it works
        // even when JUMP_STRENGTH is missing or ignored on the player.
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            upsert(gravity, INERTIA_GRAVITY_MODIFIER_ID, gravityMultiplier - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    public static void applySyntheticAirGravity(ServerPlayer player, ResourceLocation morphId) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.getAbilities().flying || player.isFallFlying()) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            return;
        }
        // When GRAVITY attribute is available and modified, skip synthetic pull to avoid double gravity.
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null && gravity.getModifier(INERTIA_GRAVITY_MODIFIER_ID) != null) {
            return;
        }

        double pull = MassInertiaManager.getSyntheticGravityTickPull(MetabolismManager.getMass(morphId));
        if (Math.abs(pull) < 1.0E-7D) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, v.y - pull, v.z);
    }

    public static void clear(ServerPlayer player) {
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), INERTIA_SPEED_MODIFIER_ID);
        remove(player.getAttribute(Attributes.STEP_HEIGHT), INERTIA_STEP_MODIFIER_ID);
        remove(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), INERTIA_KNOCKBACK_MODIFIER_ID);
        remove(player.getAttribute(Attributes.GRAVITY), INERTIA_GRAVITY_MODIFIER_ID);
        remove(player.getAttribute(Attributes.JUMP_STRENGTH), INERTIA_JUMP_MODIFIER_ID);
    }

    private static void upsert(AttributeInstance attribute, ResourceLocation id, double amount,
                               AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void remove(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
    }
}
