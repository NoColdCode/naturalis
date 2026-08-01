package dev.naturalis.profile;

import com.google.gson.JsonObject;
import dev.naturalis.NaturalisMod;
import dev.naturalis.diet.DietManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Central lookup for datapack-driven mob profiles and archetypes. */
public final class MobProfileRegistry {

    private static final Map<ResourceLocation, MobProfileData> RESOLVED = new HashMap<>();
    private static final Map<ResourceLocation, JsonObject> RAW_ARCHETYPES = new HashMap<>();
    private static final Map<ResourceLocation, JsonObject> RAW_PROFILES = new HashMap<>();
    private static final Map<String, JsonObject> RAW_NAMESPACE_DEFAULTS = new HashMap<>();

    private MobProfileRegistry() {
    }

    public static void applyLoadedData(MobProfileLoader.LoadedMobProfileData loaded) {
        RAW_ARCHETYPES.clear();
        RAW_PROFILES.clear();
        RAW_NAMESPACE_DEFAULTS.clear();
        RESOLVED.clear();

        RAW_ARCHETYPES.putAll(loaded.archetypes());
        RAW_PROFILES.putAll(loaded.profiles());
        RAW_NAMESPACE_DEFAULTS.putAll(loaded.namespaceDefaults());

        Set<ResourceLocation> entityIds = new HashSet<>();
        for (JsonObject profile : RAW_PROFILES.values()) {
            if (profile.has("entity")) {
                ResourceLocation entity = ResourceLocation.tryParse(profile.get("entity").getAsString());
                if (entity != null) {
                    entityIds.add(entity);
                }
            }
        }
        for (Map.Entry<ResourceLocation, JsonObject> entry : RAW_PROFILES.entrySet()) {
            if (!entry.getValue().has("entity")) {
                entityIds.add(entry.getKey());
            }
        }
        for (Map.Entry<String, JsonObject> entry : RAW_NAMESPACE_DEFAULTS.entrySet()) {
            JsonObject defaults = entry.getValue();
            if (defaults.has("overrides")) {
                String ns = defaults.has("namespace") ? defaults.get("namespace").getAsString() : entry.getKey();
                for (String path : defaults.getAsJsonObject("overrides").keySet()) {
                    entityIds.add(ResourceLocation.fromNamespaceAndPath(ns, path));
                }
            }
        }

        for (ResourceLocation entityId : entityIds) {
            resolveEntity(entityId).ifPresent(profile -> RESOLVED.put(entityId, profile));
        }
    }

    public static Optional<MobProfileData> get(ResourceLocation morphId) {
        if (morphId == null) {
            return Optional.empty();
        }
        MobProfileData cached = RESOLVED.get(morphId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<MobProfileData> resolved = resolveEntity(morphId);
        resolved.ifPresent(profile -> RESOLVED.put(morphId, profile));
        return resolved;
    }

    public static boolean hasProfile(ResourceLocation morphId) {
        return get(morphId).isPresent();
    }

    public static Optional<DietManager.DietType> getDiet(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::diet);
    }

    public static Optional<Double> getMass(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::mass);
    }

    public static Optional<Boolean> getWander(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::wander);
    }

    public static Optional<Boolean> getFlightOnly(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::flightOnly);
    }

    public static Optional<Boolean> getStaticMorph(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::staticMorph);
    }

    public static Optional<Boolean> getFloating(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::floating);
    }

    public static Optional<Double> getWalkSpeed(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::walkSpeed);
    }

    public static Optional<Integer> getSmellStrength(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::smellStrength);
    }

    public static Optional<Boolean> getNyctalopHostile(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::nyctalopHostile);
    }

    public static Optional<Set<String>> getHuntPrey(ResourceLocation morphId) {
        return get(morphId).map(MobProfileData::huntPreyPaths).filter(set -> !set.isEmpty());
    }

    public static boolean hasFear(ResourceLocation morphId, String fearId) {
        return get(morphId).map(profile -> profile.hasFear(fearId)).orElse(false);
    }

    public static Optional<Boolean> getColdVulnerable(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::coldVulnerable);
    }

    public static Optional<Boolean> getHotVulnerable(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::hotVulnerable);
    }

    public static Optional<Boolean> getWetVulnerable(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::wetVulnerable);
    }

    public static Optional<Boolean> getDryVulnerable(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::dryVulnerable);
    }

    public static Optional<Boolean> getSunlightSensitive(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::sunlightSensitive);
    }

    public static Optional<ResourceLocation> getVisionShader(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::visionShader);
    }

    public static Optional<String> getChromaticMode(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::chromaticMode);
    }

    public static Optional<Double> getKaleidoStrength(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::kaleidoStrength);
    }

    public static Optional<Integer> getKaleidoFolds(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::kaleidoFolds);
    }

    public static Optional<Double> getSpectralProfile(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::spectralProfile);
    }

    public static Optional<Double> getMotionTrail(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::motionTrail);
    }

    public static Optional<Boolean> getCaninePredator(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::caninePredator);
    }

    public static Optional<Set<MorphQuickSlotCategory>> getQuickSlotCategories(ResourceLocation morphId) {
        return get(morphId).map(MobProfileData::quickSlotCategories).filter(set -> !set.isEmpty());
    }

    public static Optional<MorphQuickSlotCategory> getQuickSlotPrimary(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::quickSlotPrimary);
    }

    public static boolean isVolcanicMorph(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::volcanicAdapted).orElse(false);
    }

    public static boolean isSnowAdaptedMorph(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::snowAdapted).orElse(false);
    }

    public static boolean isEnderAdaptedMorph(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::enderAdapted).orElse(false);
    }

    public static boolean isCaveAdaptedMorph(ResourceLocation morphId) {
        return get(morphId).flatMap(MobProfileData::caveAdapted).orElse(false);
    }

    public static Set<String> getTags(ResourceLocation morphId) {
        return get(morphId).map(MobProfileData::tags).orElse(Collections.emptySet());
    }

    private static Optional<MobProfileData> resolveEntity(ResourceLocation entityId) {
        MobProfileData.Builder builder = MobProfileData.builder(entityId);

        JsonObject namespaceDefaults = RAW_NAMESPACE_DEFAULTS.get(entityId.getNamespace());
        if (namespaceDefaults != null && namespaceDefaults.has("default_inherits")) {
            mergeInherits(builder, ResourceLocation.tryParse(namespaceDefaults.get("default_inherits").getAsString()));
        }

        JsonObject explicit = findRawProfile(entityId);
        if (explicit != null) {
            mergeJson(builder, entityId, explicit);
        } else if (namespaceDefaults != null && namespaceDefaults.has("overrides")) {
            JsonObject overrides = namespaceDefaults.getAsJsonObject("overrides");
            if (overrides.has(entityId.getPath())) {
                JsonObject override = overrides.getAsJsonObject(entityId.getPath());
                mergeJson(builder, entityId, override);
            }
        }

        MobProfileData built = builder.build();
        if (built.diet().isEmpty()
            && built.mass().isEmpty()
            && built.wander().isEmpty()
            && built.flightOnly().isEmpty()
            && built.visionShader().isEmpty()
            && built.fears().isEmpty()
            && built.huntPreyPaths().isEmpty()
            && built.tags().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(built);
    }

    private static JsonObject findRawProfile(ResourceLocation entityId) {
        JsonObject direct = RAW_PROFILES.get(entityId);
        if (direct != null) {
            return direct;
        }
        for (JsonObject profile : RAW_PROFILES.values()) {
            if (profile.has("entity")) {
                ResourceLocation listed = ResourceLocation.tryParse(profile.get("entity").getAsString());
                if (entityId.equals(listed)) {
                    return profile;
                }
            }
        }
        return null;
    }

    private static void mergeJson(MobProfileData.Builder builder, ResourceLocation entityId, JsonObject json) {
        ResourceLocation inherits = MobProfileParser.readInherits(json);
        if (inherits != null) {
            mergeInherits(builder, inherits);
        }
        builder.mergeFrom(MobProfileParser.parse(entityId, json).build());
    }

    private static void mergeInherits(MobProfileData.Builder builder, ResourceLocation inheritsId) {
        if (inheritsId == null) {
            return;
        }
        Set<ResourceLocation> chain = new HashSet<>();
        ResourceLocation current = inheritsId;
        while (current != null && chain.add(current)) {
            JsonObject archetype = RAW_ARCHETYPES.get(current);
            if (archetype == null) {
                archetype = RAW_ARCHETYPES.get(ResourceLocation.fromNamespaceAndPath(
                    NaturalisMod.ID, current.getPath().replace('/', '_')));
            }
            if (archetype == null) {
                break;
            }
            ResourceLocation parentInherits = MobProfileParser.readInherits(archetype);
            builder.mergeFrom(MobProfileParser.parse(current, archetype).build());
            current = parentInherits;
        }
    }
}
