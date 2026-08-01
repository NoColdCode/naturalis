package dev.naturalis.mixin;

import dev.naturalis.chat.MorphComprehensionProfile;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignText.class)
public class SignTextComprehensionMixin {

    @Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
    private void naturalis$distortSignReading(int line, boolean filtered, CallbackInfoReturnable<Component> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Player player = mc.player;
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || TranslationDeviceUtil.isTranslationCoreHeld(player)) {
            return;
        }

        Component original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        String distorted = MorphComprehensionProfile.scrambleForMorph(morphId, original.getString());
        cir.setReturnValue(Component.literal(distorted));
    }

}
