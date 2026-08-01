package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphCombatFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphCombatClientEvents {

    private MorphCombatClientEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (!event.getEntity().level().isClientSide()) {
            return;
        }
        MorphCombatFeedback.onAttackEntity(player, event.getTarget());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MorphCombatFeedback.tickCooldown();
    }
}
