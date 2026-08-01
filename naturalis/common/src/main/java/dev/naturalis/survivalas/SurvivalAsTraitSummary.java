package dev.naturalis.survivalas;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.diet.DietManager;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.metabolism.MetabolismManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the permanent-identity sheet shown when starting a Survival-as world.
 * Walkers traits are loaded reflectively so both {@code tocraft.walkers} (1.21.1)
 * and {@code dev.tocraft.walkers} (1.21.8) work without a hard compile dependency.
 */
public final class SurvivalAsTraitSummary {

    public record Sheet(
        ResourceLocation morphId,
        String entityNameKey,
        double mass,
        String dietId,
        List<String> traitIds,
        List<String> traitExtras
    ) {
    }

    private SurvivalAsTraitSummary() {
    }

    public static Sheet build(ServerPlayer player, ResourceLocation morphId) {
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        double mass = MetabolismManager.getMass(morphId);
        String diet = DietManager.getDietType(morphId).name().toLowerCase(Locale.ROOT);
        Map<String, String> traits = new LinkedHashMap<>();

        if (type != null && player.level() instanceof ServerLevel level) {
            Entity probe = CompatAccess.createEntity(type, level);
            if (probe instanceof LivingEntity living) {
                collectWalkersTraits(living, traits);
                living.discard();
            } else if (probe != null) {
                probe.discard();
            }
        }

        addHeuristicTraits(morphId, traits);

        List<String> ids = new ArrayList<>(traits.keySet());
        List<String> extras = new ArrayList<>(ids.size());
        for (String id : ids) {
            extras.add(traits.getOrDefault(id, ""));
        }
        return new Sheet(morphId, morphId.toString(), mass, diet, ids, extras);
    }

    @SuppressWarnings("unchecked")
    private static void collectWalkersTraits(LivingEntity living, Map<String, String> traits) {
        try {
            Class<?> registry = resolveTraitRegistry();
            if (registry == null) {
                return;
            }
            Method getAll = registry.getMethod("getAll", LivingEntity.class);
            Object listObj = getAll.invoke(null, living);
            if (!(listObj instanceof Iterable<?> list)) {
                return;
            }
            for (Object trait : list) {
                if (trait == null) {
                    continue;
                }
                Method getId = trait.getClass().getMethod("getId");
                Object id = getId.invoke(trait);
                String traitId;
                if (id instanceof ResourceLocation rl) {
                    traitId = rl.toString();
                } else if (id != null) {
                    traitId = id.toString();
                } else {
                    continue;
                }
                String extra = describeTraitEntities(trait);
                traits.merge(traitId, extra, SurvivalAsTraitSummary::mergeExtra);
            }
        } catch (Throwable ignored) {
            // Walkers absent / API drift — heuristics below.
        }
    }

    private static Class<?> resolveTraitRegistry() {
        for (String name : new String[]{
            "tocraft.walkers.traits.TraitRegistry",
            "dev.tocraft.walkers.traits.TraitRegistry"
        }) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static void addHeuristicTraits(ResourceLocation morphId, Map<String, String> traits) {
        String path = morphId.getPath();

        if (InstinctManager.hasSmellSense(morphId) || InstinctManager.isScentbound(morphId)) {
            traits.putIfAbsent("naturalis:scentbound", "");
        }
        if (InstinctManager.isPhotophobic(morphId)
            || path.contains("zombie") || path.contains("skeleton") || path.contains("phantom")) {
            traits.putIfAbsent("naturalis:photophobic", "");
        }
        if (InstinctManager.isNyctalopHostile(morphId)) {
            traits.putIfAbsent("naturalis:nyctalop", "");
        }
        if (InstinctManager.isStaticMorph(morphId)) {
            traits.putIfAbsent("naturalis:static", "");
        }
        if (InstinctManager.isFloatingMorph(morphId)) {
            traits.putIfAbsent("naturalis:floating", "");
        }
        if (InstinctManager.isFlightOnly(morphId)) {
            traits.putIfAbsent("walkers:flying", "");
        }
        if (InstinctManager.isHunterMorph(morphId)) {
            String prey = formatPreyPaths(InstinctManager.getHuntedPrey(morphId));
            traits.merge("naturalis:hunter", prey, SurvivalAsTraitSummary::mergeExtra);
        }
        if (InstinctManager.isWanderMorph(morphId)) {
            traits.putIfAbsent("naturalis:wanderer", "");
        }
        if (InstinctManager.isAquaticDiver(morphId)) {
            traits.putIfAbsent("walkers:aquatic", "");
        }
        if (InstinctManager.isUndrownableMorph(morphId)) {
            traits.putIfAbsent("walkers:undrownable", "");
        }
        if (InstinctManager.isCantSwimMorph(morphId)) {
            traits.putIfAbsent("walkers:cant_swim", "");
        }
        if (InstinctManager.isClimbMorph(morphId)) {
            traits.putIfAbsent("walkers:climb_blocks", "");
        }
        if (InstinctManager.isSlowFallingMorph(morphId)) {
            traits.putIfAbsent("walkers:slow_falling", "");
        }
        if (InstinctManager.isLavaWalker(morphId)) {
            traits.putIfAbsent("walkers:stand_on_fluid", "");
        }
        if (InstinctManager.fearsWater(morphId)) {
            traits.putIfAbsent("naturalis:survival_as.water_fear", "");
        }

        if (path.contains("zombie") || path.contains("skeleton") || path.contains("wither_skeleton")
            || path.contains("stray") || path.contains("husk") || path.contains("drowned")
            || path.contains("zoglin") || path.contains("phantom") || path.contains("zombified")) {
            traits.putIfAbsent("walkers:burn_in_daylight", "");
            traits.putIfAbsent("walkers:attack_for_health", "");
            traits.putIfAbsent("naturalis:survival_as.undead_healing", "");
            if (path.contains("zombie") || path.contains("drowned") || path.contains("husk")) {
                traits.putIfAbsent("naturalis:survival_as.villager_prey", "");
            }
        }
        if (path.contains("enderman")) {
            traits.putIfAbsent("naturalis:survival_as.water_fear", "");
        }
    }

    private static String mergeExtra(String existing, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return existing == null ? "" : existing;
        }
        if (existing == null || existing.isBlank()) {
            return incoming;
        }
        if (existing.contains(incoming)) {
            return existing;
        }
        return existing + ", " + incoming;
    }

    private static String formatPreyPaths(Set<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String path : paths) {
            ResourceLocation id = path.contains(":")
                ? ResourceLocation.tryParse(path)
                : ResourceLocation.fromNamespaceAndPath("minecraft", path);
            EntityType<?> type = id == null ? null : CompatAccess.getEntityType(id);
            if (type != null) {
                names.add(type.getDescription().getString());
            } else {
                names.add(prettifyPath(path.contains(":") ? path.substring(path.indexOf(':') + 1) : path));
            }
        }
        return String.join(", ", names);
    }

    private static String describeTraitEntities(Object trait) {
        Set<String> names = new LinkedHashSet<>();
        for (String fieldName : new String[]{
            "fearfulTypes", "reinforcementTypes", "hunterTypes", "types", "entityTypes"
        }) {
            appendEntityTypeNames(trait, fieldName, names);
        }
        for (String fieldName : new String[]{
            "fearfulTags", "reinforcementTags", "hunterTags", "tags"
        }) {
            appendTagNames(trait, fieldName, names);
        }
        for (String fieldName : new String[]{"fearfulClasses", "hunterClasses"}) {
            appendClassNames(trait, fieldName, names);
        }
        return String.join(", ", names);
    }

    @SuppressWarnings("unchecked")
    private static void appendEntityTypeNames(Object trait, String fieldName, Set<String> names) {
        Object value = readField(trait, fieldName);
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            if (entry instanceof EntityType<?> type) {
                names.add(type.getDescription().getString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendTagNames(Object trait, String fieldName, Set<String> names) {
        Object value = readField(trait, fieldName);
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            if (entry instanceof TagKey<?> tag) {
                names.add("#" + tag.location());
            }
        }
    }

    private static void appendClassNames(Object trait, String fieldName, Set<String> names) {
        Object value = readField(trait, fieldName);
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            if (entry instanceof Class<?> cls) {
                names.add(prettifyPath(cls.getSimpleName()));
            }
        }
    }

    private static Object readField(Object trait, String fieldName) {
        Class<?> c = trait.getClass();
        while (c != null && c != Object.class) {
            try {
                Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(trait);
            } catch (ReflectiveOperationException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static String prettifyPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
