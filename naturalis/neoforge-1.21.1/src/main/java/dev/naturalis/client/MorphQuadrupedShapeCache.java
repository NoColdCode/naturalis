package dev.naturalis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import tocraft.walkers.api.PlayerShape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
}
