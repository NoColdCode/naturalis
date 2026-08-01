package dev.naturalis.experience;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class NaturalisExperienceMessages {

    private NaturalisExperienceMessages() {
    }

    public static Component welcome() {
        return Component.translatable("message.naturalis.experience.welcome")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    public static Component chooseHint() {
        MutableComponent out = Component.empty();
        out.append(Component.translatable("message.naturalis.experience.choose_hint.line1")
            .withStyle(ChatFormatting.YELLOW));
        out.append(Component.literal("\n"));
        out.append(Component.translatable("message.naturalis.experience.choose_hint.line2")
            .withStyle(ChatFormatting.GRAY));
        out.append(Component.literal("\n\n"));
        out.append(chatButton(
            "message.naturalis.experience.button.choose",
            "/morph experience choose",
            ChatFormatting.GOLD
        ));
        out.append(Component.literal("  "));
        out.append(chatButton(
            "message.naturalis.experience.button.realistic",
            "/morph experience realistic",
            ChatFormatting.GREEN
        ));
        out.append(Component.literal("  "));
        out.append(chatButton(
            "message.naturalis.experience.button.softened",
            "/morph experience softened",
            ChatFormatting.AQUA
        ));
        out.append(Component.literal("\n"));
        out.append(Component.translatable("message.naturalis.experience.choose_hint.footer")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return out;
    }

    private static MutableComponent chatButton(String labelKey, String command, ChatFormatting color) {
        return Component.translatable(labelKey)
            .withStyle(buttonStyle(color, command));
    }

    private static Style buttonStyle(ChatFormatting color, String command) {
        return Style.EMPTY
            .withColor(color)
            .withBold(true)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Component.translatable("message.naturalis.experience.button.hover")
            ));
    }

    public static Component activeRealistic() {
        return Component.translatable("message.naturalis.experience.active_realistic")
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    }

    public static Component activeSoftened() {
        return Component.translatable("message.naturalis.experience.active_softened")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    public static Component chosen(NaturalisExperienceMode mode) {
        return Component.translatable(
                mode.isRealistic()
                    ? "message.naturalis.experience.chosen_realistic"
                    : "message.naturalis.experience.chosen_softened")
            .withStyle(mode.isRealistic() ? ChatFormatting.GREEN : ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    public static Component defaultApplied() {
        return Component.translatable("message.naturalis.experience.default_realistic")
            .withStyle(ChatFormatting.GOLD);
    }

    public static Component previewRealistic() {
        return Component.translatable("message.naturalis.experience.preview_realistic")
            .withStyle(ChatFormatting.GREEN);
    }

    public static Component previewSoftened() {
        return Component.translatable("message.naturalis.experience.preview_softened")
            .withStyle(ChatFormatting.AQUA);
    }

    public static Style titleStyle() {
        return Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true);
    }

    public static int highlightColor(NaturalisExperienceMode mode) {
        return mode.isRealistic() ? 0x55FF55 : 0x55AAFF;
    }
}
