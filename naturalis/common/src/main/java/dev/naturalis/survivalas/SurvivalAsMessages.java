package dev.naturalis.survivalas;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class SurvivalAsMessages {

    private SurvivalAsMessages() {
    }

    public static MutableComponent firstSpawnLore(EntityType<?> type, ResourceLocation morphId) {
        Component name = type != null ? type.getDescription() : Component.literal(morphId.toString());
        return Component.translatable("message.naturalis.survival_as.lore", name)
            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC);
    }

    public static MutableComponent lockedNotice(EntityType<?> type, ResourceLocation morphId) {
        Component name = type != null ? type.getDescription() : Component.literal(morphId.toString());
        return Component.translatable("message.naturalis.survival_as.locked", name)
            .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent unlockNotice() {
        return Component.translatable("message.naturalis.survival_as.unlocked")
            .withStyle(ChatFormatting.GOLD);
    }

    public static MutableComponent cannotChange() {
        return Component.translatable("message.naturalis.survival_as.cannot_change")
            .withStyle(ChatFormatting.RED);
    }

    public static MutableComponent habitChanged(EntityType<?> type, ResourceLocation morphId) {
        Component name = type != null ? type.getDescription() : Component.literal(morphId.toString());
        return Component.translatable("message.naturalis.habit_chrysalis.changed", name)
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
    }
}
