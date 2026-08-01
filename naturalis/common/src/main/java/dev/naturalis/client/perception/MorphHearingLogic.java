package dev.naturalis.client.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MorphHearingLogic {

    private MorphHearingLogic() {
    }

    public static Optional<MorphHearingCue> scanStrongestCue(LocalPlayer player, MorphHearingProfile profile) {
        if (player == null || !profile.hasEnhancedHearing() || profile.scanRange() < 1.0D) {
            return Optional.empty();
        }

        AABB box = player.getBoundingBox().inflate(profile.scanRange());
        List<Entity> entities = player.level().getEntities(player, box, entity ->
            entity.isAlive() && entity != player && entity instanceof LivingEntity
        );

        MorphHearingCue best = null;
        float bestScore = profile.minCueIntensity();

        for (Entity entity : entities) {
            MorphHearingCueKind kind = classify(player, entity, profile);
            if (kind == null) {
                continue;
            }

            double distance = player.distanceTo(entity);
            if (distance > profile.scanRange()) {
                continue;
            }

            float intensity = (float) (1.0D - (distance / profile.scanRange()));
            intensity *= kindWeight(kind);
            if (intensity <= bestScore) {
                continue;
            }

            double bearing = bearingDegrees(player, entity.position());
            Component label = labelFor(kind, entity, distance);
            best = new MorphHearingCue(kind, bearing, distance, intensity, label, entity);
            bestScore = intensity;
        }

        return Optional.ofNullable(best);
    }

    public static double bearingDegrees(Player player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        Vec3 delta = target.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-4D) {
            return 0.0D;
        }
        double worldAngle = Math.toDegrees(Math.atan2(-delta.x, delta.z));
        double yaw = player.getYRot();
        return Mth.degreesDifference((float) yaw, (float) worldAngle);
    }

    public static float kindWeight(MorphHearingCueKind kind) {
        return switch (kind) {
            case PREY -> 1.25F;
            case THREAT -> 1.15F;
            case PLAYER -> 1.05F;
            case NEUTRAL -> 0.85F;
        };
    }

    private static MorphHearingCueKind classify(Player player, Entity entity, MorphHearingProfile profile) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (entity instanceof Player) {
            return MorphHearingCueKind.PLAYER;
        }
        if (matchesAny(path, profile.preyEntityPaths())) {
            return MorphHearingCueKind.PREY;
        }
        if (entity instanceof Monster || matchesAny(path, profile.threatEntityPaths())) {
            return MorphHearingCueKind.THREAT;
        }
        if (entity instanceof Mob mob && mob.getTarget() == player) {
            return MorphHearingCueKind.THREAT;
        }
        if (entity instanceof LivingEntity) {
            return MorphHearingCueKind.NEUTRAL;
        }
        return null;
    }

    private static Component labelFor(MorphHearingCueKind kind, Entity entity, double distance) {
        int blocks = Mth.ceil((float) distance);
        return switch (kind) {
            case PREY -> Component.translatable("message.naturalis.hearing.prey", blocks);
            case THREAT -> Component.translatable("message.naturalis.hearing.threat", blocks);
            case PLAYER -> Component.translatable("message.naturalis.hearing.player", blocks);
            case NEUTRAL -> Component.translatable("message.naturalis.hearing.neutral", blocks);
        };
    }

    private static boolean matchesAny(String path, java.util.Set<String> tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static Comparator<MorphHearingCue> byIntensity() {
        return Comparator.comparing(MorphHearingCue::intensity).reversed();
    }
}
