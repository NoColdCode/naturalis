package dev.naturalis.client.perception;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.api.model.ArmRenderingManipulator;
import tocraft.walkers.api.model.EntityArms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Draws a morph limb (or a scaled shape bust) in the first-person hand pass.
 * Covers integration morphs whose renderers are not Walkers {@link EntityArms}-aware
 * (e.g. GeckoLib Naturalist mobs), where vanilla otherwise keeps the player arm.
 */
public final class MorphFirstPersonMorphHandRenderer {

    private static final String[] ARM_PART_CANDIDATES = {
        "right_front_leg", "rightFrontLeg", "right_leg", "rightLeg",
        "right_arm", "rightArm", "arm_right", "front_right_leg",
        "leg0", "leg1", "bone", "body", "shell", "head"
    };

    private MorphFirstPersonMorphHandRenderer() {
    }

    public static boolean render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        AbstractClientPlayer player,
        float partialTick,
        double handSide
    ) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return false;
        }

        syncShapeBasics(player, shape);

        Minecraft mc = Minecraft.getInstance();
        EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(shape);

        poseStack.pushPose();
        try {
            poseStack.translate(0.42D * handSide, -0.42D, -0.55D);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (handSide > 0 ? -12.0F : 12.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            float fit = 0.62F / Math.max(0.35F, Math.max(shape.getBbWidth(), shape.getBbHeight() * 0.55F));
            poseStack.scale(fit, fit, fit);

            if (renderer instanceof LivingEntityRenderer<?, ?> living
                && renderLivingLimb(poseStack, buffer, packedLight, shape, living, partialTick)) {
                return true;
            }

            return renderFullShape(poseStack, buffer, packedLight, shape, partialTick);
        } finally {
            poseStack.popPose();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean renderLivingLimb(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        LivingEntity shape,
        LivingEntityRenderer<?, ?> living,
        float partialTick
    ) {
        EntityModel model = living.getModel();
        if (model == null) {
            return false;
        }

        ModelPart arm = null;
        ArmRenderingManipulator manipulator = null;

        try {
            Tuple<ModelPart, ArmRenderingManipulator<?>> arms = EntityArms.get(shape, model);
            if (arms != null) {
                arm = arms.getA();
                manipulator = arms.getB();
            }
        } catch (Throwable ignored) {
        }

        if (arm == null) {
            arm = findArmPart(model);
        }
        if (arm == null) {
            return false;
        }

        model.attackTime = 0.0F;
        try {
            model.setupAnim(shape, 0.0F, 0.0F, shape.tickCount + partialTick, 0.0F, 0.0F);
        } catch (Throwable ignored) {
        }

        if (manipulator != null) {
            try {
                manipulator.run(poseStack, model);
            } catch (Throwable ignored) {
            }
        }

        arm.xRot = 0.0F;
        ResourceLocation texture = resolveTexture(living, shape);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        arm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        return true;
    }

    private static boolean renderFullShape(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        LivingEntity shape,
        float partialTick
    ) {
        Minecraft mc = Minecraft.getInstance();
        float yBody = shape.yBodyRot;
        float yBodyO = shape.yBodyRotO;
        float yHead = shape.yHeadRot;
        float yHeadO = shape.yHeadRotO;
        float yRot = shape.getYRot();
        float yRotO = shape.yRotO;
        float xRot = shape.getXRot();
        float xRotO = shape.xRotO;

        shape.yBodyRot = 180.0F;
        shape.yBodyRotO = 180.0F;
        shape.yHeadRot = 180.0F;
        shape.yHeadRotO = 180.0F;
        shape.setYRot(180.0F);
        shape.yRotO = 180.0F;
        shape.setXRot(0.0F);
        shape.xRotO = 0.0F;

        try {
            mc.getEntityRenderDispatcher().render(
                shape,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                partialTick,
                poseStack,
                buffer,
                packedLight
            );
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            shape.yBodyRot = yBody;
            shape.yBodyRotO = yBodyO;
            shape.yHeadRot = yHead;
            shape.yHeadRotO = yHeadO;
            shape.setYRot(yRot);
            shape.yRotO = yRotO;
            shape.setXRot(xRot);
            shape.xRotO = xRotO;
        }
    }

    @Nullable
    private static ModelPart findArmPart(EntityModel<?> model) {
        ModelPart root = findRoot(model);
        if (root == null) {
            return null;
        }
        for (String name : ARM_PART_CANDIDATES) {
            ModelPart part = findChildMatching(root, n -> n.equalsIgnoreCase(name));
            if (part != null) {
                return part;
            }
        }
        ModelPart limb = findChildMatching(root, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.contains("leg") || lower.contains("arm") || lower.contains("paw")
                || lower.contains("wing") || lower.contains("tentacle") || lower.contains("shell");
        });
        return limb != null ? limb : root;
    }

    @Nullable
    private static ModelPart findRoot(EntityModel<?> model) {
        try {
            for (Field field : model.getClass().getFields()) {
                if (ModelPart.class.isAssignableFrom(field.getType())) {
                    Object value = field.get(model);
                    if (value instanceof ModelPart part) {
                        return part;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method root = model.getClass().getMethod("root");
            Object value = root.invoke(model);
            if (value instanceof ModelPart part) {
                return part;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @Nullable
    private static ModelPart findChildMatching(ModelPart root, java.util.function.Predicate<String> nameMatch) {
        try {
            Field childrenField = ModelPart.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Object children = childrenField.get(root);
            if (children instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && nameMatch.test(key)
                        && entry.getValue() instanceof ModelPart part) {
                        return part;
                    }
                }
                for (var entry : map.entrySet()) {
                    if (entry.getValue() instanceof ModelPart part) {
                        ModelPart nested = findChildMatching(part, nameMatch);
                        if (nested != null) {
                            return nested;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static ResourceLocation resolveTexture(LivingEntityRenderer<?, ?> living, LivingEntity shape) {
        try {
            Method method = LivingEntityRenderer.class.getDeclaredMethod("getTextureLocation", LivingEntity.class);
            method.setAccessible(true);
            Object value = method.invoke(living, shape);
            if (value instanceof ResourceLocation location) {
                return location;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(shape.getType());
        return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/entity/" + key.getPath() + ".png");
    }

    private static void syncShapeBasics(AbstractClientPlayer player, LivingEntity shape) {
        shape.walkAnimation.setSpeed(player.walkAnimation.speed());
        shape.swinging = player.swinging;
        shape.swingTime = player.swingTime;
        shape.attackAnim = player.attackAnim;
        shape.oAttackAnim = player.oAttackAnim;
        shape.tickCount = player.tickCount;
        shape.setOnGround(player.onGround());
        shape.setXRot(player.getXRot());
        shape.xRotO = player.xRotO;
    }
}
