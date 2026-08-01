package dev.naturalis.fabric;

import dev.naturalis.gameplay.logic.MorphGameplayTickLogic;
import dev.naturalis.worldgen.EchoSovereignRuntime;
import dev.naturalis.worldgen.NaturalDimensionRuntime;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;

public final class FabricNaturalisServerHooks {

    private FabricNaturalisServerHooks() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (NaturalDimensionRuntime.tryActivatePortal(player, world, hitResult.getBlockPos(), player.getItemInHand(hand), hand)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> NaturalDimensionRuntime.onServerTick(server));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                List<ServerPlayer> players = List.copyOf(level.players());
                for (ServerPlayer player : players) {
                    MorphGameplayTickLogic.tick(player);
                }
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!world.isClientSide() && world instanceof ServerLevel serverLevel && chunk instanceof LevelChunk levelChunk) {
                NaturalDimensionRuntime.onOverworldChunkLoaded(serverLevel, levelChunk);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity.level() instanceof ServerLevel)) {
                return true;
            }
            float d = EchoSovereignRuntime.modifyIncomingBossDamage(entity, source, amount);
            if (d <= 0.0F && amount > 0.0F && entity instanceof Mob mob && EchoSovereignRuntime.isEchoSovereign(mob)) {
                return false;
            }
            EchoSovereignRuntime.prepareBossPhaseMeterAfterClamp(entity, source, d);
            return true;
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
            EchoSovereignRuntime.onBossDamagedAfterApplied(entity, source, damageTaken));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
                return;
            }
            var extras = new ArrayList<ItemEntity>();
            NaturalDimensionRuntime.modifyMobLoot(entity, damageSource, extras);
            for (ItemEntity drop : extras) {
                level.addFreshEntity(drop);
            }
        });
    }
}
