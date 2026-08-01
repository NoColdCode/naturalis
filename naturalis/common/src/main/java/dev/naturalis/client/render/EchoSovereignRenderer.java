package dev.naturalis.client.render;

import dev.naturalis.entity.EchoSovereignEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;

/** Vanilla Evoker model for {@link EchoSovereignEntity}. */
public class EchoSovereignRenderer extends EvokerRenderer {

    public EchoSovereignRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
