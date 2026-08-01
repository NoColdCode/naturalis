package dev.naturalis.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.naturalis.NaturalisMod;
import dev.naturalis.diet.DietManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Parses mob profile / archetype JSON into {@link MobProfileData.Builder}. */
public final class MobProfileParser {

    private MobProfileParser() {
    }

    public static MobProfileData.Builder parse(ResourceLocation sourceId, JsonObject root) {
        ResourceLocation entityId = readEntityId(sourceId, root);
        MobProfileData.Builder builder = MobProfileData.builder(entityId);

        if (root.has("inherits")) {
            // Resolved by MobProfileRegistry.mergeInherits().
        }

        if (root.has("diet")) {
            builder.diet(parseDiet(root.get("diet").getAsString()));
        }
        if (root.has("mass")) {
            builder.mass(root.get("mass").getAsDouble());
        }

        if (root.has("instinct")) {
            parseInstinct(root.getAsJsonObject("instinct"), builder);
        }
        if (root.has("environment")) {
            parseEnvironment(root.getAsJsonObject("environment"), builder);
        }
        if (root.has("biome_suitability")) {
            parseBiomeSuitability(root.getAsJsonObject("biome_suitability"), builder);
        }
        if (root.has("vision")) {
            parseVision(root.getAsJsonObject("vision"), builder);
        }
        if (root.has("quick_slot")) {
            parseQuickSlot(root.getAsJsonObject("quick_slot"), builder);
        }
        if (root.has("resonance")) {
            JsonObject resonance = root.getAsJsonObject("resonance");
            if (resonance.has("archetype")) {
                builder.resonanceArchetype(resonance.get("archetype").getAsString());
            }
        }
        if (root.has("tags")) {
            builder.tags(readStringSet(root.getAsJsonArray("tags")));
        }

        return builder;
    }

    public static ResourceLocation readInherits(JsonObject root) {
        if (!root.has("inherits")) {
            return null;
        }
        return ResourceLocation.tryParse(root.get("inherits").getAsString());
    }

    private static ResourceLocation readEntityId(ResourceLocation sourceId, JsonObject root) {
        if (root.has("entity")) {
            ResourceLocation entity = ResourceLocation.tryParse(root.get("entity").getAsString());
            if (entity == null) {
                throw new JsonParseException("Invalid entity id in " + sourceId);
            }
            return entity;
        }
        return sourceId;
    }

    private static void parseInstinct(JsonObject instinct, MobProfileData.Builder builder) {
        if (instinct.has("wander")) {
            builder.wander(instinct.get("wander").getAsBoolean());
        }
        if (instinct.has("flight_only")) {
            builder.flightOnly(instinct.get("flight_only").getAsBoolean());
        }
        if (instinct.has("static")) {
            builder.staticMorph(instinct.get("static").getAsBoolean());
        }
        if (instinct.has("floating")) {
            builder.floating(instinct.get("floating").getAsBoolean());
        }
        if (instinct.has("walk_speed")) {
            builder.walkSpeed(instinct.get("walk_speed").getAsDouble());
        }
        if (instinct.has("smell_strength")) {
            builder.smellStrength(instinct.get("smell_strength").getAsInt());
        }
        if (instinct.has("nyctalop_hostile")) {
            builder.nyctalopHostile(instinct.get("nyctalop_hostile").getAsBoolean());
        }
        if (instinct.has("fears")) {
            builder.fears(readStringSet(instinct.getAsJsonArray("fears")));
        }
        if (instinct.has("hunt_prey")) {
            builder.huntPreyPaths(readPreyPaths(instinct.getAsJsonArray("hunt_prey")));
        }
    }

    private static void parseEnvironment(JsonObject env, MobProfileData.Builder builder) {
        if (env.has("cold_vulnerable")) builder.coldVulnerable(env.get("cold_vulnerable").getAsBoolean());
        if (env.has("hot_vulnerable")) builder.hotVulnerable(env.get("hot_vulnerable").getAsBoolean());
        if (env.has("wet_vulnerable")) builder.wetVulnerable(env.get("wet_vulnerable").getAsBoolean());
        if (env.has("dry_vulnerable")) builder.dryVulnerable(env.get("dry_vulnerable").getAsBoolean());
        if (env.has("sunlight_sensitive")) builder.sunlightSensitive(env.get("sunlight_sensitive").getAsBoolean());
    }

    private static void parseBiomeSuitability(JsonObject bio, MobProfileData.Builder builder) {
        if (bio.has("volcanic")) builder.volcanicAdapted(bio.get("volcanic").getAsBoolean());
        if (bio.has("snow_adapted")) builder.snowAdapted(bio.get("snow_adapted").getAsBoolean());
        if (bio.has("ender_adapted")) builder.enderAdapted(bio.get("ender_adapted").getAsBoolean());
        if (bio.has("cave_adapted")) builder.caveAdapted(bio.get("cave_adapted").getAsBoolean());
    }

    private static void parseVision(JsonObject vision, MobProfileData.Builder builder) {
        if (vision.has("shader")) {
            builder.visionShader(ResourceLocation.tryParse(vision.get("shader").getAsString()));
        }
        if (vision.has("chromatic_mode")) {
            builder.chromaticMode(vision.get("chromatic_mode").getAsString());
        }
        if (vision.has("kaleido_strength")) {
            builder.kaleidoStrength(vision.get("kaleido_strength").getAsDouble());
        }
        if (vision.has("kaleido_folds")) {
            builder.kaleidoFolds(vision.get("kaleido_folds").getAsInt());
        }
        if (vision.has("spectral_profile")) {
            builder.spectralProfile(vision.get("spectral_profile").getAsDouble());
        }
        if (vision.has("motion_trail")) {
            builder.motionTrail(vision.get("motion_trail").getAsDouble());
        }
        if (vision.has("canine_predator")) {
            builder.caninePredator(vision.get("canine_predator").getAsBoolean());
        }
    }

    private static void parseQuickSlot(JsonObject quickSlot, MobProfileData.Builder builder) {
        if (quickSlot.has("categories")) {
            EnumSet<MorphQuickSlotCategory> categories = EnumSet.noneOf(MorphQuickSlotCategory.class);
            for (JsonElement element : quickSlot.getAsJsonArray("categories")) {
                categories.add(parseQuickSlotCategory(element.getAsString()));
            }
            builder.quickSlotCategories(categories);
        }
        if (quickSlot.has("primary")) {
            builder.quickSlotPrimary(parseQuickSlotCategory(quickSlot.get("primary").getAsString()));
        }
    }

    public static DietManager.DietType parseDiet(String raw) {
        try {
            return DietManager.DietType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Unknown diet type: " + raw);
        }
    }

    private static MorphQuickSlotCategory parseQuickSlotCategory(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "ground" -> MorphQuickSlotCategory.GROUND;
            case "aerial", "air", "flying" -> MorphQuickSlotCategory.AERIAL;
            case "aquatic", "water" -> MorphQuickSlotCategory.AQUATIC;
            case "nether" -> MorphQuickSlotCategory.NETHER;
            case "hostile" -> MorphQuickSlotCategory.HOSTILE;
            case "high_damage", "boss" -> MorphQuickSlotCategory.HIGH_DAMAGE;
            // Passive / ambient wildlife maps to the ground wheel slot.
            case "ambient", "passive" -> MorphQuickSlotCategory.GROUND;
            default -> throw new JsonParseException("Unknown quick_slot category: " + raw);
        };
    }

    private static Set<String> readStringSet(JsonArray array) {
        Set<String> out = new HashSet<>();
        for (JsonElement element : array) {
            out.add(element.getAsString().trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static Set<String> readPreyPaths(JsonArray array) {
        Set<String> out = new HashSet<>();
        for (JsonElement element : array) {
            String raw = element.getAsString().trim();
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null) {
                out.add(id.getPath());
            } else {
                out.add(raw);
            }
        }
        return out;
    }

    public static ResourceLocation archetypeIdFromFile(ResourceLocation fileId) {
        if (!NaturalisMod.ID.equals(fileId.getNamespace())) {
            return fileId;
        }
        return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, fileId.getPath());
    }
}
