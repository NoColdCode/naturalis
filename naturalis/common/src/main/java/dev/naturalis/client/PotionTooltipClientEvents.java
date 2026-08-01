package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class PotionTooltipClientEvents {

    private static final int DEFAULT_BREWED_DURATION = 20 * 60;
    private static final int DEFAULT_BINDING_DURATION = 8 * 20 * 60;

    private PotionTooltipClientEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        boolean brewed = stack.is(NaturalisItems.BREWED_MORPH_POTION.get())
            || stack.is(NaturalisItems.BREWED_MORPH_SPLASH_POTION.get())
            || stack.is(NaturalisItems.BREWED_MORPH_LINGERING_POTION.get());

        boolean binding = stack.is(NaturalisItems.MORPH_BINDING_POTION.get())
            || stack.is(NaturalisItems.MORPH_BINDING_SPLASH_POTION.get())
            || stack.is(NaturalisItems.MORPH_BINDING_LINGERING_POTION.get());

        if (!brewed && !binding) {
            return;
        }

        int durationTicks = brewed ? readBrewedDuration(stack) : DEFAULT_BINDING_DURATION;
        String durationText = formatDuration(durationTicks);
        Component effect = brewed
            ? Component.translatable("effect.naturalis.brewed_morph")
            : Component.translatable("effect.naturalis.morph_binding");

        event.getToolTip().add(Component.translatable("tooltip.naturalis.potion_effect", effect, durationText));
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
        return String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
