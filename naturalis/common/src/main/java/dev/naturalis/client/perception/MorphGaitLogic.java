package dev.naturalis.client.perception;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Archetype-specific first-person head motion: sniff prowl, trot bob, skitter, peck, swim sway, alert idle.
 */
public final class MorphGaitLogic {

    private static float gaitPhase;
    private static float sniffPhase;
    private static float idlePhase;

    private MorphGaitLogic() {
    }

    public static boolean usesQuadrupedGait(MorphEmbodimentProfile profile) {
        return usesStyleGait(profile);
    }

    public static boolean usesSniffWalk(MorphEmbodimentProfile profile) {
        return profile.armInteractionStyle() == MorphArmInteractionStyle.CANINE
            || profile.armInteractionStyle() == MorphArmInteractionStyle.FELINE;
    }

    public static boolean usesStyleGait(MorphEmbodimentProfile profile) {
        return profile.hasEmbodiment()
            && profile.armInteractionStyle() != MorphArmInteractionStyle.NONE;
    }

    public static boolean usesIdleHeadBob(MorphEmbodimentProfile profile) {
        return switch (profile.armInteractionStyle()) {
            case CANINE, FELINE, AVIAN, EQUINE, SPIDER, AQUATIC, GENERIC -> true;
            default -> false;
        };
    }

    public static float cameraPitchScale(MorphEmbodimentProfile profile) {
        return switch (profile.armInteractionStyle()) {
            case CANINE, FELINE -> 0.35F;
            case EQUINE -> 0.28F;
            case SPIDER -> 0.42F;
            case AVIAN -> 0.55F;
            case AQUATIC -> 0.32F;
            case GENERIC -> 0.38F;
            default -> 0.0F;
        };
    }

    public static float cameraYawScale(MorphEmbodimentProfile profile) {
        return switch (profile.armInteractionStyle()) {
            case CANINE, FELINE -> 0.25F;
            case EQUINE -> 0.18F;
            case SPIDER -> 0.35F;
            case AVIAN -> 0.30F;
            case AQUATIC -> 0.22F;
            case GENERIC -> 0.25F;
            default -> 0.0F;
        };
    }

    public static GaitOffsets computeGaitOffsets(Player player, MorphEmbodimentProfile profile, float partialTick) {
        if (!usesStyleGait(profile)) {
            gaitPhase = 0.0F;
            return GaitOffsets.NONE;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean moving = horizontal > 0.02D && (player.onGround() || horizontal > 0.1D);
        boolean inWater = player.isInWater() || player.isUnderWater();

        float speed = (float) Mth.clamp(horizontal * 7.0D, 0.0D, 1.5D);
        float intensity = Math.max(speed, moving ? 0.35F : 0.0F)
            * (float) profile.firstPersonArmHideStrength()
            * MorphPerceptionScaling.sniffGaitStrength(profile);

        return switch (profile.armInteractionStyle()) {
            case CANINE, FELINE -> computeSniffGait(moving, speed, intensity, partialTick);
            case EQUINE -> computeEquineGait(moving, speed, intensity, partialTick);
            case SPIDER -> computeSpiderGait(moving, speed, intensity, partialTick);
            case AVIAN -> computeAvianGait(moving, speed, intensity, partialTick);
            case AQUATIC -> computeAquaticGait(moving, speed, intensity, partialTick, inWater);
            case GENERIC -> computeGenericQuadrupedGait(moving, speed, intensity, partialTick);
            default -> {
                gaitPhase = 0.0F;
                yield GaitOffsets.NONE;
            }
        };
    }

    private static GaitOffsets computeSniffGait(boolean moving, float speed, float intensity, float partialTick) {
        sniffPhase += (0.35F + speed * 0.5F) * (1.0F + partialTick * 0.04F);
        float sniffDown = 5.0F + (float) Math.sin(sniffPhase) * 2.5F;
        float prowlSway = (float) Math.sin(sniffPhase * 0.85D) * 3.5F * intensity;
        float prowlRoll = (float) Math.sin(sniffPhase * 0.85D + 0.5D) * 6.0F * intensity;
        float bob = (float) Math.sin(sniffPhase * 1.7D) * 1.2F * intensity;

        if (!moving) {
            idlePhase += 0.22F * (1.0F + partialTick * 0.03F);
            float idle = (float) Math.sin(idlePhase) * 1.8F * Math.max(intensity, 0.25F);
            return new GaitOffsets(idle * 0.6F, prowlSway * 0.25F, prowlRoll * 0.2F, 0.0F, 0.0F);
        }

        gaitPhase += (0.55F + speed * 0.75F) * (1.0F + partialTick * 0.05F);
        float step = (float) Math.sin(gaitPhase);
        float lift = Math.abs((float) Math.sin(gaitPhase * 2.0D)) * 0.028F * intensity;
        return new GaitOffsets(
            sniffDown + bob + step * 0.8F,
            prowlSway + step * 1.5F,
            prowlRoll,
            lift,
            step * 0.02F
        );
    }

    private static GaitOffsets computeEquineGait(boolean moving, float speed, float intensity, float partialTick) {
        if (!moving) {
            idlePhase += 0.15F;
            float breathe = (float) Math.sin(idlePhase * 0.9D) * 1.2F;
            return new GaitOffsets(-breathe * 0.4F, 0.0F, breathe * 0.15F, 0.0F, 0.0F);
        }
        gaitPhase += (0.62F + speed * 0.8F) * (1.0F + partialTick * 0.05F);
        float step = (float) Math.sin(gaitPhase);
        float trot = (float) Math.sin(gaitPhase * 2.0D);
        return new GaitOffsets(
            -4.0F * intensity + trot * 2.5F * intensity,
            step * 1.8F * intensity,
            trot * 4.0F * intensity,
            Math.abs(trot) * 0.04F * intensity,
            trot * 0.028F * intensity
        );
    }

    private static GaitOffsets computeSpiderGait(boolean moving, float speed, float intensity, float partialTick) {
        sniffPhase += (0.4F + speed * 0.45F) * (1.0F + partialTick * 0.04F);
        float skitter = (float) Math.sin(sniffPhase * 1.4D) * 4.5F * intensity;
        float lateral = (float) Math.sin(sniffPhase * 0.7D + 1.2D) * 5.5F * intensity;

        if (!moving) {
            idlePhase += 0.18F;
            float still = (float) Math.sin(idlePhase * 1.1D) * 2.0F * Math.max(intensity, 0.2F);
            return new GaitOffsets(6.0F + still, lateral * 0.3F, skitter * 0.25F, 0.0F, 0.0F);
        }

        gaitPhase += (0.7F + speed * 0.9F) * (1.0F + partialTick * 0.05F);
        float phase = (float) Math.sin(gaitPhase * 1.6D);
        return new GaitOffsets(
            8.0F * intensity + phase * 2.0F,
            lateral + phase * 2.2F,
            skitter + phase * 3.0F,
            Math.abs(phase) * 0.02F * intensity,
            phase * 0.012F
        );
    }

    private static GaitOffsets computeAvianGait(boolean moving, float speed, float intensity, float partialTick) {
        sniffPhase += (0.5F + speed * 0.55F) * (1.0F + partialTick * 0.04F);
        float peck = (float) Math.max(0.0D, Math.sin(sniffPhase * 2.2D)) * 6.0F * intensity;

        if (!moving) {
            idlePhase += 0.28F;
            float watch = (float) Math.sin(idlePhase * 0.65D) * 2.5F;
            return new GaitOffsets(watch, (float) Math.sin(idlePhase * 0.4D) * 1.5F, 0.0F, 0.0F, 0.0F);
        }

        gaitPhase += (0.75F + speed) * (1.0F + partialTick * 0.05F);
        float flap = (float) Math.sin(gaitPhase * 3.0D) * 1.5F * intensity;
        return new GaitOffsets(
            peck + flap,
            (float) Math.sin(gaitPhase) * 2.0F * intensity,
            flap * 0.5F,
            0.0F,
            (float) Math.sin(gaitPhase * 2.0D) * 0.01F * intensity
        );
    }

    private static GaitOffsets computeAquaticGait(
        boolean moving,
        float speed,
        float intensity,
        float partialTick,
        boolean inWater
    ) {
        float waterBoost = inWater ? 1.35F : 0.75F;
        sniffPhase += (0.25F + speed * 0.35F) * waterBoost * (1.0F + partialTick * 0.04F);
        float roll = (float) Math.sin(sniffPhase * 0.8D) * 5.0F * intensity * waterBoost;
        float pitch = (float) Math.sin(sniffPhase * 1.1D + 0.4D) * 3.0F * intensity * waterBoost;

        if (!moving) {
            idlePhase += 0.12F * waterBoost;
            float drift = (float) Math.sin(idlePhase) * 1.5F * waterBoost;
            return new GaitOffsets(drift, roll * 0.2F, roll * 0.35F, 0.0F, 0.0F);
        }

        gaitPhase += (0.4F + speed * 0.5F) * waterBoost;
        float surge = (float) Math.sin(gaitPhase);
        return new GaitOffsets(
            pitch + surge * 1.5F,
            roll * 0.6F,
            roll + surge * 2.0F,
            Math.abs(surge) * 0.018F * intensity * waterBoost,
            surge * 0.015F * waterBoost
        );
    }

    private static GaitOffsets computeGenericQuadrupedGait(boolean moving, float speed, float intensity, float partialTick) {
        if (!moving) {
            idlePhase += 0.16F;
            float alert = (float) Math.sin(idlePhase * 0.75D) * 2.0F * Math.max(intensity, 0.22F);
            return new GaitOffsets(alert, alert * 0.4F, alert * 0.25F, 0.0F, 0.0F);
        }

        gaitPhase += (0.5F + speed * 0.65F) * (1.0F + partialTick * 0.05F);
        float step = (float) Math.sin(gaitPhase);
        float doubleStep = (float) Math.sin(gaitPhase * 2.0D);
        return new GaitOffsets(
            doubleStep * 5.0F * intensity + 6.0F * intensity,
            step * 3.0F * intensity,
            step * 7.0F * intensity,
            Math.abs(doubleStep) * 0.035F * intensity,
            doubleStep * 0.022F * intensity
        );
    }

    public record GaitOffsets(float pitch, float yaw, float roll, float lift, float forward) {
        public static final GaitOffsets NONE = new GaitOffsets(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }
}
