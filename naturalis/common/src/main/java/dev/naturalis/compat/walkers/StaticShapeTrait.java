package dev.naturalis.compat.walkers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import tocraft.walkers.traits.ShapeTrait;

/** Sessile morph — cannot walk (shulker / aechor plant style). */
public final class StaticShapeTrait<E extends LivingEntity> extends ShapeTrait<E> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("naturalis", "static");
    public static final MapCodec<StaticShapeTrait<?>> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.stable(new StaticShapeTrait<>()));

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public MapCodec<? extends ShapeTrait<?>> codec() {
        return CODEC;
    }

    @Override
    public boolean canBeRegisteredMultipleTimes() {
        return false;
    }

    @Override
    public TextureAtlasSprite getIcon() {
        return iconOf(Items.SHULKER_SHELL);
    }

    /**
     * Same pattern as Walkers {@code FlyingTrait#getIcon} — do not swallow failures as null or
     * Remorphed skips the bubble entirely.
     */
    static TextureAtlasSprite iconOf(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        var shaper = mc.getItemRenderer().getItemModelShaper();
        if (shaper == null) {
            return null;
        }
        BakedModel model = shaper.getItemModel(item);
        if (model == null) {
            return null;
        }
        return model.getParticleIcon();
    }
}
