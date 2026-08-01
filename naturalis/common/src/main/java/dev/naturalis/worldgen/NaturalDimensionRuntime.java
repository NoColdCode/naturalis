package dev.naturalis.worldgen;

import dev.naturalis.NaturalisMod;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.resonance.ResonanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;

public final class NaturalDimensionRuntime {

    private static final double SOVEREIGN_LOOKUP_RADIUS = 96.0D;

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalDimensionRuntime.class);

    private static final String NATURAL_PORTAL_COOLDOWN_KEY = "naturalis_portal_cooldown";
    private static final String NATURAL_PORTAL_TRANSFER_LOCK_KEY = "naturalis_portal_transfer_lock";
    /** Ignores portal collision until this game tick — avoids bounce when spawn lands inside portal blocks. */
    private static final String NATURAL_PORTAL_ARRIVAL_GRACE_UNTIL_KEY = "naturalis_portal_arrival_grace_until";
    private static final boolean PORTAL_DEBUG_LOGS = Boolean.parseBoolean(System.getProperty("naturalis.debug.portal", "false"));
    private static final long PORTAL_SLOW_WARN_MS = 200L;
    private static final int NATURAL_PORTAL_COOLDOWN_TICKS = 80;
    private static final int NATURAL_PORTAL_ARRIVAL_GRACE_TICKS = 200;
    private static final int NATURAL_HUMANITY_DRAIN_TICKS = 60 * 20;

    /** Undead mob types that would normally burn in sunlight and should be protected in the volcano biome. */
    private static final Set<String> VOLCANO_FIRE_IMMUNE_MOBS = Set.of(
        "zombie", "skeleton", "zombie_villager", "husk", "stray", "drowned", "phantom",
        "zombie_horse", "skeleton_horse", "zombified_piglin"
    );
    /**
     * Flags for bulk block placement during chunk generation.
     * UPDATE_CLIENTS (2): sends change to client.
     * UPDATE_KNOWN_SHAPE (16): suppresses updateNeighbourShapes — prevents cascading
     * neighbour-shape propagation during worldgen.
     */
    static final int GEN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    static final List<BlockPos> NATURAL_HUT_CENTERS = List.of(
        new BlockPos(420, 90, 0),
        new BlockPos(360, 90, 170),
        new BlockPos(250, 90, 280),
        new BlockPos(90, 90, 330),
        new BlockPos(-90, 90, 330),
        new BlockPos(-250, 90, 280),
        new BlockPos(-360, 90, 170),
        new BlockPos(-420, 90, 0),
        new BlockPos(-360, 90, -170),
        new BlockPos(-250, 90, -280),
        new BlockPos(90, 90, -330),
        new BlockPos(320, 90, -220)
    );

    private NaturalDimensionRuntime() {
    }

    public static void modifyMobLoot(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof Mob sovereign
            && entity.level() instanceof ServerLevel natLevel
            && EchoSovereignRuntime.isEchoSovereign(sovereign)) {
            EchoSovereignRuntime.onBossDrops(natLevel, sovereign, source);
            return;
        }

        EntityType<?> type = entity.getType();
        if ((type == EntityType.WITCH || type == EntityType.EVOKER)
            && !(entity instanceof Mob m && EchoSovereignRuntime.isEchoSovereign(m))) {
            double chance = 0.45D;
            if (entity.getRandom().nextDouble() <= chance) {
                ItemEntity keyEntity = new ItemEntity(
                    entity.level(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    new ItemStack(CompatAccess.naturalisItem("natural_sigil_key"))
                );
                drops.add(keyEntity);
            }
        }

        if (type == EntityType.WARDEN && entity.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            if (entity.getRandom().nextDouble() <= 0.025D) {
                ItemEntity starEntity = new ItemEntity(
                    entity.level(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    new ItemStack(CompatAccess.naturalisItem("natural_star"))
                );
                drops.add(starEntity);
            }
        }
    }

    public static boolean tryActivatePortal(Player player, Level level, BlockPos clickedPos, ItemStack stack, InteractionHand hand) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!stack.is(CompatAccess.naturalisItem("natural_sigil_key"))) {
            return false;
        }
        if (!level.getBlockState(clickedPos).is(CompatAccess.naturalisBlock("natural_portal_frame"))) {
            return false;
        }

        Direction[][] axes = new Direction[][] {
            {Direction.WEST, Direction.EAST},
            {Direction.NORTH, Direction.SOUTH}
        };

        for (Direction[] axis : axes) {
            if (!isPortalFrameValid(level, clickedPos, axis[0], axis[1])) {
                continue;
            }

            placePortalInterior(level, clickedPos);
            level.playSound(null, clickedPos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return true;
        }

        return false;
    }

    public static void handlePortalTravel(ServerPlayer player) {
        // Throttle collision handling to avoid repeated heavy work while standing in portal blocks.
        long gameTime = player.level().getGameTime();
        if ((gameTime & 3L) != 0L) {
            return;
        }

        debugPortal("handlePortalTravel called: player={} dim={} gameTime={} lock={} cooldown={}",
            player.getGameProfile().getName(),
            player.level().dimension().location(),
            gameTime,
            CompatAccess.getBoolean(CompatAccess.getPersistentData(player), NATURAL_PORTAL_TRANSFER_LOCK_KEY),
            CompatAccess.getLong(CompatAccess.getPersistentData(player), NATURAL_PORTAL_COOLDOWN_KEY));

        if (CompatAccess.getBoolean(CompatAccess.getPersistentData(player), NATURAL_PORTAL_TRANSFER_LOCK_KEY)) {
            debugPortal("skipped — transfer lock active");
            return;
        }

        long cooldownUntil = CompatAccess.getLong(CompatAccess.getPersistentData(player), NATURAL_PORTAL_COOLDOWN_KEY);
        if (gameTime < cooldownUntil) {
            debugPortal("skipped — cooldown active until {} (now {})", cooldownUntil, gameTime);
            return;
        }

        long arrivalGraceUntil = CompatAccess.getLong(CompatAccess.getPersistentData(player), NATURAL_PORTAL_ARRIVAL_GRACE_UNTIL_KEY);
        if (gameTime < arrivalGraceUntil) {
            debugPortal("skipped — post-teleport arrival grace until {} (now {})", arrivalGraceUntil, gameTime);
            return;
        }

        BlockPos feet = player.blockPosition();
        var feetBlock = player.level().getBlockState(feet);
        var aboveBlock = player.level().getBlockState(feet.above());
        net.minecraft.world.level.block.Block naturalPortal = CompatAccess.naturalisBlock("natural_portal");
        boolean inPortal = feetBlock.is(naturalPortal)
            || aboveBlock.is(naturalPortal);
        if (!inPortal) {
            return;
        }

        final long startNanos = System.nanoTime();
        final String trace = shortPlayerId(player) + "@" + gameTime;
        debugPortal("[{}] in-portal dimension={} pos={}", trace, player.level().dimension().location(), feet);

        // Guard against re-entrant calls while cross-dimension transfer is in flight.
        CompatAccess.getPersistentData(player).putBoolean(NATURAL_PORTAL_TRANSFER_LOCK_KEY, true);
        try {
            CompatAccess.getPersistentData(player).putLong(NATURAL_PORTAL_COOLDOWN_KEY, gameTime + NATURAL_PORTAL_COOLDOWN_TICKS);
            CompatAccess.getPersistentData(player).putLong(NATURAL_PORTAL_ARRIVAL_GRACE_UNTIL_KEY, gameTime + NATURAL_PORTAL_ARRIVAL_GRACE_TICKS);
            debugPortal("[{}] transfer-lock set, cooldown until {}", trace, gameTime + NATURAL_PORTAL_COOLDOWN_TICKS);

            if (player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
                var server = CompatAccess.getServer(player);
                if (server == null) {
                    debugPortal("[{}] abort: server is null", trace);
                    return;
                }
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld == null) {
                    debugPortal("[{}] abort: overworld is null", trace);
                    return;
                }
                BlockPos spawn = overworld.getSharedSpawnPos();
                int y = Math.max(-63, overworld.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()));
                debugPortal("[{}] natural->overworld pre-teleport target=({}, {}, {})", trace, spawn.getX() + 0.5D, y + 1.0D, spawn.getZ() + 0.5D);
                long tpStart = System.nanoTime();
                CompatAccess.teleportCrossDimension(player, overworld, spawn.getX() + 0.5D, y + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
                long tpMs = (System.nanoTime() - tpStart) / 1_000_000L;
                debugPortal("[{}] natural->overworld teleportCrossDimension returned in {}ms", trace, tpMs);
                return;
            }

            var server = CompatAccess.getServer(player);
            if (server == null) {
                debugPortal("[{}] abort: server is null", trace);
                return;
            }
            ServerLevel naturalLevel = server.getLevel(NaturalDimensionKeys.NATURAL_DIMENSION);
            if (naturalLevel == null) {
                debugPortal("[{}] abort: natural dimension level is null", trace);
                return;
            }

            BlockPos hutCenter = selectNaturalHut(player);
            double tx = hutCenter.getX() + 0.5D;
            double ty = hutCenter.getY() + 1.0D;   // use pre-defined safe Y
            double tz = hutCenter.getZ() + 0.5D;

            // --- Chunk status diagnostic ---
            ChunkPos destChunk = new ChunkPos(hutCenter);
            boolean destChunkLoaded = naturalLevel.hasChunk(destChunk.x, destChunk.z);
            int loadedCount = 0;
            int missingCount = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (naturalLevel.hasChunk(destChunk.x + dx, destChunk.z + dz)) loadedCount++;
                    else missingCount++;
                }
            }
            debugPortal("[{}] dest-chunk [{},{}] loaded={} nearby(7x7): loaded={} missing={}",
                trace, destChunk.x, destChunk.z, destChunkLoaded, loadedCount, missingCount);
            debugPortal("[{}] naturalLevel players={} chunkSource={}",
                trace, naturalLevel.players().size(),
                naturalLevel.getChunkSource().getLoadedChunksCount());

            debugPortal("[{}] overworld->natural pre-teleport target=({}, {}, {})", trace, tx, ty, tz);
            debugPortal("[{}] naturalLevel.isLoaded={} dimension={}", trace, naturalLevel != null, naturalLevel.dimension().location());
            long tpStart = System.nanoTime();
            CompatAccess.teleportCrossDimension(player, naturalLevel, tx, ty, tz, player.getYRot(), player.getXRot());
            long tpMs = (System.nanoTime() - tpStart) / 1_000_000L;
            debugPortal("[{}] overworld->natural teleportCrossDimension returned in {}ms", trace, tpMs);

            // --- Post-teleport diagnostics (off unless naturalis.debug.portal=true) ---
            if (PORTAL_DEBUG_LOGS) {
                final String portalTrace = trace;
                final MinecraftServer srv = naturalLevel.getServer();
                new Thread(() -> {
                    long[] delaysMs = {100, 500, 1000, 2000, 3000, 5000, 8000, 12000};
                    for (long delayMs : delaysMs) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            return;
                        }
                        srv.execute(() -> {
                            String dim = player.level().dimension().location().toString();
                            int loaded = (int) naturalLevel.getChunkSource().getLoadedChunksCount();
                            LOGGER.info("[naturalis-portal] [{}] server-alive t+{}ms: playerDim={} naturalLoadedChunks={} serverTick={}",
                                portalTrace, delayMs, dim, loaded, srv.getTickCount());
                        });
                    }
                }, "naturalis-post-teleport-pings").start();
            }
            // Schedule a follow-up log to confirm the player actually changed dimension
            // on the next server tick. If the log shows the player is still in the
            // overworld, the teleport packet was lost or the client failed to respawn.
            if (PORTAL_DEBUG_LOGS) {
                final ResourceLocation targetDim = NaturalDimensionKeys.NATURAL_DIMENSION.location();
                naturalLevel.getServer().execute(() -> {
                    String actualDim = player.level().dimension().location().toString();
                    if (!actualDim.equals(targetDim.toString())) {
                        LOGGER.warn("[naturalis-portal] [{}] DIMENSION MISMATCH after teleport: player is in {} (expected {})",
                            trace, actualDim, targetDim);
                    } else {
                        LOGGER.info("[naturalis-portal] [{}] dimension confirmed: {}", trace, actualDim);
                    }
                });
            }
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            CompatAccess.getPersistentData(player).putBoolean(NATURAL_PORTAL_TRANSFER_LOCK_KEY, false);
            if (PORTAL_DEBUG_LOGS) {
                if (elapsedMs >= PORTAL_SLOW_WARN_MS) {
                    LOGGER.warn("[naturalis-portal][{}] transfer complete in {} ms (slow)", trace, elapsedMs);
                } else {
                    LOGGER.info("[naturalis-portal][{}] transfer complete in {} ms", trace, elapsedMs);
                }
                startFreezeWatchdog(trace);
            }
        }
    }

    /**
     * After a portal transfer, monitors the render thread for signs of a
     * client-side freeze.  Dumps a stack trace every 2 seconds for up to 30
     * seconds so the log clearly shows what the render thread is doing.
     */
    private static void startFreezeWatchdog(String portalTrace) {
        // Find the render thread. MC names it "Render thread"; NeoForge may call
        // it "Client thread" in some versions — check both.
        Thread renderThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            String n = t.getName();
            if (n.startsWith("Render thread") || n.equalsIgnoreCase("Client thread")) {
                renderThread = t;
                break;
            }
        }
        if (renderThread == null) {
            LOGGER.warn("[naturalis-portal] [{}] freeze-watchdog: render thread not found (available: {})",
                portalTrace,
                Thread.getAllStackTraces().keySet().stream().map(Thread::getName)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ", "));
            return;
        }
        final Thread rt = renderThread;
        // Also find the server thread
        Thread serverThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals("Server thread")) { serverThread = t; break; }
        }
        final Thread st = serverThread;
        LOGGER.info("[naturalis-portal] [{}] freeze-watchdog starting — watching render='{}' server='{}'",
            portalTrace, rt.getName(), st != null ? st.getName() : "NOT FOUND");
        Thread watchdog = new Thread(() -> {
            for (int tick = 1; tick <= 20; tick++) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                // Render thread
                {
                    Thread.State state = rt.getState();
                    StackTraceElement[] frames = rt.getStackTrace();
                    StringBuilder sb = new StringBuilder(512);
                    sb.append("[naturalis-portal] [").append(portalTrace)
                      .append("] freeze-watchdog t+").append(tick * 500).append("ms")
                      .append(" — RENDER '").append(rt.getName()).append("' state=").append(state);
                    for (StackTraceElement el : frames) sb.append("\n    at ").append(el);
                    LOGGER.warn(sb.toString());
                }
                // Server thread
                if (st != null) {
                    Thread.State state = st.getState();
                    StackTraceElement[] frames = st.getStackTrace();
                    StringBuilder sb = new StringBuilder(512);
                    sb.append("[naturalis-portal] [").append(portalTrace)
                      .append("] freeze-watchdog t+").append(tick * 500).append("ms")
                      .append(" — SERVER '").append(st.getName()).append("' state=").append(state);
                    for (StackTraceElement el : frames) sb.append("\n    at ").append(el);
                    LOGGER.warn(sb.toString());
                }
            }
            LOGGER.info("[naturalis-portal] [{}] freeze-watchdog done", portalTrace);
        }, "naturalis-freeze-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    /**
     * Adds {@code TicketType.FORCED} tickets for a radius-6 area around every
     * hut spawn centre.  Called once when the natural dimension level loads so
     * those chunks are never unloaded between player visits — eliminating the
     * "Loading terrain…" screen on repeat teleports.
     */
    public static void forceLoadSpawnChunks(ServerLevel level) {
        if (level == null) {
            LOGGER.error("[naturalis] forceLoadSpawnChunks called with null level! Natural dimension may not be registered.");
            return;
        }
        int radius = 6; // 13×13 = 169 chunks per hut, 12 huts ≈ 2000 chunks total
        int totalTickets = 0;
        LOGGER.info("[naturalis] forceLoadSpawnChunks START: level={} dim={} huts={}",
            level, level.dimension().location(), NATURAL_HUT_CENTERS.size());
        for (int i = 0; i < NATURAL_HUT_CENTERS.size(); i++) {
            BlockPos hut = NATURAL_HUT_CENTERS.get(i);
            ChunkPos cp = new ChunkPos(hut);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    level.setChunkForced(cp.x + dx, cp.z + dz, true);
                }
            }
            int squareSide = 2 * radius + 1;
            totalTickets += squareSide * squareSide;
            LOGGER.info("[naturalis] forceLoadSpawnChunks hut[{}]: center={} chunkPos=[{},{}] tickets+={}",
                i, hut, cp.x, cp.z, squareSide * squareSide);
        }
        LOGGER.info("[naturalis] forceLoadSpawnChunks DONE: radius={} huts={} totalTickets~={} (async — chunks will load in background)",
            radius, NATURAL_HUT_CENTERS.size(), totalTickets);
    }

    private static void debugPortal(String message, Object... args) {
        if (!PORTAL_DEBUG_LOGS) {
            return;
        }
        LOGGER.info("[naturalis-portal] " + message, args);
    }

    private static String shortPlayerId(ServerPlayer player) {
        String id = player.getUUID().toString();
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    public static void onOverworldChunkLoaded(ServerLevel level, LevelChunk chunk) {
        if (level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            // Block placement for the Natural dimension is handled in
            // IslandChunkGenerator.applyBiomeDecoration, which runs on world-gen worker
            // threads using WorldGenLevel (never blocks the server thread).
            return;
        }

        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (!NaturalisWorldgenFlags.overworldSwampInactivePortalsOnChunkLoad()) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        final long seedMix = level.getSeed() ^ (ChunkPos.asLong(chunkPos.x, chunkPos.z) * 132897987541L);
        // Swamp check must use this chunk's noise biome only — level.getBiome() can sync-load neighbors
        // and stall integrated-server spawn preparation (Forge 1.20.1).
        BlockPos chunkCenter = new BlockPos(chunkPos.getMiddleBlockX(), level.getSeaLevel(), chunkPos.getMiddleBlockZ());
        int qx = QuartPos.fromBlock(chunkCenter.getX());
        int qy = QuartPos.fromBlock(chunkCenter.getY());
        int qz = QuartPos.fromBlock(chunkCenter.getZ());
        if (!chunk.getNoiseBiome(qx, qy, qz).is(Biomes.SWAMP)) {
            return;
        }

        RandomSource random = RandomSource.create(seedMix);
        if (random.nextDouble() > 0.04D) {
            return;
        }

        final int cx = chunkPos.x;
        final int cz = chunkPos.z;
        final int worldX = chunkPos.getMiddleBlockX() + random.nextInt(7) - 3;
        final int worldZ = chunkPos.getMiddleBlockZ() + random.nextInt(7) - 3;
        final int localX = worldX - chunkPos.getMinBlockX();
        final int localZ = worldZ - chunkPos.getMinBlockZ();

        // Defer block writes only: avoids re-entrant chunk loads from setBlock during ChunkEvent.Load.
        server.execute(() -> {
            if (!level.hasChunk(cx, cz)) {
                return;
            }
            LevelChunk c = level.getChunk(cx, cz);
            int y = Math.max(-63, c.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ));
            placeInactivePortalFrame(level, new BlockPos(worldX, y, worldZ), Direction.WEST, Direction.EAST);
        });
    }

    public static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
                continue;
            }

            long gameTime = level.getGameTime();

            if (gameTime % 200L == 0L) {
                ensureEchoSovereignSpawn(level);
            }

            // Rain is disabled in the Natural Dimension.
            if (level.isRaining() && !level.isThundering()) {
                level.setWeatherParameters(6000, 0, false, false);
            }

            // Keep thunderstorms frequent and dry-like (no persistent rain phase).
            if (gameTime % 200L == 0L && !level.isThundering() && level.random.nextDouble() < 0.06D) {
                int thunderDuration = (30 + level.random.nextInt(120)) * 20;
                level.setWeatherParameters(0, thunderDuration, false, true);
            }
            if (level.isThundering() && gameTime % 40L == 0L) {
                level.setWeatherParameters(0, 600, false, true);
            }

            for (ServerPlayer player : level.players()) {
                if (gameTime % NATURAL_HUMANITY_DRAIN_TICKS == 0L && ResonanceManager.isResonanceEnabled(player)) {
                    ResonanceManager.addHumanity(player, -1);
                }

                if (level.isThundering()) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(CompatAccess.naturalisMobEffectHolder("storm_attunement"), 120, 0, true, false, true));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(CompatAccess.naturalisMobEffectHolder("morph_binding"), 120, 0, true, false, true));
                }
            }

            EchoSovereignRuntime.tick(level);
            NaturalIslandPassiveBoost.tick(level);
            NaturalDimensionEntityBudget.tick(level);

            // Undead mobs that normally burn in sunlight are fire-immune in the volcano biome.
            // Scan near players only — never the whole ±512 island AABB.
            if (gameTime % 10L == 0L) {
                for (ServerPlayer player : level.players()) {
                    level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(48.0D),
                        entity -> !(entity instanceof Player) && entity.isOnFire()
                    ).forEach(entity -> {
                        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                        if (id != null && VOLCANO_FIRE_IMMUNE_MOBS.contains(id.getPath())
                            && level.getBiome(entity.blockPosition()).is(NaturalDimensionKeys.VOLCANO)) {
                            entity.clearFire();
                        }
                    });
                }
            }
        }
    }

    private static void ensureEchoSovereignSpawn(ServerLevel level) {
        if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }

        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm == null) {
            return;
        }

        int ax = hm.getEchoArenaBlockX();
        int az = hm.getEchoArenaBlockZ();
        if (!hm.isInsideIsland(ax, az) || !NaturalDimensionKeys.NATURAL_ECHO.equals(hm.getBiomeKey(ax, az))) {
            return;
        }

        // Force-load arena so an existing boss (or stale track) can be resolved before summoning.
        level.getChunk(ax >> 4, az >> 4);

        if (!findEchoSovereigns(level).isEmpty()) {
            return;
        }

        // Arena loaded + empty: tracked UUIDs are stale (e.g. /kill while chunk was unloaded).
        if (EchoSovereignRuntime.hasTrackedSovereign()) {
            EchoSovereignRuntime.clearTrackedSovereigns();
        }

        BlockPos arena = new BlockPos(ax, hm.getSurfaceY(ax, az), az);
        if (!level.getBiome(arena).is(NaturalDimensionKeys.NATURAL_ECHO)) {
            return;
        }

        int y = hm.getSurfaceY(ax, az) + 1;

        EntityType<?> sovereignType = CompatAccess.getEntityType(ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "echo_sovereign"));
        if (sovereignType == null) {
            sovereignType = EntityType.EVOKER;
        }

        var created = CompatAccess.createEntity(sovereignType, level);
        if (!(created instanceof Mob boss)) {
            return;
        }

        boss.teleportTo(ax + 0.5D, y, az + 0.5D);
        EchoSovereignRuntime.initializeBoss(boss);
        level.addFreshEntity(boss);
    }

    /** Prefer known boss UUIDs / arena / player vicinity — never scan the whole ±2048 island. */
    private static java.util.List<Mob> findEchoSovereigns(ServerLevel level) {
        java.util.List<Mob> found = EchoSovereignRuntime.knownSovereigns(level);
        if (!found.isEmpty()) {
            return found;
        }
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm != null) {
            int ax = hm.getEchoArenaBlockX();
            int az = hm.getEchoArenaBlockZ();
            int y = hm.getSurfaceY(ax, az);
            AABB arena = new AABB(
                ax - SOVEREIGN_LOOKUP_RADIUS, y - 32, az - SOVEREIGN_LOOKUP_RADIUS,
                ax + SOVEREIGN_LOOKUP_RADIUS, y + 64, az + SOVEREIGN_LOOKUP_RADIUS
            );
            found = level.getEntitiesOfClass(Mob.class, arena, EchoSovereignRuntime::isEchoSovereign);
            if (!found.isEmpty()) {
                return found;
            }
        }
        for (ServerPlayer player : level.players()) {
            found = level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(SOVEREIGN_LOOKUP_RADIUS),
                EchoSovereignRuntime::isEchoSovereign
            );
            if (!found.isEmpty()) {
                return found;
            }
        }
        return java.util.List.of();
    }

    private static BlockPos selectNaturalHut(ServerPlayer player) {
        int index = Math.floorMod(player.getUUID().hashCode(), NATURAL_HUT_CENTERS.size());
        return NATURAL_HUT_CENTERS.get(index);
    }

    private static boolean isPortalFrameValid(Level level, BlockPos bottomCenter, Direction left, Direction right) {
        Block frame = CompatAccess.naturalisBlock("natural_portal_frame");
        BlockPos leftBase = bottomCenter.relative(left);
        BlockPos rightBase = bottomCenter.relative(right);

        BlockPos[] framePositions = new BlockPos[] {
            leftBase,
            bottomCenter,
            rightBase,
            leftBase.above(),
            rightBase.above(),
            leftBase.above(2),
            rightBase.above(2),
            leftBase.above(3),
            bottomCenter.above(3),
            rightBase.above(3)
        };

        for (BlockPos pos : framePositions) {
            if (!level.getBlockState(pos).is(frame)) {
                return false;
            }
        }

        BlockPos[] interior = new BlockPos[] {
            bottomCenter.above(),
            bottomCenter.above(2)
        };

        for (BlockPos pos : interior) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(CompatAccess.naturalisBlock("natural_portal"))) {
                return false;
            }
        }

        return true;
    }

    private static void placePortalInterior(Level level, BlockPos bottomCenter) {
        var portalState = CompatAccess.naturalisBlock("natural_portal").defaultBlockState();
        level.setBlock(bottomCenter.above(), portalState, Block.UPDATE_ALL);
        level.setBlock(bottomCenter.above(2), portalState, Block.UPDATE_ALL);
    }

    static void placeInactivePortalFrame(net.minecraft.world.level.LevelAccessor level, BlockPos base, Direction left, Direction right) {
        // GEN_FLAGS: no neighbor shape propagation — safe during chunk generation.
        Block frame = CompatAccess.naturalisBlock("natural_portal_frame");
        BlockPos leftBase = base.relative(left);
        BlockPos rightBase = base.relative(right);

        for (int y = 0; y <= 3; y++) {
            level.setBlock(leftBase.above(y), frame.defaultBlockState(), GEN_FLAGS);
            level.setBlock(rightBase.above(y), frame.defaultBlockState(), GEN_FLAGS);
        }
        level.setBlock(base, frame.defaultBlockState(), GEN_FLAGS);
        level.setBlock(base.above(3), frame.defaultBlockState(), GEN_FLAGS);

        level.setBlock(base.above(), Blocks.AIR.defaultBlockState(), GEN_FLAGS);
        level.setBlock(base.above(2), Blocks.AIR.defaultBlockState(), GEN_FLAGS);
    }
}
