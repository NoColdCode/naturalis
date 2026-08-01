package dev.naturalis.compat.walkers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import tocraft.walkers.traits.ShapeTrait;

/**
 * Surface floater — stays on top of water and cannot dive underwater
 * (pairs with {@code walkers:stand_on_fluid} for Walkers physics).
 */
public final class FloatingShapeTrait<E extends LivingEntity> extends ShapeTrait<E> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("naturalis", "floating");
    public static final MapCodec<FloatingShapeTrait<?>> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.stable(new FloatingShapeTrait<>()));

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
        return StaticShapeTrait.iconOf(Items.LILY_PAD);
    }
}
