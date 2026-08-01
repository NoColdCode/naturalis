package dev.naturalis.client.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MorphVibrationLogic {

    private MorphVibrationLogic() {
    }

    public static Optional<MorphVibrationCue> scanStrongestCue(LocalPlayer player, MorphVibrationProfile profile, Minecraft mc) {
        if (player == null || !profile.hasPawVibrationSense()) {
            return Optional.empty();
        }

        MorphVibrationCue best = null;
        float bestScore = profile.minIntensity();

        if (MorphEmbodimentLogic.isBreakingBlock(mc)) {
            MorphVibrationCue mining = miningCue(player, profile);
            if (mining.intensity() > bestScore) {
                best = mining;
                bestScore = mining.intensity();
            }
        }

        AABB box = player.getBoundingBox().inflate(profile.scanRange());
        List<Entity> entities = player.level().getEntities(player, box, Entity::isAlive);

        for (Entity entity : entities) {
            if (entity == player) {
                continue;
            }
            MorphVibrationCue cue = cueFromEntity(player, entity, profile);
            if (cue != null && cue.intensity() > bestScore) {
                best = cue;
                bestScore = cue.intensity();
            }
        }

        return Optional.ofNullable(best);
    }

    private static MorphVibrationCue miningCue(LocalPlayer player, MorphVibrationProfile profile) {
        float progress = MorphDigClientState.destroyProgress();
        float dig = MorphDigClientState.digAnim();
        float intensity = Mth.clamp(0.35F + progress * 0.55F + dig * 0.25F, 0.0F, 1.0F);
        return new MorphVibrationCue(
            MorphVibrationCueKind.MINING,
            0.0D,
            0.0D,
            intensity,
            Component.translatable("message.naturalis.vibration.mining"),
            null
        );
    }

    private static MorphVibrationCue cueFromEntity(LocalPlayer player, Entity entity, MorphVibrationProfile profile) {
        double distance = player.distanceTo(entity);
        if (distance > profile.scanRange() || distance < 0.25D) {
            return null;
        }

        MorphVibrationCueKind kind;
        float intensity;

        if (entity instanceof Warden || entity instanceof Ravager) {
            kind = MorphVibrationCueKind.RUMBLE;
            intensity = (float) ((1.0D - distance / profile.scanRange()) * 1.1D);
        } else if (entity instanceof FallingBlockEntity falling) {
            kind = MorphVibrationCueKind.IMPACT;
            double fallSpeed = Math.abs(falling.getDeltaMovement().y);
            if (fallSpeed < 0.12D) {
                return null;
            }
            intensity = (float) Math.min(1.0D, fallSpeed * 2.5D * (1.0D - distance / profile.scanRange()));
        } else if (entity instanceof LivingEntity living) {
            kind = MorphVibrationCueKind.FOOTFALL;
            double horiz = living.getDeltaMovement().horizontalDistance();
            if (horiz < 0.06D && !living.onGround()) {
                return null;
            }
            double mass = living.getBbWidth() * living.getBbHeight();
            intensity = (float) Math.min(1.0D, horiz * mass * 1.6D * (1.0D - distance / profile.scanRange()));
            if (intensity < profile.minIntensity()) {
                return null;
            }
        } else {
            return null;
        }

        double bearing = MorphHearingLogic.bearingDegrees(player, entity.position());
        Component message = labelFor(kind, entity, distance);
        return new MorphVibrationCue(kind, bearing, distance, intensity, message, entity);
    }

    private static Component labelFor(MorphVibrationCueKind kind, Entity entity, double distance) {
        int blocks = Mth.ceil((float) distance);
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = id.getPath().toLowerCase(Locale.ROOT);

        return switch (kind) {
            case FOOTFALL -> Component.translatable("message.naturalis.vibration.footfall", blocks);
            case MINING -> Component.translatable("message.naturalis.vibration.mining");
            case IMPACT -> Component.translatable("message.naturalis.vibration.impact", blocks);
            case RUMBLE -> Component.translatable("message.naturalis.vibration.rumble", path.replace('_', ' '), blocks);
        };
    }
}
