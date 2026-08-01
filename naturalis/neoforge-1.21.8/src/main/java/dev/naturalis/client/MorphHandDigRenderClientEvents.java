package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphDigClientState;
import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphPawDigRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Draws both mob front legs in the first-person hand pass while paw-digging.
 */
@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphHandDigRenderClientEvents {

    private MorphHandDigRenderClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        var profile = MorphEmbodimentLogic.profileFor(mc.player);
        if (MorphEmbodimentLogic.shouldRenderFirstPersonMorphBody(profile)) {
            return;
        }

        boolean showPaws = MorphEmbodimentLogic.usesPawDigging(profile)
            && (MorphDigClientState.digAnim() > 0.05F || MorphEmbodimentLogic.isAttackingBlock(mc));
        if (!showPaws) {
            return;
        }

        MorphPawDigRenderer.render(
            event.getPoseStack(),
            event.getMultiBufferSource(),
            event.getPackedLight(),
            mc.player,
            event.getPartialTick(),
            MorphPawDigTextureUtil.resolve(mc.player)
        );
    }
}
