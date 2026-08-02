package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class PotionTooltipClientEvents {

    private PotionTooltipClientEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        PotionTooltipLogic.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
