package dev.naturalis.mixin;

import dev.naturalis.worldgen.NaturalBiomeSuitability;
import dev.naturalis.worldgen.NaturalDimensionKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = {
    "tocraft.remorphed.screen.widget.EntityWidget",
    "dev.tocraft.remorphed.screen.widget.EntityWidget"
}, remap = false)
public abstract class NaturalBiomeSuitabilityIconMixin {

    @Inject(method = "renderShape", at = @At("RETURN"))
    private void naturalis$renderSuitabilityIcon(GuiGraphics guiGraphics, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!minecraft.level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }

        LivingEntity morph = naturalis$resolveEntity();
        if (morph == null) {
            return;
        }

        ResourceLocation morphId = BuiltInRegistries.ENTITY_TYPE.getKey(morph.getType());
        NaturalBiomeSuitability.Suitability suitability = NaturalBiomeSuitability.evaluate(
            morphId,
            minecraft.level.getBiome(minecraft.player.blockPosition())
        );
        if (suitability == NaturalBiomeSuitability.Suitability.ADAPTED) {
            return;
        }

        AbstractButton widget = (AbstractButton) (Object) this;
        int x = widget.getX() + widget.getWidth() - 10;
        int y = widget.getY() + 1;

        int bgColor;
        int textColor;
        String icon;
        switch (suitability) {
            case HARSH -> {
                bgColor = 0xCC9E8B2A;
                textColor = 0xFFF8E38E;
                icon = "W";
            }
            case HOSTILE -> {
                bgColor = 0xCC6E366B;
                textColor = 0xFFFFA4F2;
                icon = "P";
            }
            case FORBIDDEN -> {
                bgColor = 0xCC8A2626;
                textColor = 0xFFFF9D9D;
                icon = "L";
            }
            default -> {
                return;
            }
        }

        guiGraphics.fill(x - 1, y - 1, x + 9, y + 9, 0xAA05070B);
        guiGraphics.fill(x, y, x + 8, y + 8, bgColor);
        guiGraphics.drawString(Minecraft.getInstance().font, icon, x + 2, y + 1, textColor, false);
    }

    @Unique
    private LivingEntity naturalis$resolveEntity() {
        try {
            Field field = this.getClass().getDeclaredField("entity");
            field.setAccessible(true);
            Object value = field.get(this);
            if (value instanceof LivingEntity living) {
                return living;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
