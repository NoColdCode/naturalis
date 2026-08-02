package dev.naturalis.experience;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-save experience choice persisted in the world folder ({@code naturalis_experience.dat}).
 */
public final class NaturalisWorldExperienceStorage {

    private static final String FILE_NAME = "naturalis_experience.dat";
    private static final String TAG_MODE = "mode";
    private static final String TAG_CHOSEN = "chosen";

    private static NaturalisExperienceMode cachedMode = NaturalisExperienceMode.UNSET;
    private static boolean cachedChosen;

    private NaturalisWorldExperienceStorage() {
    }

    public static void load(MinecraftServer server) {
        Path path = worldFile(server);
        if (!Files.isRegularFile(path)) {
            cachedMode = NaturalisExperienceMode.UNSET;
            cachedChosen = false;
            return;
        }
        try {
            CompoundTag tag = NbtIo.read(path);
            cachedMode = NaturalisExperienceMode.fromId(CompatAccess.getByte(tag, TAG_MODE));
            cachedChosen = CompatAccess.getBoolean(tag, TAG_CHOSEN);
        } catch (IOException ignored) {
            cachedMode = NaturalisExperienceMode.UNSET;
            cachedChosen = false;
        }
    }

    public static NaturalisExperienceMode getMode() {
        return cachedMode;
    }

    public static boolean isChosen() {
        return cachedChosen;
    }

    public static boolean shouldPrompt() {
        return !cachedChosen;
    }

    public static void setMode(MinecraftServer server, NaturalisExperienceMode mode) {
        cachedMode = mode;
        cachedChosen = true;
        persist(server);
    }

    private static void persist(MinecraftServer server) {
        Path path = worldFile(server);
        try {
            Files.createDirectories(path.getParent());
            CompoundTag tag = new CompoundTag();
            tag.putByte(TAG_MODE, cachedMode.id());
            tag.putBoolean(TAG_CHOSEN, cachedChosen);
            NbtIo.write(tag, path);
        } catch (IOException ignored) {
        }
    }

    private static Path worldFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }
}
