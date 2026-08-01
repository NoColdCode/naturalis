package dev.naturalis.client.perception;

import dev.naturalis.instinct.InstinctEvents;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.network.ScentHintPayload;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Short-lived action-bar readout after an active sniff. */
public final class MorphSniffHudClient {

    private static final int MAX_LINES = 4;
    private static final int DISPLAY_TICKS = 100;

    private static final List<Line> LINES = new ArrayList<>();
    private static int displayTicks;

    private MorphSniffHudClient() {
    }

    public static void populateFromWorld(Minecraft mc, int smellStrength) {
        LocalPlayer player = mc.player;
        if (player == null || player.level() == null) {
            return;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || InstinctManager.getSmellStrength(morphId) <= 0) {
            return;
        }

        LINES.clear();
        double range = 12.0D + smellStrength * 8.0D;
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
            LivingEntity.class,
            box,
            e -> e.isAlive() && e != player && player.distanceToSqr(e) <= range * range
        );
        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));

        for (LivingEntity entity : nearby) {
            if (LINES.size() >= MAX_LINES) {
                break;
            }
            byte category = InstinctEvents.classifyScentTarget(morphId, entity);
            Component label = Component.translatable(entity.getType().getDescriptionId());
            int dist = (int) Math.round(player.distanceTo(entity));
            LINES.add(new Line(category, label, dist));
        }
        if (!LINES.isEmpty()) {
            displayTicks = DISPLAY_TICKS;
        }
    }

    public static void tick() {
        if (displayTicks > 0) {
            displayTicks--;
        }
        if (displayTicks <= 0) {
            LINES.clear();
        }
    }

    public static Component actionBarOverlay() {
        if (displayTicks <= 0 || LINES.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < LINES.size(); i++) {
            Line line = LINES.get(i);
            if (i > 0) {
                text.append("  |  ");
            }
            String prefix = switch (line.category) {
                case ScentHintPayload.CATEGORY_PREY -> "◆ ";
                case ScentHintPayload.CATEGORY_HOSTILE -> "▲ ";
                default -> "○ ";
            };
            text.append(prefix).append(line.label.getString())
                .append(" (").append(line.distance).append("m)");
        }
        return Component.literal(text.toString());
    }

    private record Line(byte category, Component label, int distance) {
    }
}
