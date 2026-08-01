package dev.naturalis.client.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cached mob leg models for first-person paw digging — left and right legs are independent model instances.
 */
public final class MorphPawDigModelCache {

    private static final Map<ResourceLocation, MorphPawDigLimbs> BY_ENTITY = new HashMap<>();

    private MorphPawDigModelCache() {
    }

    public static Optional<MorphPawDigLimbs> resolve(ResourceLocation morphEntityId) {
        if (morphEntityId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ENTITY.computeIfAbsent(morphEntityId, MorphPawDigModelCache::create));
    }

    private static MorphPawDigLimbs create(ResourceLocation morphEntityId) {
        Optional<Holder.Reference<EntityType<?>>> holder = BuiltInRegistries.ENTITY_TYPE.get(morphEntityId);
        if (holder.isEmpty()) {
            return null;
        }
        EntityType<?> type = holder.get().value();

        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        ResourceLocation texture = ResourceLocation.withDefaultNamespace(
            "textures/entity/" + morphEntityId.getPath() + "/" + morphEntityId.getPath() + ".png"
        );

        if (type == EntityType.WOLF) {
            return limbsFromQuadruped(
                new WolfModel(models.bakeLayer(ModelLayers.WOLF)),
                new WolfModel(models.bakeLayer(ModelLayers.WOLF)),
                texture,
                1.0F
            );
        }
        if (type == EntityType.FOX) {
            return limbsFromQuadruped(
                new FoxModel(models.bakeLayer(ModelLayers.FOX)),
                new FoxModel(models.bakeLayer(ModelLayers.FOX)),
                texture,
                0.92F
            );
        }
        if (type == EntityType.CAT || type == EntityType.OCELOT) {
            return limbsFromQuadruped(
                new CatModel(models.bakeLayer(ModelLayers.CAT)),
                new CatModel(models.bakeLayer(ModelLayers.CAT)),
                texture,
                0.85F
            );
        }
        if (type == EntityType.COW || type == EntityType.MOOSHROOM) {
            return limbsFromRootPair(models, ModelLayers.COW, texture, 0.78F);
        }
        if (type == EntityType.PIG) {
            return limbsFromRootPair(models, ModelLayers.PIG, texture, 0.82F);
        }
        if (type == EntityType.SHEEP) {
            return limbsFromRootPair(models, ModelLayers.SHEEP, texture, 0.80F);
        }

        ModelLayerLocation layer = layerForPath(morphEntityId.getPath());
        if (layer == null) {
            return null;
        }
        ModelPart leftRoot = models.bakeLayer(layer);
        ModelPart rightRoot = models.bakeLayer(layer);
        return limbsFromRoot(leftRoot, rightRoot, texture, 0.9F);
    }

    private static MorphPawDigLimbs limbsFromRootPair(EntityModelSet models, ModelLayerLocation layer, ResourceLocation texture, float scale) {
        return limbsFromRoot(models.bakeLayer(layer), models.bakeLayer(layer), texture, scale);
    }

    private static ModelLayerLocation layerForPath(String path) {
        if (path.contains("wolf") || path.contains("dog")) {
            return ModelLayers.WOLF;
        }
        if (path.contains("fox")) {
            return ModelLayers.FOX;
        }
        if (path.contains("cat") || path.contains("ocelot")) {
            return ModelLayers.CAT;
        }
        if (path.contains("cow") || path.contains("mooshroom")) {
            return ModelLayers.COW;
        }
        if (path.contains("pig")) {
            return ModelLayers.PIG;
        }
        if (path.contains("sheep")) {
            return ModelLayers.SHEEP;
        }
        return null;
    }

    private static MorphPawDigLimbs limbsFromQuadruped(Object leftModel, Object rightModel, ResourceLocation texture, float scale) {
        try {
            var leftField = leftModel.getClass().getField("leftFrontLeg");
            var rightField = rightModel.getClass().getField("rightFrontLeg");
            Object left = leftField.get(leftModel);
            Object right = rightField.get(rightModel);
            if (left instanceof ModelPart leftLeg && right instanceof ModelPart rightLeg) {
                return new MorphPawDigLimbs(leftLeg, rightLeg, texture, scale);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static MorphPawDigLimbs limbsFromRoot(ModelPart leftRoot, ModelPart rightRoot, ResourceLocation texture, float scale) {
        return new MorphPawDigLimbs(
            leftRoot.getChild("left_front_leg"),
            rightRoot.getChild("right_front_leg"),
            texture,
            scale
        );
    }

    public record MorphPawDigLimbs(
        ModelPart leftFrontLeg,
        ModelPart rightFrontLeg,
        ResourceLocation texture,
        float scale
    ) {
    }
}
