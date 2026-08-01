package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphSniffClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Clears client glow flags when scent vision ends. Entity color comes from {@link NaturalisScentEntityTintMixin}.
 */
@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class ScentVisionEntityHighlight {

    private static final Set<Integer> GLOWING_ENTITIES = new HashSet<>();

    private ScentVisionEntityHighlight() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (MorphSniffClientState.isScentVisionActive()) {
            return;
        }
        clearGlowMarks(Minecraft.getInstance());
    }

    static void trackGlow(int entityId) {
        GLOWING_ENTITIES.add(entityId);
    }

    private static void removeStaleGlow(Minecraft mc, Set<Integer> keep) {
        if (mc.level == null) {
            return;
        }
        for (int id : GLOWING_ENTITIES) {
            if (keep.contains(id)) {
                continue;
            }
            Entity entity = mc.level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                living.setGlowingTag(false);
            }
        }
    }

    private static void clearGlowMarks(Minecraft mc) {
        removeStaleGlow(mc, Set.of());
        GLOWING_ENTITIES.clear();
    }
}
