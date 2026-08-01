package dev.naturalis.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ScentVisionFlatColorVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;

    public ScentVisionFlatColorVertexConsumer(VertexConsumer delegate, int tintArgb) {
        this.delegate = delegate;
        this.red = (tintArgb >> 16) & 0xFF;
        this.green = (tintArgb >> 8) & 0xFF;
        this.blue = tintArgb & 0xFF;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        if (a <= 0) {
            return delegate.setColor(0, 0, 0, 0);
        }
        return delegate.setColor(red, green, blue, a);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        return setColor(
            (int) (r * 255.0F),
            (int) (g * 255.0F),
            (int) (b * 255.0F),
            (int) (a * 255.0F)
        );
    }

    @Override
    public VertexConsumer setColor(int packedColor) {
        return setColor(red, green, blue, (packedColor >> 24) & 0xFF);
    }

    @Override
    public VertexConsumer setWhiteAlpha(int alpha) {
        return setColor(red, green, blue, alpha);
    }
}
