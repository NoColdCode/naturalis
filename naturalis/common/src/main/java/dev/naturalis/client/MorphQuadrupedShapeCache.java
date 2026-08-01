package dev.naturalis.client;

import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.tocraft.walkers.api.PlayerShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks Walkers shape entity ids that should hide hand items and show mouth-carried stacks instead.
 */
public final class MorphQuadrupedShapeCache {

    private static final Set<Integer> MOUTH_CARRY_SHAPE_IDS = new HashSet<>();
    private static final Map<Integer, AbstractClientPlayer> SHAPE_OWNERS = new HashMap<>();

    private MorphQuadrupedShapeCache() {
    }

    public static void rebuild(Minecraft mc) {
        MOUTH_CARRY_SHAPE_IDS.clear();
        SHAPE_OWNERS.clear();
        if (mc == null || mc.level == null) {
            return;
        }

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!MorphQuadrupedMouthRender.shouldShowMouthCarry(player, mc)) {
                continue;
            }
            LivingEntity shape = PlayerShape.getCurrentShape(player);
            if (shape == null) {
                continue;
            }
            int id = shape.getId();
            MOUTH_CARRY_SHAPE_IDS.add(id);
            SHAPE_OWNERS.put(id, player);
            MorphQuadrupedMouthRender.hideShapeHandItems(shape);
        }
    }

    public static boolean isMouthCarryShape(int entityId) {
        return MOUTH_CARRY_SHAPE_IDS.contains(entityId);
    }

    public static AbstractClientPlayer ownerForShape(int entityId) {
        return SHAPE_OWNERS.get(entityId);
    }

    public static boolean shouldHideHandItems(int entityId) {
        return MOUTH_CARRY_SHAPE_IDS.contains(entityId);
    }

    /** Match render state to a tracked morph shape (Walkers uses entity render states, not entity ids). */
    public static boolean shouldHideHandItems(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || state == null) {
            return false;
        }
        for (int entityId : MOUTH_CARRY_SHAPE_IDS) {
            var entity = mc.level.getEntity(entityId);
            if (entity == null) {
                continue;
            }
            double dx = entity.getX() - state.x;
            double dy = entity.getY() - state.y;
            double dz = entity.getZ() - state.z;
            if (dx * dx + dy * dy + dz * dz < 0.25D) {
                return true;
            }
        }
        return false;
    }

    public static boolean isQuadrupedMorph(ResourceLocation morphId) {
        return morphId != null && InventoryRestrictionManager.isQuadruped(morphId);
    }
}
