package dev.naturalis.profile;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** NeoForge 1.21.8 reload listener (4-arg {@link PreparableReloadListener}). */
public final class MobProfileReloadListener implements PreparableReloadListener {

    public static final MobProfileReloadListener INSTANCE = new MobProfileReloadListener();

    private MobProfileReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier barrier,
        ResourceManager manager,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> MobProfileLoader.loadAll(manager), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(MobProfileRegistry::applyLoadedData, gameExecutor);
    }
}
