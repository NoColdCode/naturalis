package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.screen.SurvivalAsMobSelectScreen;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.survivalas.SurvivalAsClientCreateState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class SurvivalAsCreateWorldClientEvents {

    private SurvivalAsCreateWorldClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof CreateWorldScreen createWorld)) {
            return;
        }

        CycleButton<?> gameModeCycle = findGameModeCycle(event);
        if (gameModeCycle != null) {
            SurvivalAsCreateWorldGameModePatch.patch(gameModeCycle, createWorld);
        }

        int x = gameModeCycle != null ? gameModeCycle.getX() : screen.width / 2 - 105;
        int y = gameModeCycle != null
            ? gameModeCycle.getY() + (gameModeCycle.getHeight() + 8) * 3
            : screen.height / 2;
        int w = gameModeCycle != null ? gameModeCycle.getWidth() : 210;

        SurvivalAsMobPickButton pick = new SurvivalAsMobPickButton(x, y, w, 20, b -> {
            Minecraft.getInstance().setScreen(new SurvivalAsMobSelectScreen(screen));
        });
        // Start hidden for classic Survival; Render.Pre turns it on for Survival as…
        pick.visible = SurvivalAsClientCreateState.isModeSelected();
        event.addListener(pick);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof CreateWorldScreen createWorld)) {
            return;
        }

        boolean showPick = SurvivalAsClientCreateState.isModeSelected();
        CycleButton<?> gameModeCycle = null;

        for (GuiEventListener listener : screen.children()) {
            CycleButton<?> found = findGameModeCycle(listener);
            if (found != null) {
                gameModeCycle = found;
                SurvivalAsCreateWorldGameModePatch.syncFromUiState(found, createWorld);
            }
        }

        AbstractWidget anchor = findAllowCommandsCycle(screen);
        if (anchor == null) {
            anchor = gameModeCycle;
        }

        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof SurvivalAsMobPickButton pick)) {
                continue;
            }
            // Update BEFORE AbstractWidget.render skips invisible widgets.
            pick.visible = showPick;
            if (!showPick) {
                continue;
            }
            pick.setMessage(mobPickLabel());
            if (anchor != null) {
                pick.setX(anchor.getX());
                pick.setWidth(anchor.getWidth());
                pick.setY(anchor.getY() + anchor.getHeight() + 8);
            }
        }
    }

    private static CycleButton<?> findAllowCommandsCycle(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            CycleButton<?> found = findBooleanCycle(listener);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static CycleButton<?> findBooleanCycle(GuiEventListener listener) {
        if (listener instanceof CycleButton<?> cycle && cycle.getValue() instanceof Boolean) {
            return cycle;
        }
        if (listener instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                CycleButton<?> found = findBooleanCycle(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static CycleButton<?> findGameModeCycle(ScreenEvent.Init.Post event) {
        for (GuiEventListener listener : event.getListenersList()) {
            CycleButton<?> found = findGameModeCycle(listener);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static CycleButton<?> findGameModeCycle(GuiEventListener listener) {
        if (listener instanceof CycleButton<?> cycle) {
            if (SurvivalAsCreateWorldGameModePatch.isPatched(cycle)
                || SurvivalAsCreateWorldGameModePatch.isVanillaGameModeCycle(cycle)) {
                return cycle;
            }
            return null;
        }
        if (listener instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                CycleButton<?> found = findGameModeCycle(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Component mobPickLabel() {
        if (!SurvivalAsClientCreateState.isActive()) {
            return Component.translatable("gui.naturalis.survival_as.create_button")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
        }
        ResourceLocation id = SurvivalAsClientCreateState.getMorphId();
        EntityType<?> type = id == null ? null : CompatAccess.getEntityType(id);
        Component name = type != null ? type.getDescription() : Component.literal(id == null ? "?" : id.toString());
        return Component.translatable("gui.naturalis.survival_as.create_button_selected", name)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static final class SurvivalAsMobPickButton extends Button {
        SurvivalAsMobPickButton(int x, int y, int w, int h, OnPress onPress) {
            super(x, y, w, h, mobPickLabel(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setMessage(mobPickLabel());
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return SurvivalAsClientCreateState.isModeSelected() && super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
