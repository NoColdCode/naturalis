package dev.naturalis.profile;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Loads mob_archetypes and mob_profiles JSON from all datapacks. */
public final class MobProfileReloadListener implements PreparableReloadListener {

    public static final MobProfileReloadListener INSTANCE = new MobProfileReloadListener();

    private MobProfileReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier barrier,
        ResourceManager manager,
        ProfilerFiller preparationsProfiler,
        ProfilerFiller reloadProfiler,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> MobProfileLoader.loadAll(manager), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(MobProfileRegistry::applyLoadedData, gameExecutor);
    }
}
