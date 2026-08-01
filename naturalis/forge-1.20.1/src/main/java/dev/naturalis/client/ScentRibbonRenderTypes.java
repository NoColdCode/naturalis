package dev.naturalis.client;

import dev.naturalis.Naturalis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ScentRibbonRenderTypes {

    public static final ResourceLocation TEXTURE = new ResourceLocation(
        Naturalis.MOD_ID,
        "textures/misc/scent_ribbon.png"
    );

    private static final RenderType RIBBON = RenderType.entityTranslucent(TEXTURE);

    private ScentRibbonRenderTypes() {
    }

    public static RenderType ribbon() {
        return RIBBON;
    }

    public static int fullBrightLight() {
        return 0x00F000F0;
    }

    public static int noOverlay() {
        return OverlayTexture.NO_OVERLAY;
    }
}
