package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.render.EchoSovereignRenderer;
import dev.naturalis.client.render.MorphBeaconRenderer;
import dev.naturalis.client.screen.EchoForgeScreen;
import dev.naturalis.client.screen.MorphBeaconScreen;
import dev.naturalis.client.screen.MorphArmorForgeScreen;
import dev.naturalis.client.screen.MorphKnowledgeScreen;
import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisEntityTypes;
import dev.naturalis.content.NaturalisMenus;
import dev.naturalis.network.CurlSleepTogglePayload;
import dev.naturalis.network.MorphMovementKeyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NaturalisClient {

    public static final KeyMapping OPEN_KNOWLEDGE_KEY = new KeyMapping(
        "key.naturalis.open_knowledge",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.naturalis"
    );

    public static final KeyMapping RESONANCE_INSTINCT_KEY = new KeyMapping(
        "key.naturalis.resonance_instinct",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.naturalis"
    );

    public static final KeyMapping CURL_SLEEP_KEY = new KeyMapping(
        "key.naturalis.curl_sleep",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        "key.categories.naturalis"
    );

    public static final KeyMapping PRIMAL_MOVEMENT_KEY = new KeyMapping(
        "key.naturalis.primal_movement",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.naturalis"
    );

    private NaturalisClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NaturalisMenus.ECHO_FORGE.get(), EchoForgeScreen::new);
        event.register(NaturalisMenus.MORPH_KNOWLEDGE.get(), MorphKnowledgeScreen::new);
        event.register(NaturalisMenus.MORPH_ARMOR_FORGE.get(), MorphArmorForgeScreen::new);
        event.register(NaturalisMenus.MORPH_BEACON.get(), MorphBeaconScreen::new);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NaturalisBlockEntities.MORPH_BEACON.get(), MorphBeaconRenderer::new);
        event.registerEntityRenderer(NaturalisEntityTypes.ECHO_SOVEREIGN.get(), EchoSovereignRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addRenderLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(model);
            if (renderer != null) {
                renderer.addLayer(new MorphArmorRenderLayer(renderer));
            }
        }

        // WoodWalkers renders morphed players through the shape's own renderer, not PlayerRenderer.
        // Attach this layer to all living renderers so morphs also display morph armor.
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) {
                continue;
            }

            EntityRenderer<?> baseRenderer = event.getRenderer(type);
            if (baseRenderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new MorphArmorRenderLayer(livingRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KNOWLEDGE_KEY);
        event.register(RESONANCE_INSTINCT_KEY);
        event.register(CURL_SLEEP_KEY);
        event.register(PRIMAL_MOVEMENT_KEY);
    }

    @EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
    public static final class InputEvents {

        private static boolean lastPrimalMovementDown;

        private InputEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player == null) {
                lastPrimalMovementDown = false;
                dev.naturalis.client.perception.MorphMotionVisionClient.setPrimalKeyDown(false);
                return;
            }

            MorphTextRefreshClientEvents.tick();
            dev.naturalis.client.perception.MorphSniffClientState.tick();
            ScentTrailClient.tick();

            while (OPEN_KNOWLEDGE_KEY.consumeClick()) {
                net.minecraft.client.Minecraft.getInstance().player.connection.sendCommand("morph knowledge");
            }

            while (RESONANCE_INSTINCT_KEY.consumeClick()) {
                net.minecraft.client.Minecraft.getInstance().player.connection.sendCommand("morph resonance instinct");
            }

            while (CURL_SLEEP_KEY.consumeClick()) {
                var connection = minecraft.getConnection();
                if (connection != null) {
                    connection.send(new CurlSleepTogglePayload());
                }
            }

            boolean primalMovementDown = !MorphQuickSlotClient.shouldBlockTransformKey() && PRIMAL_MOVEMENT_KEY.isDown();
            dev.naturalis.client.perception.MorphMotionVisionClient.setPrimalKeyDown(primalMovementDown);
            if (primalMovementDown != lastPrimalMovementDown) {
                var connection = minecraft.getConnection();
                if (connection != null) {
                    connection.send(new MorphMovementKeyPayload(primalMovementDown));
                }
                lastPrimalMovementDown = primalMovementDown;
            }
        }
    }
}
