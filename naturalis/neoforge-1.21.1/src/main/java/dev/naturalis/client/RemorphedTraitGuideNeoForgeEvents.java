package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.screen.MorphTraitGuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

/**
 * Remorphed's morph menu ({@code RemorphedScreen}) draws header widgets manually and never calls
 * {@code super.render()}, so a normal {@code Init.Post} button is invisible. We draw/click our own
 * "Traits Info" control, and also add a guide button on Remorphed's {@code ?} help screen (which
 * does call {@code super.render()}).
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class RemorphedTraitGuideNeoForgeEvents {

    private static final Component GUIDE_LABEL = Component.translatable("gui.naturalis.trait_guide.button");
    private static final Component GUIDE_TOOLTIP = Component.translatable("gui.naturalis.trait_guide.button.tooltip");
    private static final Component HELP_GUIDE_LABEL = Component.translatable("gui.naturalis.trait_guide.help_button");

    private static Button menuGuideButton;
    private static Screen menuGuideParent;

    private RemorphedTraitGuideNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        String name = screen.getClass().getName();

        if (isRemorphedHelpScreen(name)) {
            ensureTraitsVisible();
            int w = Math.min(180, Math.max(120, screen.width / 3));
            Button helpGuide = Button.builder(
                HELP_GUIDE_LABEL,
                b -> Minecraft.getInstance().setScreen(new MorphTraitGuideScreen(screen))
            )
                .bounds(screen.width / 2 - w / 2, 36, w, 20)
                .tooltip(Tooltip.create(GUIDE_TOOLTIP))
                .build();
            event.addListener(helpGuide);
            return;
        }

        if (!isRemorphedMorphMenu(name)) {
            return;
        }

        ensureTraitsVisible();
        menuGuideParent = screen;
        menuGuideButton = Button.builder(
            GUIDE_LABEL,
            b -> Minecraft.getInstance().setScreen(new MorphTraitGuideScreen(screen))
        )
            .bounds(0, 0, 88, 20)
            .tooltip(Tooltip.create(GUIDE_TOOLTIP))
            .build();
        layoutMenuGuideButton(screen);
        // Still register for focus/narration; Remorphed won't paint it — Render.Post does.
        event.addListener(menuGuideButton);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (menuGuideButton == null || menuGuideParent != screen || !isRemorphedMorphMenu(screen.getClass().getName())) {
            return;
        }
        layoutMenuGuideButton(screen);
        GuiGraphics graphics = event.getGuiGraphics();
        menuGuideButton.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || menuGuideButton == null || menuGuideParent != event.getScreen()) {
            return;
        }
        if (!isRemorphedMorphMenu(event.getScreen().getClass().getName())) {
            return;
        }
        layoutMenuGuideButton(event.getScreen());
        if (menuGuideButton.isMouseOver(event.getMouseX(), event.getMouseY())) {
            Minecraft.getInstance().setScreen(new MorphTraitGuideScreen(menuGuideParent));
            event.setCanceled(true);
        }
    }

    private static void layoutMenuGuideButton(Screen screen) {
        if (menuGuideButton == null) {
            return;
        }
        // Sit just left of Remorphed's Traits toggle (Traits is at width/2 + width/8 + 65).
        int specialOffset = hasSpecialShapeOffset() ? 30 : 0;
        int guiWidth = screen.width;
        int traitsX = (int) (guiWidth / 2.0F + guiWidth / 8.0F + 65.0F) + specialOffset;
        int buttonW = 88;
        int buttonX = Math.max(8, traitsX - buttonW - 6);
        int buttonY = 5;
        menuGuideButton.setX(buttonX);
        menuGuideButton.setY(buttonY);
        menuGuideButton.setWidth(buttonW);
    }

    private static boolean isRemorphedMorphMenu(String name) {
        String lower = name.toLowerCase();
        return lower.contains("remorphed")
            && (name.endsWith("RemorphedScreen") || name.endsWith("RemorphedMenu"));
    }

    private static boolean isRemorphedHelpScreen(String name) {
        String lower = name.toLowerCase();
        return lower.contains("remorphed") && lower.contains("help");
    }

    private static void ensureTraitsVisible() {
        for (String className : new String[]{"tocraft.remorphed.Remorphed", "dev.tocraft.remorphed.Remorphed"}) {
            try {
                Class<?> remorphed = Class.forName(className);
                for (String fieldName : new String[]{"displayTraitsInMenu", "displayDataInMenu"}) {
                    try {
                        Field field = remorphed.getField(fieldName);
                        field.setBoolean(null, true);
                        return;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static boolean hasSpecialShapeOffset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        for (String className : new String[]{"tocraft.walkers.Walkers", "dev.tocraft.walkers.Walkers"}) {
            try {
                Class<?> walkers = Class.forName(className);
                var method = walkers.getMethod("hasSpecialShape", java.util.UUID.class);
                Object result = method.invoke(null, mc.player.getUUID());
                return result instanceof Boolean b && b;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }
}
