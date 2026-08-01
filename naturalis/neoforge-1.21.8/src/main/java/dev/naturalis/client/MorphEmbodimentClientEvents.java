package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphCameraLogic;
import dev.naturalis.client.perception.MorphSniffClientState;
import dev.naturalis.client.perception.MorphListenClientState;
import dev.naturalis.client.perception.MorphPeckClientState;
import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphEmbodimentProfile;
import dev.naturalis.client.perception.MorphEmbodimentProfiles;
import dev.naturalis.client.perception.MorphEmbodimentDriftEffects;
import dev.naturalis.client.perception.MorphPerceptionScaling;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphEmbodimentClientEvents {

    private MorphEmbodimentClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        MorphEmbodimentProfile profile = MorphEmbodimentLogic.activeProfile(Minecraft.getInstance());
        if (!profile.hasEmbodiment()) {
            return;
        }
        MorphEmbodimentProfile scaled = profile.withFovMultiplier(MorphPerceptionScaling.fovMultiplier(profile));
        float fov = (float) MorphEmbodimentProfiles.clampFov(event.getFOV(), scaled);
        fov += MorphEmbodimentDriftEffects.fovBreathOffset();
        fov += MorphSniffClientState.fovOffsetDegrees();
        fov += MorphListenClientState.fovOffsetDegrees();
        event.setFOV(fov);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(mc.player);
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        MorphSniffClientState.tick();
        MorphListenClientState.tick();
        MorphPeckClientState.tick();
        MorphCameraLogic.enforcePlayerView(mc.player, profile, morphId);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameRenderer == null) {
            return;
        }
        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(mc.player);
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        MorphCameraLogic.applyCameraOffsets(
            mc.gameRenderer.getMainCamera(),
            mc.player,
            profile,
            morphId,
            partialTick
        );
    }
}
