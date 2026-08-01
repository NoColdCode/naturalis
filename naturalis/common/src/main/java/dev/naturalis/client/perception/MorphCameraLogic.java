package dev.naturalis.client.perception;

import dev.naturalis.experience.NaturalisExperienceProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Animal first-person view: separate neck model from vanilla player look (see {@link MorphAnimalView}).
 */
public final class MorphCameraLogic {

    private MorphCameraLogic() {
    }

    public static void enforcePlayerView(Player player, MorphEmbodimentProfile profile, ResourceLocation morphId) {
        Minecraft mc = Minecraft.getInstance();
        if (!profile.hasEmbodiment() || player == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            MorphAnimalView.reset();
            MorphDigViewBlend.reset();
            return;
        }

        boolean fullCamera = NaturalisExperienceProfile.useMorphHeadAndCameraClient();
        if (!fullCamera) {
            MorphAnimalView.reset();
        }

        boolean attackingBlock = MorphEmbodimentLogic.isBreakingBlock(mc);
        boolean pawDig = MorphEmbodimentLogic.usesPawDigging(profile);
        boolean digAnimActive = MorphDigClientState.digAnim() > 0.08F;
        BlockHitResult blockHit = mc.hitResult instanceof BlockHitResult bhr ? bhr : null;

        boolean meaningfulDig = NaturalisExperienceProfile.useDigViewBlendClient()
            && pawDig
            && attackingBlock
            && digAnimActive
            && blockHit != null
            && MorphDigViewBlend.isMeaningfulDigTarget(player, blockHit);

        if (NaturalisExperienceProfile.useDigViewBlendClient()) {
            MorphDigViewBlend.tick(player, blockHit, meaningfulDig);
        } else {
            MorphDigViewBlend.reset();
        }

        if (fullCamera) {
            if (dev.naturalis.client.instinct.WanderLookClientState.isActive()) {
                float yaw = dev.naturalis.client.instinct.WanderLookClientState.targetYaw();
                float pitch = dev.naturalis.client.instinct.WanderLookClientState.targetPitch();
                player.setYRot(yaw);
                player.setYHeadRot(yaw);
                player.setXRot(pitch);
                MorphAnimalView.setEmbodiedYaw(yaw);
                MorphAnimalView.setEmbodiedPitch(pitch);
            } else {
                float neckPitch = MorphAnimalView.constrainNeckPitch(player, profile);
                float neckYaw = MorphAnimalView.constrainNeckYaw(player, profile);
                player.setXRot(neckPitch);
                MorphAnimalView.setEmbodiedPitch(neckPitch);
                MorphAnimalView.setEmbodiedYaw(neckYaw);
            }
        }
    }

    public static void applyCameraOffsets(
        Camera camera,
        Player player,
        MorphEmbodimentProfile profile,
        ResourceLocation morphId,
        float partialTick
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (!profile.hasEmbodiment() || player == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        boolean fullCamera = NaturalisExperienceProfile.useMorphHeadAndCameraClient();
        boolean digBlend = NaturalisExperienceProfile.useDigViewBlendClient();
        boolean digging = MorphDigClientState.digAnim() > 0.12F;

        Vec3 pos = camera.getPosition();

        if (fullCamera) {
            if (Math.abs(profile.eyeHeightOffset()) > 1.0E-6D) {
                pos = pos.add(0.0D, profile.eyeHeightOffset(), 0.0D);
            }
            if (Math.abs(profile.forwardOffset()) > 1.0E-6D) {
                pos = pos.add(player.getViewVector(partialTick).scale(profile.forwardOffset()));
            }
            if (digging) {
                var digNudge = MorphArmInteractionLogic.digCameraNudge(
                    profile.armInteractionStyle(),
                    MorphDigClientState.digAnim(),
                    MorphEmbodimentLogic.blockDestroyProgress(mc),
                    player.tickCount,
                    partialTick
                );
                float rake = (float) Math.max(0.0D, Math.sin((player.tickCount + partialTick) * 4.2D));
                float digDrop = digNudge.isActive() ? digNudge.forwardBlocks() * 2.5F : 0.04F;
                pos = pos.add(0.0D, -digDrop - rake * 0.025D, 0.0D);
                pos = pos.add(player.getViewVector(partialTick).scale(0.03D + rake * 0.02D + digNudge.forwardBlocks()));
            } else if (MorphPeckClientState.isActive()) {
                Vec3 view = player.getViewVector(partialTick);
                pos = pos.add(0.0D, MorphPeckClientState.verticalBlocks(), 0.0D);
                pos = pos.add(view.scale(MorphPeckClientState.forwardBlocks()));
            } else if (MorphSniffClientState.isActive()) {
                Vec3 view = player.getViewVector(partialTick);
                pos = pos.add(0.0D, MorphSniffClientState.verticalBlocks(), 0.0D);
                pos = pos.add(view.scale(MorphSniffClientState.forwardBlocks()));
            } else if (MorphListenClientState.isFocusActive()) {
                pos = pos.add(0.0D, -0.035D * MorphListenClientState.focusStrength(), 0.0D);
            } else if (MorphGaitLogic.usesQuadrupedGait(profile)) {
                var gait = MorphGaitLogic.computeGaitOffsets(player, profile, partialTick);
                pos = pos.add(0.0D, gait.lift(), 0.0D).add(player.getViewVector(partialTick).scale(gait.forward()));
            }

            if (morphId != null && MorphVibrationClientState.feltIntensity() > 0.06F
                && NaturalisExperienceProfile.useVibrationCameraClient()) {
                MorphVibrationProfile vib = MorphPerceptionScaling.vibration(MorphVibrationProfiles.resolve(morphId));
                float phase = MorphVibrationClientState.tremorPhase();
                float scale = vib.cameraTremorScale() * MorphVibrationClientState.feltIntensity();
                pos = pos.add(0.0D, (float) Math.sin(phase * 4.1D) * 0.012D * scale, 0.0D);
            }
            setPosition(camera, pos);
        }

        float pitch = fullCamera && !Float.isNaN(MorphAnimalView.embodiedPitch())
            ? MorphAnimalView.embodiedPitch()
            : player.getXRot();
        float wanderYaw = dev.naturalis.client.instinct.WanderLookClientState.embodiedYawForCamera(player);
        float wanderPitch = dev.naturalis.client.instinct.WanderLookClientState.embodiedPitchForCamera(player);
        float yaw = !Float.isNaN(wanderYaw)
            ? wanderYaw
            : fullCamera && !Float.isNaN(MorphAnimalView.embodiedYaw())
            ? MorphAnimalView.embodiedYaw()
            : player.getYRot();
        if (!Float.isNaN(wanderPitch)) {
            pitch = wanderPitch;
        }

        if (digging && digBlend) {
            MorphDigViewBlend.tickPartial(partialTick);
            pitch += MorphDigViewBlend.pitchOffset();
            yaw += MorphDigViewBlend.yawOffset();
            if (fullCamera) {
                var digNudge = MorphArmInteractionLogic.digCameraNudge(
                    profile.armInteractionStyle(),
                    MorphDigClientState.digAnim(),
                    MorphEmbodimentLogic.blockDestroyProgress(mc),
                    player.tickCount,
                    partialTick
                );
                pitch += digNudge.pitchDegrees();
                yaw += digNudge.yawDegrees();
                float dig = MorphDigClientState.digAnim();
                float phase = player.tickCount + partialTick;
                // Rake bob: only downward (positive pitch in MC), never glance upward.
                float rake = (float) Math.max(0.0D, Math.sin(phase * 4.2D));
                pitch += rake * 1.1F * dig;
                yaw += (float) Math.sin(phase * 2.1D) * 0.7F * dig;
            }
        } else if (fullCamera && NaturalisExperienceProfile.useGaitCameraSwayClient()) {
            pitch += MorphAnimalView.cameraSniffPitchOffset(player, profile, partialTick);
            yaw += MorphAnimalView.cameraSwayYawOffset(player, profile, partialTick);
        }

        if (MorphPeckClientState.isActive()) {
            pitch += MorphPeckClientState.pitchOffsetDegrees();
            yaw += MorphPeckClientState.yawOffsetDegrees();
        } else if (MorphSniffClientState.isActive()) {
            pitch += MorphSniffClientState.pitchOffsetDegrees();
            yaw += MorphSniffClientState.yawOffsetDegrees();
        } else if (MorphListenClientState.isActive()) {
            pitch += MorphListenClientState.pitchOffsetDegrees();
            yaw += MorphListenClientState.yawOffsetDegrees();
        }

        setRotation(camera, yaw, pitch);

        if (fullCamera) {
            MorphEmbodimentDriftEffects.applyToCamera(camera, player, profile, partialTick);
        }
    }

    private static void setPosition(Camera camera, Vec3 position) {
        try {
            Method vec = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            vec.setAccessible(true);
            vec.invoke(camera, position);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method xyz = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
            xyz.setAccessible(true);
            xyz.invoke(camera, position.x, position.y, position.z);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setRotation(Camera camera, float yaw, float pitch) {
        try {
            Method method = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            method.setAccessible(true);
            method.invoke(camera, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
            setField(camera, "xRot", pitch);
            setField(camera, "yRot", yaw);
        }
    }

    private static void setField(Camera camera, String name, float value) {
        try {
            Field field = Camera.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setFloat(camera, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
