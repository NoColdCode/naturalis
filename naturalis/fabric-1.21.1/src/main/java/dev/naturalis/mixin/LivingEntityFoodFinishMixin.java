package dev.naturalis.mixin;

import dev.naturalis.diet.DietLogic;
import dev.naturalis.resonance.ResonanceLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric substitute for NeoForge {@code LivingEntityUseItemEvent.Finish}.
 * {@code completeUsingItem} is void on 1.21.x — capture {@link LivingEntity#getUseItem()} at HEAD.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFoodFinishMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void naturalis$onCompleteUsingItem(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        ItemStack consumed = self.getUseItem();
        if (consumed == null || consumed.isEmpty()) {
            return;
        }
        ItemStack snapshot = consumed.copy();
        DietLogic.onFoodFinish(player, snapshot);
        ResonanceLogic.onFoodFinished(player, snapshot);
    }
}
