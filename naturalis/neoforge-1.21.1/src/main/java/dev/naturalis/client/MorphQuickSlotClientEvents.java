package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import dev.naturalis.network.MorphQuickSlotAssignPayload;
import dev.naturalis.network.MorphQuickSlotSelectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import tocraft.remorphed.network.NetworkHandler;
import tocraft.walkers.api.variant.ShapeType;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class MorphQuickSlotClientEvents {

    private static boolean networkingReady;

    private MorphQuickSlotClientEvents() {
    }

    private static void registerNetworking() {
        MorphQuickSlotClientActions.registerAssignSender((slotIndex, morphId) -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new MorphQuickSlotAssignPayload(slotIndex, morphId));
            }
        });
        MorphQuickSlotClientActions.registerResyncSender(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new dev.naturalis.network.MorphQuickSlotResyncPayload());
            }
        });
        MorphQuickSlotClientActions.registerSelectSender((slotIndex, morphId) -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new MorphQuickSlotSelectPayload(slotIndex, morphId));
            }
        });
        MorphQuickSlotClientActions.registerRemorphedMorphSender(MorphQuickSlotClientEvents::sendRemorphedMorph);
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
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (!networkingReady) {
            registerNetworking();
            networkingReady = true;
        }
        MorphQuickSlotClient.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        MorphQuickSlotOverlay.render(event.getGuiGraphics(), ClientPartialTick.get(Minecraft.getInstance()));
    }
}
