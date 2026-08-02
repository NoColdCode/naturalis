package dev.naturalis.resonance;

import dev.naturalis.NaturalisMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class ResonanceEvents {

    private ResonanceEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResonanceLogic.tick(player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            event.setNewDamage(ResonanceLogic.modifyOutgoingDamage(player, event.getEntity(), event.getNewDamage()));
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        ResonanceLogic.onLivingDeath(event.getEntity(), event.getSource());
    }

    public static void onBondSet(ServerPlayer player) {
        ResonanceLogic.onBondSet(player);
    }

    public static ResonanceLogic.ActiveInstinctResult triggerActiveInstinct(ServerPlayer player) {
        return ResonanceLogic.triggerActiveInstinct(player);
    }

    public static void applyHumanityActionLoss(ServerPlayer player, int amount) {
        ResonanceLogic.applyHumanityActionLoss(player, amount);
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResonanceLogic.onFoodFinished(player, event.getItem());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult result = ResonanceLogic.tryRightClickItem(player, event.getItemStack());
        if (result != null) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) {
            return;
        }
        InteractionResult result = ResonanceLogic.tryEntityInteract(helper, event.getTarget(), event.getItemStack());
        if (result != null) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setNewDamage(ResonanceLogic.modifyIncomingDamage(player, event.getNewDamage()));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult result = ResonanceLogic.tryRightClickBlock(player, event.getPos(), event.getItemStack());
        if (result != null) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (ResonanceLogic.shouldCancelLeftClickBlock(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (ResonanceLogic.shouldCancelAttack(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Float speed = ResonanceLogic.modifyBreakSpeed(player, event.getNewSpeed());
        if (speed != null) {
            event.setNewSpeed(speed);
        }
    }

    public static ResonanceLogic.RebirthResult triggerHumanRebirth(ServerPlayer player) {
        return ResonanceLogic.triggerHumanRebirth(player);
    }

    public static boolean tryTriggerRebirthFromCurlKey(ServerPlayer player) {
        return ResonanceLogic.tryTriggerRebirthFromCurlKey(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResonanceLogic.onPlayerRespawn(player);
        }
    }
}
