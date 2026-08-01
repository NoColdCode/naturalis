package dev.naturalis.util;

import dev.naturalis.NaturalisMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Translation core is meant to be active only while held, not merely present anywhere in inventory.
 */
public final class TranslationDeviceUtil {

    private static final ResourceLocation TRANSLATION_CORE_ID =
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "translation_core");

    private TranslationDeviceUtil() {
    }

    public static boolean isTranslationCoreStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(TRANSLATION_CORE_ID);
    }

    public static boolean isTranslationCoreHeld(Player player) {
        return isTranslationCoreStack(player.getMainHandItem()) || isTranslationCoreStack(player.getOffhandItem());
    }
}
