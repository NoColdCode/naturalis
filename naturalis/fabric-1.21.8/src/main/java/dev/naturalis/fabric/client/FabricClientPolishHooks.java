package dev.naturalis.fabric.client;

import dev.naturalis.client.HumanityHudLogic;
import dev.naturalis.client.MorphQuickSlotOverlay;
import dev.naturalis.client.PotionTooltipLogic;
import dev.naturalis.client.perception.MorphMusicPerceptionClient;
import dev.naturalis.fabric.client.quickslot.FabricMorphQuickSlotClient;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import dev.naturalis.network.MorphQuickSlotAssignPayload;
import dev.naturalis.network.MorphQuickSlotResyncPayload;
import dev.naturalis.network.MorphQuickSlotSelectPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Fabric client polish: humanity HUD, potion tooltips, morph music, quick-slot overlay.
 * Morph FOV deferred (no Fabric ViewportEvent; {@code MorphFovLogic} ready for a GameRenderer mixin).
 * Remorphed trait-guide NeoForge events skipped (Remorphed API differs on Fabric).
 */
public final class FabricClientPolishHooks {

    private static boolean quickSlotNetworkingReady;

    private FabricClientPolishHooks() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            HumanityHudLogic.render(graphics);
            MorphQuickSlotOverlay.render(graphics, 0.0F);
        });

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->
            PotionTooltipLogic.appendTooltip(stack, lines));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MorphMusicPerceptionClient.clientTick(client);
            ensureQuickSlotNetworking();
            FabricMorphQuickSlotClient.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(HumanityHudLogic::onLogout));
    }

    private static void ensureQuickSlotNetworking() {
        if (quickSlotNetworkingReady) {
            return;
        }
        quickSlotNetworkingReady = true;
        MorphQuickSlotClientActions.registerAssignSender((slotIndex, morphId) -> {
            if (ClientPlayNetworking.canSend(MorphQuickSlotAssignPayload.TYPE)) {
                ClientPlayNetworking.send(new MorphQuickSlotAssignPayload(slotIndex, morphId));
            }
        });
        MorphQuickSlotClientActions.registerResyncSender(() -> {
            if (ClientPlayNetworking.canSend(MorphQuickSlotResyncPayload.TYPE)) {
                ClientPlayNetworking.send(new MorphQuickSlotResyncPayload());
            }
        });
        MorphQuickSlotClientActions.registerSelectSender((slotIndex, morphId) -> {
            if (ClientPlayNetworking.canSend(MorphQuickSlotSelectPayload.TYPE)) {
                ClientPlayNetworking.send(new MorphQuickSlotSelectPayload(slotIndex, morphId));
            }
        });
        // Remorphed morph sender skipped on Fabric — Remorphed NetworkHandler API differs.
    }
}
