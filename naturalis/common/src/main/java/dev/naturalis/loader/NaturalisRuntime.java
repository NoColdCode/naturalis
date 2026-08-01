package dev.naturalis.loader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/** Loader-neutral paths and glue — NeoForge sets these from {@code Naturalis}, Fabric from {@code NaturalisFabricEntrypoint}. */
public final class NaturalisRuntime {

    private static volatile Supplier<Path> configDirectory = () -> Paths.get("config");

    private NaturalisRuntime() {
    }

    public static void setConfigDirectory(Supplier<Path> supplier) {
        configDirectory = supplier != null ? supplier : () -> Paths.get("config");
    }

    public static Path getConfigDirectory() {
        return configDirectory.get();
    }
}
