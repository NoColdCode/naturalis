package dev.naturalis.mixin;

import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.client.RuleFlagsClientCache;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class QuadrupedContainerRestrictionMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void naturalis$blockRestrictedInventorySlots(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (hoveredSlot == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (!RuleFlagsClientCache.isInventoryRestrictionEnabled()) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!InventoryRestrictionManager.isQuadruped(morphId)) {
            return;
        }

        if (hoveredSlot.container != mc.player.getInventory()) {
            return;
        }

        // In any GUI, quadrupeds below unlock level cannot interact with inventory slots.
        if (!MorphLevelClientCache.isInventoryUnlocked()) {
            mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
            cir.setReturnValue(false);
            return;
        }

        // Even when inventory is unlocked, locked hotbar slots remain blocked.
        int allowedSlots = MorphLevelClientCache.getHotbarSlots();
        int containerSlot = hoveredSlot.getContainerSlot();
        if (containerSlot >= allowedSlots && containerSlot < 9) {
            mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
            cir.setReturnValue(false);
        }
    }
}
