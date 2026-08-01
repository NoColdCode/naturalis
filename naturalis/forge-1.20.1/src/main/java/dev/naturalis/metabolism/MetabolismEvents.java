package dev.naturalis.metabolism;

import dev.naturalis.Naturalis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class MetabolismEvents {

    private static final ResourceLocation ADV_ROOT = new ResourceLocation(Naturalis.MOD_ID, "root");
    private static final ResourceLocation ADV_IMMOVABLE_OBJECT = new ResourceLocation(Naturalis.MOD_ID, "inertia/immovable_object");
    private static final ResourceLocation ADV_FEATHER_STEP = new ResourceLocation(Naturalis.MOD_ID, "inertia/feather_step");

    private static final ResourceLocation INERTIA_SPEED_MODIFIER_ID     = new ResourceLocation(Naturalis.MOD_ID, "inertia_speed");
    private static final ResourceLocation INERTIA_STEP_MODIFIER_ID      = new ResourceLocation(Naturalis.MOD_ID, "inertia_step");
    private static final ResourceLocation INERTIA_KNOCKBACK_MODIFIER_ID = new ResourceLocation(Naturalis.MOD_ID, "inertia_knockback");

    private MetabolismEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            clearInertiaModifiers(player);
            return;
        }

        applyInertiaModifiers(player, morphId);
        applySyntheticMorphAirGravity(player, morphId);

        if (!MorphWalkSpeedManager.canSprint(morphId) && player.isSprinting()) {
            player.setSprinting(false);
        }

        if (MetabolismManager.getMass(morphId) >= 8.0D && player.isSprinting()) {
            grantAdvancement(player, ADV_IMMOVABLE_OBJECT);
        }
    }

    /**
     * Vanilla 1.20 lacks {@code Attributes.GRAVITY} / jump strength — approximate inertia with motion tweaks.
     */
    private static void applySyntheticMorphAirGravity(ServerPlayer player, ResourceLocation morphId) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.getAbilities().flying || player.isFallFlying()) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isInLava()) {
            return;
        }
        if (player.onClimbable()) {
            return;
        }
        double pull = MassInertiaManager.getSyntheticGravityTickPull(MetabolismManager.getMass(morphId));
        if (Math.abs(pull) < 1e-7) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, v.y - pull, v.z);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }
        double mult = MassInertiaManager.getSyntheticJumpMotionMultiplier(MetabolismManager.getMass(morphId))
            * MorphWalkSpeedManager.getJumpGaitMultiplier(morphId);
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, v.y * mult, v.z);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        double mass = MetabolismManager.getMass(morphId);
        float multiplier = (float) MassInertiaManager.getFallDamageMultiplier(mass);
        event.setDamageMultiplier(event.getDamageMultiplier() * multiplier);

        if (mass < 0.5D && event.getDistance() >= 8.0F) {
            grantAdvancement(player, ADV_FEATHER_STEP);
        }
    }

    private static void applyInertiaModifiers(ServerPlayer player, ResourceLocation morphId) {
        double mass = MetabolismManager.getMass(morphId);

        double speedMultiplier     = MassInertiaManager.getMovementSpeedMultiplier(mass)
            * MorphWalkSpeedManager.getGaitMultiplier(morphId);
        double stepMultiplier      = MassInertiaManager.getStepHeightMultiplier(mass);
        double knockbackResistance = MassInertiaManager.getKnockbackResistance(mass);

        upsertModifier(player.getAttribute(Attributes.MOVEMENT_SPEED),
            INERTIA_SPEED_MODIFIER_ID, speedMultiplier - 1.0D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
        upsertModifier(player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()),
            INERTIA_STEP_MODIFIER_ID, stepMultiplier - 1.0D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
        upsertModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
            INERTIA_KNOCKBACK_MODIFIER_ID, knockbackResistance,
            AttributeModifier.Operation.ADDITION);
    }

    private static void clearInertiaModifiers(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED),         INERTIA_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()), INERTIA_STEP_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE),   INERTIA_KNOCKBACK_MODIFIER_ID);
    }

    private static void upsertModifier(AttributeInstance attribute, ResourceLocation id,
                                       double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        UUID uuid = toUUID(id);
        attribute.removeModifier(uuid);
        attribute.addTransientModifier(new AttributeModifier(uuid, id.toString(), amount, operation));
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(toUUID(id));
    }

    private static UUID toUUID(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        Advancement root = player.getServer().getAdvancements().getAdvancement(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }
}
