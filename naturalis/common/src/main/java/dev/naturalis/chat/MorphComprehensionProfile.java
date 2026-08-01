package dev.naturalis.chat;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class MorphComprehensionProfile {

    public enum Literacy {
        CLEAR,
        PARTIAL,
        GARBLED
    }

    private static final Set<String> CLEAR_PATHS = Set.of(
        "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner", "witch",
        "piglin", "piglin_brute", "zombified_piglin", "enderman"
    );

    private static final Set<String> PARTIAL_PATHS = Set.of(
        "wolf", "fox", "cat", "ocelot", "dolphin", "parrot", "ravager", "warden",
        "spider", "cave_spider", "goat", "axolotl", "allay", "vex", "bee", "blaze", "ghast"
    );

    private static final char[] GARBLED_GLYPHS = new char[] {
        '#', '%', '&', '*', '?', '@', '!', ';', ':', '/', '+', '='
    };

    private MorphComprehensionProfile() {
    }

    public static Literacy getLiteracy(ResourceLocation morphId) {
        if (morphId == null) {
            return Literacy.CLEAR;
        }

        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        if (CLEAR_PATHS.contains(path)
            || containsAny(path, "villager", "trader", "illager", "piglin", "enderman", "human", "sage")) {
            return Literacy.CLEAR;
        }
        if (PARTIAL_PATHS.contains(path)
            || containsAny(path, "wolf", "fox", "cat", "ocelot", "spider", "raven", "owl", "bat", "hound", "canid", "feline")) {
            return Literacy.PARTIAL;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type != null) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MONSTER || category == MobCategory.CREATURE || category == MobCategory.AMBIENT) {
                return Literacy.PARTIAL;
            }
            if (category.getName().contains("water")) {
                return Literacy.PARTIAL;
            }
        }

        return Literacy.GARBLED;
    }

    public static String scrambleForMorph(ResourceLocation morphId, String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        Literacy literacy = getLiteracy(morphId);
        return switch (literacy) {
            case CLEAR -> message;
            case PARTIAL -> partial(message, morphId.hashCode());
            case GARBLED -> garbled(message, morphId.hashCode());
        };
    }

    public static boolean hasEnhancedHearing(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        return containsAny(path,
            "wolf", "fox", "cat", "ocelot", "bat", "spider", "cave_spider", "warden", "owl", "hound", "canid", "feline");
    }

    public static float hearingMultiplier(ResourceLocation morphId) {
        if (morphId == null) {
            return 1.0F;
        }
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        if (containsAny(path, "bat", "warden", "wolf", "fox")) {
            return 1.8F;
        }
        if (containsAny(path, "cat", "ocelot", "spider", "cave_spider")) {
            return 1.5F;
        }
        return hasEnhancedHearing(morphId) ? 1.3F : 1.0F;
    }

    private static String partial(String message, int seed) {
        Random random = new Random(0x6E617475L + seed + message.hashCode());
        StringBuilder out = new StringBuilder();
        String[] words = message.split(" ", -1);

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                if (i < words.length - 1) {
                    out.append(' ');
                }
                continue;
            }

            out.append(fragmentWord(word, random));
            if (i < words.length - 1) {
                out.append(' ');
            }
        }
        return out.toString();
    }

    private static String fragmentWord(String word, Random random) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                out.append(c);
                continue;
            }

            boolean keep = i == 0 || i == word.length() - 1 || random.nextFloat() < 0.34F;
            if (keep) {
                out.append(c);
            } else {
                out.append(random.nextBoolean() ? '.' : '_');
            }
        }
        return out.toString();
    }

    private static String garbled(String message, int seed) {
        Random random = new Random(0x70617468L + seed + message.hashCode());
        StringBuilder out = new StringBuilder(message.length());

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(c);
            } else if (!Character.isLetterOrDigit(c)) {
                out.append(c);
            } else {
                out.append(GARBLED_GLYPHS[random.nextInt(GARBLED_GLYPHS.length)]);
            }
        }

        return out.toString();
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
