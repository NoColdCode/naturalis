package dev.naturalis.fabric;

import dev.naturalis.diet.DietLogic;
import dev.naturalis.environment.EnvironmentalSusceptibilityLogic;
import dev.naturalis.gameplay.KnowledgeLevelLogic;
import dev.naturalis.instinct.InstinctLogic;
import dev.naturalis.metabolism.MetabolismLogic;
import dev.naturalis.resonance.ResonanceLogic;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * Fabric 1.21.8 wiring for extracted loader-neutral gameplay Logic classes.
 * UseItemCallback returns {@link InteractionResult} on this game version.
 */
public final class FabricGameplaySystemsHooks {

    private FabricGameplaySystemsHooks() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                KnowledgeLevelLogic.tick(player);
                InstinctLogic.tick(player);
                MetabolismLogic.tick(player);
                EnvironmentalSusceptibilityLogic.tick(player);
                ResonanceLogic.tick(player);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            var stack = player.getItemInHand(hand);
            InteractionResult diet = DietLogic.tryRightClickItem(serverPlayer, stack);
            if (diet != null) {
                return diet;
            }
            InteractionResult resonance = ResonanceLogic.tryRightClickItem(serverPlayer, stack);
            return resonance != null ? resonance : InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            InteractionResult result = ResonanceLogic.tryRightClickBlock(
                serverPlayer, hitResult.getBlockPos(), player.getItemInHand(hand));
            return result != null ? result : InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            InteractionResult result = ResonanceLogic.tryEntityInteract(
                serverPlayer, entity, player.getItemInHand(hand));
            return result != null ? result : InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (ResonanceLogic.shouldCancelLeftClickBlock(serverPlayer, player.getItemInHand(hand))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (ResonanceLogic.shouldCancelAttack(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
            ResonanceLogic.onLivingDeath(entity, damageSource));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            ResonanceLogic.onPlayerRespawn(newPlayer));
    }
}
