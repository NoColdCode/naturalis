package dev.naturalis.client;

import dev.naturalis.config.NaturalisConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only preferences persisted under {@code config/naturalis-client.json}.
 */
public final class NaturalisClientPrefs {

    private static final String KEY_MUTE = "muteMorphPerceptionSounds";

    private static volatile boolean muteMorphPerceptionSounds;
    private static volatile boolean loaded;

    private NaturalisClientPrefs() {
    }

    public static boolean isMuteMorphPerceptionSounds() {
        ensureLoaded();
        return muteMorphPerceptionSounds;
    }

    /** Called when {@code naturalis-client.toml} is loaded or reloaded. */
    public static void syncFromModConfig() {
        muteMorphPerceptionSounds = NaturalisConfig.clientMuteMorphPerceptionSounds();
        loaded = true;
    }

    public static void setMuteMorphPerceptionSounds(boolean value) {
        muteMorphPerceptionSounds = value;
        loaded = true;
        save();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (NaturalisClientPrefs.class) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }

    private static Path configPath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("config").resolve("naturalis-client.json");
    }

    private static void load() {
        try {
            muteMorphPerceptionSounds = NaturalisConfig.clientMuteMorphPerceptionSounds();
            return;
        } catch (IllegalStateException | ExceptionInInitializerError ignored) {
            // Mod config not ready yet; fall back to legacy JSON.
        }

        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            muteMorphPerceptionSounds = false;
            return;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (root.has(KEY_MUTE) && root.get(KEY_MUTE).isJsonPrimitive()) {
                muteMorphPerceptionSounds = root.get(KEY_MUTE).getAsBoolean();
            }
        } catch (Exception ignored) {
            muteMorphPerceptionSounds = false;
        }
    }

    private static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty(KEY_MUTE, muteMorphPerceptionSounds);
            Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
