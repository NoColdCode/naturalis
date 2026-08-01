package dev.naturalis.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ForgeScentVisionFlatColorVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;

    public ForgeScentVisionFlatColorVertexConsumer(VertexConsumer delegate, int tintArgb) {
        this.delegate = delegate;
        this.red = (tintArgb >> 16) & 0xFF;
        this.green = (tintArgb >> 8) & 0xFF;
        this.blue = tintArgb & 0xFF;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        if (a <= 0) {
            return delegate.color(0, 0, 0, 0);
        }
        return delegate.color(red, green, blue, a);
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return delegate.uv(u, v);
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return delegate.overlayCoords(u, v);
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return delegate.uv2(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        delegate.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
