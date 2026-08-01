package dev.naturalis.client;

import tocraft.walkers.api.model.EntityUpdater;
import tocraft.walkers.api.model.EntityUpdaters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;

/**
 * Syncs the local player onto the Walkers shape entity before first-person shape rendering.
 */
public final class NaturalisShapeRenderSync {

    private NaturalisShapeRenderSync() {
    }

    @SuppressWarnings("unchecked")
    public static void syncShapeFromPlayer(AbstractClientPlayer player, LivingEntity shape) {
        shape.walkAnimation.setSpeed(player.walkAnimation.speed());
        shape.swinging = player.swinging;
        shape.swingTime = player.swingTime;
        shape.oAttackAnim = player.oAttackAnim;
        shape.attackAnim = player.attackAnim;
        shape.yBodyRot = player.yBodyRot;
        shape.yBodyRotO = player.yBodyRotO;
        shape.yHeadRot = player.yHeadRot;
        shape.yHeadRotO = player.yHeadRotO;
        shape.tickCount = player.tickCount;
        shape.swingingArm = player.swingingArm;
        shape.tickCount = player.tickCount;
        // Keep aerial morphs "airborne" while the player is flying so wing flaps animate.
        NaturalisMorphWingSync.syncFromPlayer(player, shape);
        shape.setDeltaMovement(player.getDeltaMovement());
        shape.setXRot(player.getXRot());
        shape.xRotO = player.xRotO;
        shape.setPose(player.getPose());
        if (!NaturalisMorphWingSync.groundedForWings(player)) {
            shape.setNoGravity(true);
        }

        if (MorphQuadrupedMouthRender.shouldShowMouthCarry(player, Minecraft.getInstance())) {
            shape.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            shape.setItemSlot(EquipmentSlot.OFFHAND, net.minecraft.world.item.ItemStack.EMPTY);
        } else {
            shape.setItemSlot(EquipmentSlot.MAINHAND, player.getItemBySlot(EquipmentSlot.MAINHAND));
            shape.setItemSlot(EquipmentSlot.OFFHAND, player.getItemBySlot(EquipmentSlot.OFFHAND));
        }
        shape.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        shape.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST));
        shape.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS));
        shape.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET));

        if (shape instanceof Mob mob) {
            mob.setAggressive(player.isUsingItem());
        }

        InteractionHand used = player.getUsedItemHand() == null ? InteractionHand.MAIN_HAND : player.getUsedItemHand();
        shape.startUsingItem(used);
        shape.hurtTime = player.hurtTime;

        EntityUpdater<LivingEntity> updater = EntityUpdaters.getUpdater((EntityType<LivingEntity>) shape.getType());
        if (updater != null) {
            updater.update(player, shape);
        }
    }
}
