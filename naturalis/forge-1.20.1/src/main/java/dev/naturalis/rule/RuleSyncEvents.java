package dev.naturalis.rule;

import dev.naturalis.Naturalis;
import dev.naturalis.network.NaturalisNetwork;
import dev.naturalis.network.RuleFlagsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class RuleSyncEvents {

    private RuleSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        NaturalisNetwork.sendToPlayer(
            player,
            new RuleFlagsPayload(
                NaturalisGameRules.isColorFilterEnabled(player.level()),
                NaturalisGameRules.isInventoryRestrictionEnabled(player.level()),
                NaturalisGameRules.isInstinctsEnabled(player.level())
            )
        );
    }
}
