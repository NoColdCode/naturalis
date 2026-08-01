package dev.naturalis.client;

import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.jetbrains.annotations.Nullable;
import dev.tocraft.walkers.api.PlayerShape;

/**
 * Resolves the active morph's entity texture for first-person paw rendering.
 * Does not touch entity renderers (their {@code extractRenderState} requires a live camera).
 */
public final class MorphPawDigTextureUtil {

    private MorphPawDigTextureUtil() {
    }

    @Nullable
    public static ResourceLocation resolve(AbstractClientPlayer player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape instanceof Wolf wolf) {
            if (wolf.isTame()) {
                return ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_tame.png");
            }
            return ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf.png");
        }
        if (shape instanceof Fox fox) {
            if (fox.getVariant() == Fox.Variant.RED) {
                return ResourceLocation.withDefaultNamespace("textures/entity/fox/fox.png");
            }
            return ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox.png");
        }
        if (shape instanceof Cat) {
            return ResourceLocation.withDefaultNamespace("textures/entity/cat/cat.png");
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId != null) {
            return ResourceLocation.withDefaultNamespace("textures/entity/" + morphId.getPath() + "/" + morphId.getPath() + ".png");
        }
        return null;
    }
}
