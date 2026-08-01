package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import tocraft.walkers.api.PlayerShape;

/**
 * Keeps Aether (and similar) morph wing animations advancing while Walkers shapes are not AI-ticked.
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class MorphWingSyncClientEvents {

    private MorphWingSyncClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        for (AbstractClientPlayer player : mc.level.players()) {
            // Local player first; skip remote shapes unless within a short view distance.
            if (player != mc.player && player.distanceToSqr(mc.player) > 48.0D * 48.0D) {
                continue;
            }
            LivingEntity shape = PlayerShape.getCurrentShape(player);
            if (shape == null) {
                continue;
            }
            NaturalisMorphWingSync.syncAndAnimate(player, shape);
        }
    }
}
