package dev.naturalis.morph.quickslot;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphEntityUtil;
import dev.naturalis.util.MorphShapeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import dev.tocraft.walkers.api.PlayerAbilities;
import dev.tocraft.walkers.api.PlayerShape;
import dev.tocraft.walkers.api.PlayerShapeChanger;
import dev.tocraft.walkers.api.variant.ShapeType;
import dev.tocraft.walkers.impl.PlayerDataProvider;

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
        if (entityType == null || !MorphEntityUtil.isLivingEntityType(entityType, player)) {
            return false;
        }

        ShapeType<? extends LivingEntity> shapeType = ShapeType.from((EntityType<? extends LivingEntity>) entityType);
        if (shapeType == null) {
            return false;
        }

        if (!PlayerShapeChanger.change2ndShape(player, shapeType)) {
            PlayerDataProvider provider = (PlayerDataProvider) player;
            provider.walkers$set2ndShape(shapeType);
        }
        PlayerShapeChanger.sync(player);
        PlayerAbilities.sync(player);

        LivingEntity shape = shapeType.create(player.level(), player);
        if (shape == null) {
            return false;
        }

        MorphShapeUtil.applyStoredShape(player, shape, morphId);

        if (PlayerShape.getCurrentShape(player) == null) {
            if (PlayerShape.updateShapes(player, shape)) {
                player.refreshDimensions();
                PlayerShape.sync(player);
                return true;
            }
        }

        PlayerDataProvider provider = (PlayerDataProvider) player;
        provider.walkers$updateShapes(shape);
        PlayerShape.sync(player);
        player.refreshDimensions();
        return PlayerShape.getCurrentShape(player) != null;
    }
}
