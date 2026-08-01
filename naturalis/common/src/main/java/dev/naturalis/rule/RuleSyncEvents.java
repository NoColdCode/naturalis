package dev.naturalis.rule;

import dev.naturalis.NaturalisMod;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.network.RuleFlagsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import dev.naturalis.network.PlayToClientSender;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class RuleSyncEvents {

    private RuleSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        PlayToClientSender.send(
            player,
            new RuleFlagsPayload(
                NaturalisConfig.isColorFilterEnabled(player.level()),
                NaturalisConfig.isInventoryRestrictionEnabled(player.level()),
                NaturalisConfig.isInstinctsEnabled(player.level())
            )
        );
    }
}
