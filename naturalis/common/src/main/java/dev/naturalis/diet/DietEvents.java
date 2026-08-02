package dev.naturalis.diet;

import dev.naturalis.NaturalisMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class DietEvents {

    private DietEvents() {
    }

    @SubscribeEvent
    public static void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DietLogic.onFoodFinish(player, event.getItem());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult result = DietLogic.tryRightClickItem(player, event.getItemStack());
        if (result != null) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    public static String debugDiet(net.minecraft.resources.ResourceLocation morphId, net.minecraft.resources.ResourceLocation itemId) {
        return DietLogic.debugDiet(morphId, itemId);
    }
}
