package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphArmorTier;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renders MorphArmorItem on any living renderer layer target.
 *
 * <p>This intentionally does NOT extend {@code HumanoidArmorLayer} because
 * WoodWalkers' {@code RenderLayerMixin} suppresses that class specifically
 * for non-humanoid morphs (wolf, etc.) via {@code coloredCutoutModelCopyLayerRender}.
 * By being a plain {@link RenderLayer} subclass, this layer is unaffected.
 *
 * <p>Forge 1.20.1 — uses 4-float renderToBuffer signature (r, g, b, a)
 * and {@code new ResourceLocation} instead of {@code fromNamespaceAndPath}.
 */
@OnlyIn(Dist.CLIENT)
public class MorphArmorRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public MorphArmorRenderLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof MorphArmorItem)) {
            return;
        }

        MorphArmorTier tier = MorphArmorItem.getTier(chestStack);
        ResourceLocation texture = new ResourceLocation("naturalis",
            "textures/models/armor/morph_armor_" + tier.id + "_layer_1.png");

        // Render as translucent so the underlying morph details stay visible.
        // 1.20.1 uses 4-float renderToBuffer: (poseStack, vc, light, overlay, r, g, b, alpha)
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        this.getParentModel().renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
            1.0F, 1.0F, 1.0F, 0.72F);
    }
}
