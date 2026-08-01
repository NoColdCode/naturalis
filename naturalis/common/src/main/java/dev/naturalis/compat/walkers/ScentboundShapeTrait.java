package dev.naturalis.compat.walkers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import tocraft.walkers.traits.ShapeTrait;

/** Keen-nose morphs with strong scent tracking. */
public final class ScentboundShapeTrait<E extends LivingEntity> extends ShapeTrait<E> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("naturalis", "scentbound");
    public static final MapCodec<ScentboundShapeTrait<?>> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.stable(new ScentboundShapeTrait<>()));

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
        return StaticShapeTrait.iconOf(Items.BONE);
    }
}
