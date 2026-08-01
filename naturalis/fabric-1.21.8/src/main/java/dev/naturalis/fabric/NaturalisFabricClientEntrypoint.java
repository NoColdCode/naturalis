package dev.naturalis.fabric;

import dev.naturalis.client.render.EchoSovereignRenderer;
import dev.naturalis.fabric.client.FabricNaturalisClientScreens;
import dev.naturalis.network.CurlSleepTogglePayload;
import dev.naturalis.network.MorphMovementKeyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class NaturalisFabricClientEntrypoint implements ClientModInitializer {

    private static final KeyMapping OPEN_KNOWLEDGE_KEY = new KeyMapping(
        "key.naturalis.open_knowledge",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.naturalis"
    );

    private static final KeyMapping RESONANCE_INSTINCT_KEY = new KeyMapping(
        "key.naturalis.resonance_instinct",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.naturalis"
    );

    private static final KeyMapping CURL_SLEEP_KEY = new KeyMapping(
        "key.naturalis.curl_sleep",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        "key.categories.naturalis"
    );

    private static final KeyMapping PRIMAL_MOVEMENT_KEY = new KeyMapping(
        "key.naturalis.primal_movement",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.naturalis"
    );

    private static boolean lastPrimalMovementDown;

    @Override
    public void onInitializeClient() {
        // Payload types are registered in NaturalisFabricEntrypoint (runs before client init); do not register twice.
        FabricNetworkBootstrap.registerClientHandlers();
        EntityRendererRegistry.register(FabricNaturalisEntityTypes.ECHO_SOVEREIGN, EchoSovereignRenderer::new);
        FabricNaturalisClientScreens.register();
        FabricVisionEvents.register();
        KeyBindingHelper.registerKeyBinding(OPEN_KNOWLEDGE_KEY);
        KeyBindingHelper.registerKeyBinding(RESONANCE_INSTINCT_KEY);
        KeyBindingHelper.registerKeyBinding(CURL_SLEEP_KEY);
        KeyBindingHelper.registerKeyBinding(PRIMAL_MOVEMENT_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(NaturalisFabricClientEntrypoint::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            lastPrimalMovementDown = false;
            return;
        }

        while (OPEN_KNOWLEDGE_KEY.consumeClick()) {
            client.getConnection().sendCommand("morph knowledge");
        }

        while (RESONANCE_INSTINCT_KEY.consumeClick()) {
            client.getConnection().sendCommand("morph resonance instinct");
        }

        while (CURL_SLEEP_KEY.consumeClick()) {
            ClientPlayNetworking.send(new CurlSleepTogglePayload());
        }

        boolean primalMovementDown = PRIMAL_MOVEMENT_KEY.isDown();
        if (primalMovementDown != lastPrimalMovementDown) {
            ClientPlayNetworking.send(new MorphMovementKeyPayload(primalMovementDown));
            lastPrimalMovementDown = primalMovementDown;
        }

        while (PRIMAL_MOVEMENT_KEY.consumeClick()) {
            client.getConnection().sendCommand("morph resonance status");
        }
    }
}
