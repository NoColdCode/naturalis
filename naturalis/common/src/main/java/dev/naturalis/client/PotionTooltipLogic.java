package dev.naturalis.client;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;

/** Loader-neutral morph potion tooltip lines. */
public final class PotionTooltipLogic {

    private static final int DEFAULT_BREWED_DURATION = 20 * 60;
    private static final int DEFAULT_BINDING_DURATION = 8 * 20 * 60;

    private PotionTooltipLogic() {
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        boolean brewed = stack.is(CompatAccess.naturalisItem("brewed_morph_potion"))
            || stack.is(CompatAccess.naturalisItem("brewed_morph_splash_potion"))
            || stack.is(CompatAccess.naturalisItem("brewed_morph_lingering_potion"));

        boolean binding = stack.is(CompatAccess.naturalisItem("morph_binding_potion"))
            || stack.is(CompatAccess.naturalisItem("morph_binding_splash_potion"))
            || stack.is(CompatAccess.naturalisItem("morph_binding_lingering_potion"));

        if (!brewed && !binding) {
            return;
        }

        int durationTicks = brewed ? readBrewedDuration(stack) : DEFAULT_BINDING_DURATION;
        String durationText = formatDuration(durationTicks);
        Component effect = brewed
            ? Component.translatable("effect.naturalis.brewed_morph")
            : Component.translatable("effect.naturalis.morph_binding");

        tooltip.add(Component.translatable("tooltip.naturalis.potion_effect", effect, durationText));
    }

    private static int readBrewedDuration(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (tag.contains("BrewedMorphDuration")) {
            return Math.max(20, CompatAccess.getInt(tag, "BrewedMorphDuration"));
        }
        if (tag.contains("MorphDuration")) {
            return Math.max(20, CompatAccess.getInt(tag, "MorphDuration"));
        }
        if (tag.contains("EffectDuration")) {
            return Math.max(20, CompatAccess.getInt(tag, "EffectDuration"));
        }

        return DEFAULT_BREWED_DURATION;
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = Mth.floor(Math.max(0, ticks) / 20.0D);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
