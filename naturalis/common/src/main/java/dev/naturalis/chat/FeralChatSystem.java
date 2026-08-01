package dev.naturalis.chat;

import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Locale;
import java.util.Random;

public final class FeralChatSystem {

    private static final boolean ENABLE_CHAT_SOUND = true;
    private static final boolean ENABLE_SOLO_PREVIEW = true;
    private static final float CHAT_SOUND_VOLUME = 0.35F;

    private FeralChatSystem() {
    }

    public static void handleServerChat(ServerChatEvent event) {
        ServerPlayer speaker = event.getPlayer();
        ResourceLocation speakerMorph = CurrentMorphUtil.getCurrentMorphId(speaker);

        String raw = event.getRawText();
        if (raw == null || raw.isBlank()) {
            return;
        }

        event.setCanceled(true);

        String animalSpeech = speakerMorph != null ? toAnimalSpeech(speakerMorph, raw) : raw;
        SoundEvent chatSound = speakerMorph != null ? resolveChatSound(speakerMorph) : null;
        if (speaker.getServer() == null) {
            return;
        }

        for (ServerPlayer recipient : speaker.getServer().getPlayerList().getPlayers()) {
            ResourceLocation recipientMorph = CurrentMorphUtil.getCurrentMorphId(recipient);
            boolean hasTranslator = hasTranslationCore(recipient);

            String visibleText = raw;
            if (!hasTranslator) {
                if (speakerMorph != null) {
                    // Morphed speakers vocalize feral speech.
                    visibleText = animalSpeech;
                } else if (recipientMorph != null) {
                    // Morphed listeners without a translator struggle with human language.
                    visibleText = MorphComprehensionProfile.scrambleForMorph(recipientMorph, raw);
                }
            }

            MutableComponent message = Component.literal("<" + speaker.getName().getString() + "> ")
                .append(Component.literal(visibleText));

            if (!visibleText.equals(raw)) {
                message = message.withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
                if (ENABLE_CHAT_SOUND && chatSound != null) {
                    float pitch = 0.85F + ((Math.abs(raw.hashCode()) % 30) / 100.0F);
                    recipient.playNotifySound(chatSound, SoundSource.PLAYERS, CHAT_SOUND_VOLUME, pitch);
                }
            }

            recipient.sendSystemMessage(message);

            if (ENABLE_SOLO_PREVIEW
                && speakerMorph != null
                && recipient.getUUID().equals(speaker.getUUID())
                && speaker.getServer() != null
                && speaker.getServer().getPlayerList().getPlayerCount() == 1) {
                recipient.sendSystemMessage(
                    Component.literal("[Feral sound] " + visibleText)
                        .withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        }
    }

    private static boolean hasTranslationCore(ServerPlayer player) {
        return TranslationDeviceUtil.isTranslationCoreHeld(player);
    }

    private static String toAnimalSpeech(ResourceLocation morphId, String originalText) {
        String[] syllables = resolveSyllables(morphId.getPath());
        String normalized = originalText.trim();
        int visibleLen = normalized.replaceAll("\\s+", "").length();
        int tokenCount = Mth.clamp((visibleLen / 4) + 1, 1, 28);

        long seed = 31L * normalized.toLowerCase(Locale.ROOT).hashCode() + morphId.hashCode();
        Random random = new Random(seed);

        StringBuilder out = new StringBuilder();
        int phraseSize = 2 + random.nextInt(3);

        for (int i = 0; i < tokenCount; i++) {
            String token = syllables[random.nextInt(syllables.length)];
            if (i == 0 || (i % phraseSize == 0)) {
                token = Character.toUpperCase(token.charAt(0)) + token.substring(1);
            }

            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(token);

            boolean endPhrase = ((i + 1) % phraseSize == 0) || (i == tokenCount - 1);
            if (endPhrase) {
                out.append(choosePunctuation(originalText, random));
                phraseSize = 2 + random.nextInt(3);
            }
        }

        return out.toString();
    }

    private static char choosePunctuation(String original, Random random) {
        if (original.endsWith("?")) {
            return '?';
        }
        if (original.endsWith("!")) {
            return '!';
        }
        int value = random.nextInt(100);
        if (value < 28) {
            return '!';
        }
        if (value < 36) {
            return '?';
        }
        return '.';
    }

    private static String[] resolveSyllables(String mobPath) {
        String path = mobPath.toLowerCase(Locale.ROOT);

        if (containsAny(path, "wolf", "fox", "dog")) {
            return new String[] {"aroo", "woof", "ruff", "awoo", "grr"};
        }
        if (containsAny(path, "cat", "ocelot")) {
            return new String[] {"meow", "mrrp", "nya", "prr", "miau"};
        }
        if (containsAny(path, "cow", "mooshroom")) {
            return new String[] {"moo", "mrr", "moh", "humm"};
        }
        if (containsAny(path, "pig", "hoglin")) {
            return new String[] {"oink", "snort", "grunk", "hru"};
        }
        if (containsAny(path, "sheep", "goat")) {
            return new String[] {"baa", "meh", "maaa", "breh"};
        }
        if (containsAny(path, "chicken", "parrot")) {
            return new String[] {"cluck", "chirp", "pii", "kree"};
        }
        if (containsAny(path, "horse", "llama", "camel", "donkey", "mule")) {
            return new String[] {"neigh", "hrr", "snrr", "whee"};
        }
        if (containsAny(path, "frog", "axolotl", "fish", "salmon", "cod", "dolphin", "squid", "turtle")) {
            return new String[] {"blub", "plop", "glup", "bloop", "splash"};
        }
        if (containsAny(path, "spider", "silverfish", "endermite", "bee")) {
            return new String[] {"chrr", "tsk", "hiss", "krr", "zzzt"};
        }
        if (containsAny(path, "zombie", "husk", "drowned")) {
            return new String[] {"grra", "uurr", "braa", "gnn"};
        }
        if (containsAny(path, "skeleton", "stray", "wither")) {
            return new String[] {"clak", "ratt", "krrk", "tik"};
        }
        if (containsAny(path, "enderman", "warden")) {
            return new String[] {"vrmm", "hrnn", "whrr", "drone"};
        }
        if (containsAny(path, "blaze", "ghast", "magma", "strider")) {
            return new String[] {"fssh", "krak", "vrr", "hraa"};
        }
        if (containsAny(path, "slime")) {
            return new String[] {"splut", "blop", "slrp", "gloob"};
        }

        String compact = path.replace('_', ' ').trim();
        String[] generic = new String[] {"grr", "hrr", "rr", "chrr", "snff", "prr", "krr", "trr"};
        int pivot = Math.abs(compact.hashCode());
        return new String[] {
            generic[pivot % generic.length],
            generic[(pivot / 3) % generic.length],
            generic[(pivot / 7) % generic.length],
            generic[(pivot / 11) % generic.length]
        };
    }

    private static SoundEvent resolveChatSound(ResourceLocation morphId) {
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        if (containsAny(path, "wolf", "fox", "dog")) {
            return SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
        }
        if (containsAny(path, "cat", "ocelot")) {
            return SoundEvents.NOTE_BLOCK_CHIME.value();
        }
        if (containsAny(path, "cow", "mooshroom")) {
            return SoundEvents.NOTE_BLOCK_BASS.value();
        }
        if (containsAny(path, "sheep", "goat")) {
            return SoundEvents.NOTE_BLOCK_FLUTE.value();
        }
        if (containsAny(path, "pig", "hoglin")) {
            return SoundEvents.NOTE_BLOCK_BASEDRUM.value();
        }
        if (containsAny(path, "chicken", "parrot")) {
            return SoundEvents.NOTE_BLOCK_BIT.value();
        }
        if (containsAny(path, "horse", "llama", "camel", "donkey", "mule")) {
            return SoundEvents.NOTE_BLOCK_COW_BELL.value();
        }
        if (containsAny(path, "spider")) {
            return SoundEvents.NOTE_BLOCK_HAT.value();
        }
        if (containsAny(path, "skeleton", "stray", "wither")) {
            return SoundEvents.NOTE_BLOCK_SNARE.value();
        }
        if (containsAny(path, "zombie", "husk", "drowned")) {
            return SoundEvents.NOTE_BLOCK_BASS.value();
        }
        if (containsAny(path, "enderman", "warden")) {
            return SoundEvents.NOTE_BLOCK_BELL.value();
        }
        if (containsAny(path, "slime")) {
            return SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
        }
        if (containsAny(path, "blaze")) {
            return SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
        }
        if (containsAny(path, "ghast")) {
            return SoundEvents.NOTE_BLOCK_PLING.value();
        }
        if (containsAny(path, "frog", "axolotl", "fish", "salmon", "cod", "dolphin", "squid", "turtle")) {
            return SoundEvents.NOTE_BLOCK_HARP.value();
        }
        return SoundEvents.NOTE_BLOCK_HARP.value();
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