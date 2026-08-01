package dev.naturalis.morph.quickslot;



import dev.naturalis.compat.CompatAccess;

import dev.naturalis.util.CurrentMorphUtil;

import dev.naturalis.util.MorphEntityUtil;

import dev.naturalis.util.MorphShapeUtil;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import tocraft.walkers.api.PlayerAbilities;

import tocraft.walkers.api.PlayerShape;

import tocraft.walkers.api.PlayerShapeChanger;

import tocraft.walkers.api.variant.ShapeType;

import tocraft.walkers.impl.PlayerDataProvider;



/** Applies a morph on the server using the Remorphed / Woodwalkers swap path. */

public final class MorphQuickSlotMorphUtil {



    private MorphQuickSlotMorphUtil() {

    }



    @SuppressWarnings("unchecked")

    public static boolean applyMorph(ServerPlayer player, ResourceLocation morphId) {

        if (morphId == null) {

            return false;

        }



        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);

        if (morphId.equals(current)) {

            return true;

        }



        EntityType<?> entityType = CompatAccess.getEntityType(morphId);

        if (entityType == null) {

            MorphQuickSlotDebug.event("server", "applyMorph rejected unknown type " + morphId);

            return false;

        }

        if (!MorphEntityUtil.isLivingEntityType(entityType, player)) {

            MorphQuickSlotDebug.event("server", "applyMorph rejected non-living type " + morphId);

            return false;

        }



        ShapeType<? extends LivingEntity> shapeType = ShapeType.from((EntityType<? extends LivingEntity>) entityType);

        if (shapeType == null) {

            MorphQuickSlotDebug.event("server", "applyMorph rejected unknown shape " + morphId);

            return false;

        }



        PlayerDataProvider provider = (PlayerDataProvider) player;

        provider.walkers$set2ndShape(shapeType);

        PlayerShapeChanger.sync(player);

        PlayerAbilities.sync(player);



        LivingEntity shape = shapeType.create(player.level(), player);

        if (shape == null) {

            MorphQuickSlotDebug.event("server", "applyMorph failed to create shape " + morphId);

            return false;

        }

        MorphShapeUtil.applyStoredShape(player, shape, morphId);

        // Quick-slot morphs use the same direct path as /walkers switchShape — bypasses SWAP_SHAPE guards

        // (resonance human lock, morph binding, etc.) while the quick-slot session is active on the server.

        provider.walkers$updateShapes(shape);

        player.refreshDimensions();

        PlayerShape.sync(player);



        ResourceLocation applied = CurrentMorphUtil.getCurrentMorphId(player);

        boolean success = morphId.equals(applied);

        MorphQuickSlotDebug.event(

            "server",

            "applyMorph " + morphId + " current=" + current + " result=" + applied + " ok=" + success

        );

        return success;

    }

}


