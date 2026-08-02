package dev.naturalis.instinct;

import dev.naturalis.NaturalisMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class InstinctEvents {

    private InstinctEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InstinctLogic.tick(player);
    }

    public static InstinctLogic.DeepSniffResult performDeepSniff(ServerPlayer player) {
        return InstinctLogic.performDeepSniff(player);
    }

    public static void performActiveSniffBurst(ServerPlayer player, int maxTargets) {
        InstinctLogic.performActiveSniffBurst(player, maxTargets);
    }

    public static boolean tryPackCall(ServerPlayer player) {
        return InstinctLogic.tryPackCall(player);
    }

    public static boolean isScentAccessible(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, net.minecraft.server.level.ServerLevel level) {
        return InstinctLogic.isScentAccessible(player, target, level);
    }

    public static byte classifyScentTarget(net.minecraft.resources.ResourceLocation morphId, net.minecraft.world.entity.LivingEntity target) {
        return InstinctLogic.classifyScentTarget(morphId, target);
    }
}
