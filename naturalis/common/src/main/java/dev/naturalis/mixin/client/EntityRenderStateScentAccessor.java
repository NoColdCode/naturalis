package dev.naturalis.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateScentAccessor implements dev.naturalis.client.EntityRenderStateScentAccess {

    @Unique
    private int naturalis$scentEntityId = -1;

    @Unique
    private int naturalis$scentTintArgb = -1;

    @Override
    public void naturalis$setScentEntityId(int entityId) {
        naturalis$scentEntityId = entityId;
    }

    @Override
    public int naturalis$getScentEntityId() {
        return naturalis$scentEntityId;
    }

    @Override
    public void naturalis$setScentTintArgb(int tintArgb) {
        naturalis$scentTintArgb = tintArgb;
    }

    @Override
    public int naturalis$getScentTintArgb() {
        return naturalis$scentTintArgb;
    }
}
