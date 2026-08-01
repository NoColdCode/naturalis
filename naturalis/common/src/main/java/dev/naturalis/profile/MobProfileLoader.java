package dev.naturalis.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Shared datapack scan logic for {@link MobProfileReloadListener}. */
public final class MobProfileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobProfileLoader.class);

    private static final String ARCHETYPE_DIR = "mob_archetypes";
    private static final String PROFILE_DIR = "mob_profiles";
    private static final String NAMESPACE_DEFAULTS_DIR = "mob_namespace_defaults";

    private MobProfileLoader() {
    }

    public static LoadedMobProfileData loadAll(ResourceManager manager) {
        Map<ResourceLocation, JsonObject> archetypes = new HashMap<>();
        Map<ResourceLocation, JsonObject> profiles = new HashMap<>();
        Map<String, JsonObject> namespaceDefaults = new HashMap<>();

        for (String namespace : manager.getNamespaces()) {
            scanJsonDirectory(manager, namespace, ARCHETYPE_DIR, archetypes);
            scanJsonDirectory(manager, namespace, PROFILE_DIR, profiles);
            scanJsonDirectory(manager, namespace, NAMESPACE_DEFAULTS_DIR, (id, json) -> {
                if (json.has("namespace")) {
                    namespaceDefaults.put(json.get("namespace").getAsString(), json);
                } else {
                    namespaceDefaults.put(id.getNamespace(), json);
                }
            });
        }

        return new LoadedMobProfileData(archetypes, profiles, namespaceDefaults);
    }

    private static void scanJsonDirectory(
        ResourceManager manager,
        String namespace,
        String directory,
        Map<ResourceLocation, JsonObject> sink
    ) {
        scanJsonDirectory(manager, namespace, directory, (id, json) -> sink.put(id, json));
    }

    private static void scanJsonDirectory(
        ResourceManager manager,
        String namespace,
        String directory,
        JsonConsumer consumer
    ) {
        Map<ResourceLocation, Resource> resources = listProfileResources(manager, directory);
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            if (!namespace.equals(resourceId.getNamespace())) {
                continue;
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation logicalId = toLogicalId(namespace, directory, resourceId);
                consumer.accept(logicalId, json);
            } catch (IOException | RuntimeException ex) {
                LOGGER.error("[naturalis] Failed to load mob profile json {}: {}", resourceId, ex.toString());
            }
        }
    }

    private static ResourceLocation toLogicalId(String namespace, String directory, ResourceLocation resourceId) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        String suffix = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
        if (suffix.endsWith(".json")) {
            suffix = suffix.substring(0, suffix.length() - 5);
        }
        if (ARCHETYPE_DIR.equals(directory)) {
            return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, suffix.replace('/', '_'));
        }
        if (suffix.contains("/")) {
            String[] parts = suffix.split("/", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath(namespace, suffix);
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, Resource> listProfileResources(ResourceManager manager, String directory) {
        try {
            java.lang.reflect.Method listResources = ResourceManager.class.getMethod(
                "listResources", String.class, java.util.function.Predicate.class);
            Predicate<ResourceLocation> asResourceLocation =
                id -> id.getPath().endsWith(".json");
            try {
                return (Map<ResourceLocation, Resource>) listResources.invoke(manager, directory, asResourceLocation);
            } catch (IllegalArgumentException | java.lang.reflect.InvocationTargetException ignored) {
                Predicate<String> asString = path -> path.endsWith(".json");
                return (Map<ResourceLocation, Resource>) listResources.invoke(manager, directory, asString);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[naturalis] Failed to list mob profile resources in {}", directory, e);
            return Map.of();
        }
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(ResourceLocation id, JsonObject json);
    }

    public record LoadedMobProfileData(
        Map<ResourceLocation, JsonObject> archetypes,
        Map<ResourceLocation, JsonObject> profiles,
        Map<String, JsonObject> namespaceDefaults
    ) {
    }
}
