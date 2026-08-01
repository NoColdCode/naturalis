package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import dev.naturalis.network.MorphQuickSlotAssignPayload;
import dev.naturalis.network.MorphQuickSlotResyncPayload;
import dev.naturalis.network.MorphQuickSlotSelectPayload;
import dev.naturalis.network.MorphQuickSlotSessionPayload;
import dev.naturalis.network.NaturalisNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tocraft.remorphed.network.NetworkHandler;
import tocraft.walkers.api.variant.ShapeType;

@Mod.EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphQuickSlotClientEvents {

    private static boolean networkingReady;

    private MorphQuickSlotClientEvents() {
    }

    private static void registerNetworking() {
        MorphQuickSlotClientActions.registerAssignSender((slotIndex, morphId) ->
            NaturalisNetwork.CHANNEL.sendToServer(new MorphQuickSlotAssignPayload(slotIndex, morphId)));
        MorphQuickSlotClientActions.registerResyncSender(() ->
            NaturalisNetwork.CHANNEL.sendToServer(new MorphQuickSlotResyncPayload()));
        MorphQuickSlotClientActions.registerSelectSender((slotIndex, morphId) ->
            NaturalisNetwork.CHANNEL.sendToServer(new MorphQuickSlotSelectPayload(slotIndex, morphId)));
        MorphQuickSlotClientActions.registerRemorphedMorphSender(MorphQuickSlotClientEvents::sendRemorphedMorph);
        MorphQuickSlotClientActions.registerSessionSender(
            () -> sendSession(true),
            () -> sendSession(false)
        );
    }

    private static void sendSession(boolean active) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        NaturalisNetwork.CHANNEL.sendToServer(new MorphQuickSlotSessionPayload(active));
    }

    @SuppressWarnings("unchecked")
    private static void sendRemorphedMorph(ResourceLocation morphId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        if (type == null) {
            return;
        }
        ShapeType<? extends LivingEntity> shapeType = ShapeType.from((EntityType<? extends LivingEntity>) type);
        if (shapeType != null) {
            NetworkHandler.sendSwap2ndShapeRequest(shapeType);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (!networkingReady) {
            registerNetworking();
            networkingReady = true;
        }
        MorphQuickSlotClient.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        MorphQuickSlotOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
