package dev.naturalis.client;

import dev.naturalis.Naturalis;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphQuadrupedMouthClientEvents {

    private MorphQuadrupedMouthClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        MorphQuadrupedShapeCache.rebuild(Minecraft.getInstance());
    }
}
