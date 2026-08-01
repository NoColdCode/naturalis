package dev.naturalis.survivalas;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-save "Survival as…" mode: locked morph identity for the whole world.
 * Stored as {@code naturalis_survival_as.dat} in the world folder.
 */
public final class SurvivalAsWorldStorage {

    private static final String FILE_NAME = "naturalis_survival_as.dat";
    private static final String TAG_ENABLED = "enabled";
    private static final String TAG_LOCKED = "locked";
    private static final String TAG_MORPH = "morph";
    private static final String TAG_SPAWN_DONE = "spawn_done";
    private static final String TAG_LORE_SENT = "lore_sent";
    private static final String TAG_TRAITS_SHOWN = "traits_shown";

    private static boolean enabled;
    private static boolean locked = true;
    private static ResourceLocation morphId;
    private static boolean spawnDone;
    private static boolean loreSent;
    private static boolean traitsShown;

    private SurvivalAsWorldStorage() {
    }

    public static void load(MinecraftServer server) {
        Path path = worldFile(server);
        if (!Files.isRegularFile(path)) {
            resetCache();
            return;
        }
        try {
            CompoundTag tag = NbtIo.read(path);
            enabled = CompatAccess.getBoolean(tag, TAG_ENABLED);
            locked = tag.contains(TAG_LOCKED) ? CompatAccess.getBoolean(tag, TAG_LOCKED) : true;
            spawnDone = CompatAccess.getBoolean(tag, TAG_SPAWN_DONE);
            loreSent = CompatAccess.getBoolean(tag, TAG_LORE_SENT);
            traitsShown = CompatAccess.getBoolean(tag, TAG_TRAITS_SHOWN);
            String morph = CompatAccess.getString(tag, TAG_MORPH);
            morphId = morph.isEmpty() ? null : ResourceLocation.tryParse(morph);
            if (!tag.contains(TAG_ENABLED)) {
                // empty / corrupt file
                enabled = morphId != null;
            }
            if (morphId == null) {
                enabled = false;
            }
        } catch (IOException ignored) {
            resetCache();
        }
    }

    public static void enable(MinecraftServer server, ResourceLocation morph) {
        enabled = morph != null;
        locked = true;
        morphId = morph;
        spawnDone = false;
        loreSent = false;
        traitsShown = false;
        persist(server);
    }

    public static void disable(MinecraftServer server) {
        resetCache();
        persist(server);
    }

    public static void unlock(MinecraftServer server) {
        locked = false;
        persist(server);
    }

    public static void markSpawnDone(MinecraftServer server) {
        spawnDone = true;
        persist(server);
    }

    public static void markLoreSent(MinecraftServer server) {
        loreSent = true;
        persist(server);
    }

    public static void markTraitsShown(MinecraftServer server) {
        traitsShown = true;
        persist(server);
    }

    /** Swap locked morph without wiping spawn/lore flags; traits popup will show again. */
    public static void changeMorph(MinecraftServer server, ResourceLocation morph) {
        if (morph == null) {
            return;
        }
        enabled = true;
        locked = true;
        morphId = morph;
        traitsShown = false;
        persist(server);
    }

    public static boolean isEnabled() {
        return enabled && morphId != null;
    }

    public static boolean isLocked() {
        return isEnabled() && locked;
    }

    public static ResourceLocation getMorphId() {
        return morphId;
    }

    public static boolean isSpawnDone() {
        return spawnDone;
    }

    public static boolean isLoreSent() {
        return loreSent;
    }

    public static boolean isTraitsShown() {
        return traitsShown;
    }

    private static void resetCache() {
        enabled = false;
        locked = true;
        morphId = null;
        spawnDone = false;
        loreSent = false;
        traitsShown = false;
    }

    private static void persist(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Path path = worldFile(server);
        try {
            Files.createDirectories(path.getParent());
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(TAG_ENABLED, enabled);
            tag.putBoolean(TAG_LOCKED, locked);
            tag.putBoolean(TAG_SPAWN_DONE, spawnDone);
            tag.putBoolean(TAG_LORE_SENT, loreSent);
            tag.putBoolean(TAG_TRAITS_SHOWN, traitsShown);
            tag.putString(TAG_MORPH, morphId == null ? "" : morphId.toString());
            NbtIo.write(tag, path);
        } catch (IOException ignored) {
        }
    }

    private static Path worldFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
