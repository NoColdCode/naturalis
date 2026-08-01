package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphIdentityDriftClient;
import dev.naturalis.client.perception.MorphVibrationClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphPerceptionClientEvents {

    private MorphPerceptionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MorphIdentityDriftClient.clientTick(mc);
        MorphVibrationClient.clientTick(mc);
    }
}
