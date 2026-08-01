package dev.naturalis.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Deterministic chunk generator for the Natural dimension floating island.
 * <p>
 * Terrain is read pixel-by-pixel from two PNG resources:
 * <ul>
 *   <li>{@code assets/naturalis/island/heightmap.png}  – grayscale, 1 px = 1 block XZ</li>
 *   <li>{@code assets/naturalis/island/biome_map.png}  – colour-coded biome assignment</li>
 * </ul>
 * All coordinates outside the island remain void (air).
 */
public final class IslandChunkGenerator extends ChunkGenerator {

    // ── Codec ────────────────────────────────────────────────────────────────
    public static final MapCodec<IslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(inst ->
        inst.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
        ).apply(inst, IslandChunkGenerator::new)
    );

    // ── Surface block selection ───────────────────────────────────────────────
    // Returns the top-most block for a given biome
    private static BlockState surfaceBlock(ResourceKey<?> biome) {
        String path = biome.location().getPath();
        return switch (path) {
            case "volcano"        -> Blocks.BASALT.defaultBlockState();
            case "snowy_mountain" -> Blocks.SNOW_BLOCK.defaultBlockState();
            case "high_peak"      -> Blocks.STONE.defaultBlockState();
            case "dark_caves"     -> Blocks.GRAVEL.defaultBlockState();
            case "coral_water"  -> Blocks.SAND.defaultBlockState();   // reef floor
            case "deep_water"    -> Blocks.GRAVEL.defaultBlockState(); // deep sea bed
            case "natural_beach"  -> Blocks.SAND.defaultBlockState();
            case "arid_savanna"   -> Blocks.COARSE_DIRT.defaultBlockState();
            case "ender_forest"   -> Blocks.END_STONE.defaultBlockState();
            case "natural_echo"   -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            default               -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    // Returns the 1–3 sub-surface fill block for a biome
    private static BlockState subSurfaceBlock(ResourceKey<?> biome) {
        String path = biome.location().getPath();
        return switch (path) {
            case "volcano"        -> Blocks.BLACKSTONE.defaultBlockState();
            case "snowy_mountain",
                 "high_peak"      -> Blocks.STONE.defaultBlockState();
            case "natural_beach"  -> Blocks.SAND.defaultBlockState();
            case "arid_savanna"   -> Blocks.COARSE_DIRT.defaultBlockState();
            case "ender_forest"   -> Blocks.END_STONE.defaultBlockState();
            case "natural_echo"   -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case "coral_water"    -> Blocks.SAND.defaultBlockState();
            case "deep_water"     -> Blocks.GRAVEL.defaultBlockState();
            default               -> Blocks.DIRT.defaultBlockState();
        };
    }

    // ── Construction ─────────────────────────────────────────────────────────
    public IslandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    public int getMinY() {
        return IslandHeightmap.Y_MIN;
    }

    @Override
    public int getSeaLevel() {
        return IslandHeightmap.WATER_LEVEL;
    }

    @Override
    public int getGenDepth() {
        // Total height of the world: MAX_Y - MIN_Y
        return IslandHeightmap.Y_MAX - IslandHeightmap.Y_MIN;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ── Core generation ───────────────────────────────────────────────────────

    /**
     * Phase 1 – fills every column with stone from island bottom to raw
     * surface Y.  Lake columns get water up to WATER_LEVEL.
     */
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {

        IslandHeightmap hm  = IslandHeightmap.getOrLoad();
        ChunkPos        cp  = chunk.getPos();
        int minBuild        = chunk.getMinBuildHeight();
        int maxBuild        = chunk.getMaxBuildHeight();

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air   = Blocks.AIR.defaultBlockState();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int bx = cp.getBlockX(lx);
                int bz = cp.getBlockZ(lz);

                if (!hm.isInsideIsland(bx, bz)) continue;

                int surfY  = hm.getSurfaceY(bx, bz);
                int botY   = hm.getBottomY(bx, bz);
                int waterY = IslandHeightmap.WATER_LEVEL;

                // Clamp to build limits
                int top = Math.min(surfY, maxBuild - 1);
                int bot = Math.max(botY, minBuild);

                // Stone column
                for (int y = bot; y <= top; y++) {
                    chunk.setBlockState(new BlockPos(bx, y, bz), stone, false);
                }

                // Water fill for lake depressions
                if (surfY < waterY) {
                    int waterTop = Math.min(waterY, maxBuild - 1);
                    for (int y = surfY + 1; y <= waterTop; y++) {
                        chunk.setBlockState(new BlockPos(bx, y, bz), water, false);
                    }
                }

                // ── Hanging stalactites below island underside ────────────────
                // Two-octave smooth noise drives length: ~50% of columns get a
                // spike, up to 28 blocks long, creating a classic floating-island
                // look with irregular downward teeth.
                if (botY > minBuild) {
                    double sn1 = stalNoise(bx / 8.0 + 200.0, bz / 8.0 + 311.0);
                    double sn2 = stalNoise(bx / 3.5 + 422.0, bz / 3.5 + 178.0);
                    double sn = sn1 * 0.65 + sn2 * 0.35; // [-1, 1]
                    if (sn > 0.1) {
                        int spikeLen = 1 + (int) ((sn - 0.1) / 0.9 * 28);
                        int stalBot  = Math.max(minBuild, botY - spikeLen);
                        for (int y = stalBot; y < bot; y++) {
                            chunk.setBlockState(new BlockPos(bx, y, bz), stone, false);
                        }
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Phase 2 – replaces the top 4 blocks of each column with biome-specific
     * surface material (grass/sand/basalt/etc.).
     */
    @Override
    public void buildSurface(
            WorldGenRegion region,
            StructureManager structureManager,
            RandomState random,
            ChunkAccess chunk) {

        IslandHeightmap hm = IslandHeightmap.getOrLoad();
        ChunkPos        cp = chunk.getPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int bx = cp.getBlockX(lx);
                int bz = cp.getBlockZ(lz);

                if (!hm.isInsideIsland(bx, bz)) continue;

                int surfY = hm.getSurfaceY(bx, bz);
                ResourceKey<?> biome = hm.getBiomeKey(bx, bz);
                String biomePath = biome.location().getPath();
                boolean isOceanFloor = biomePath.equals("coral_water") || biomePath.equals("deep_water");

                // Dress land above water, AND ocean-floor biomes (even if submerged).
                // Other land biomes below the water line stay as bare stone.
                if (surfY < IslandHeightmap.WATER_LEVEL && !isOceanFloor) continue;

                // Surface layer
                BlockState top = surfaceBlock(biome);
                // Snowy mountain: substitute ice/packed_ice at high altitudes so
                // ice concentrates on the peaks rather than the lower slopes.
                if (biomePath.equals("snowy_mountain")) {
                    if (surfY > 155) top = Blocks.PACKED_ICE.defaultBlockState();
                    else if (surfY > 135) top = Blocks.ICE.defaultBlockState();
                    // else keep SNOW_BLOCK (default)
                }
                // High peak: progressive ice and powder snow from lower to summit.
                if (biomePath.equals("high_peak")) {
                    if (surfY > 168) {
                        top = Blocks.BLUE_ICE.defaultBlockState();
                    } else if (surfY > 153) {
                        top = Blocks.PACKED_ICE.defaultBlockState();
                    } else if (surfY > 135) {
                        // Noise-driven mix of ice and powder snow at mid altitudes
                        double iceN = stalNoise(bx / 9.0 + 500.0, bz / 9.0 + 700.0);
                        if (iceN > 0.55) top = Blocks.POWDER_SNOW.defaultBlockState();
                        else if (iceN > 0.05) top = Blocks.ICE.defaultBlockState();
                        // else keep STONE
                    } else if (surfY > 115) {
                        // Sparse ice patches at lower elevations
                        double iceN2 = stalNoise(bx / 12.0 + 111.0, bz / 12.0 + 333.0);
                        if (iceN2 > 0.6) top = Blocks.ICE.defaultBlockState();
                    }
                }
                if (biomePath.equals("natural_echo")) {
                    top = echoSurfaceTop(bx, bz);
                }
                chunk.setBlockState(new BlockPos(bx, surfY, bz), top, false);

                // 3 sub-surface layers
                if (biomePath.equals("natural_echo")) {
                    for (int d = 1; d <= 3; d++) {
                        int y = surfY - d;
                        if (y >= chunk.getMinBuildHeight()) {
                            chunk.setBlockState(new BlockPos(bx, y, bz), echoSubSurface(bx, bz, d), false);
                        }
                    }
                } else {
                    BlockState sub = subSurfaceBlock(biome);
                    for (int d = 1; d <= 3; d++) {
                        int y = surfY - d;
                        if (y >= chunk.getMinBuildHeight()) {
                            chunk.setBlockState(new BlockPos(bx, y, bz), sub, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * No vanilla carvers – the island has hand-crafted cave structures.
     */
    @Override
    public void applyCarvers(
            WorldGenRegion region, long seed, RandomState random,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk, GenerationStep.Carving step) {
        // Intentionally empty
    }

    // ── Height queries (used by structures, pathfinding, etc.) ───────────────

    @Override
    public int getBaseHeight(
            int x, int z, Heightmap.Types type,
            LevelHeightAccessor level, RandomState random) {
        // Use tryGet() — safe for render thread; returns null if not yet loaded.
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm == null || !hm.isInsideIsland(x, z)) return level.getMinBuildHeight();
        int surfY = hm.getSurfaceY(x, z);
        // For OCEAN_FLOOR types, return the raw surface even if submerged
        return surfY + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        // Use tryGet() — safe for render thread; returns null if not yet loaded.
        IslandHeightmap hm = IslandHeightmap.tryGet();
        int minY   = level.getMinBuildHeight();
        int height = level.getHeight();
        BlockState[] states = new BlockState[height];

        if (hm != null && hm.isInsideIsland(x, z)) {
            int surfY = hm.getSurfaceY(x, z);
            int botY  = hm.getBottomY(x, z);
            for (int i = 0; i < height; i++) {
                int y = minY + i;
                if (y >= botY && y <= surfY) {
                    states[i] = Blocks.STONE.defaultBlockState();
                } else if (y > surfY && y <= IslandHeightmap.WATER_LEVEL && surfY < IslandHeightmap.WATER_LEVEL) {
                    states[i] = Blocks.WATER.defaultBlockState();
                } else {
                    states[i] = Blocks.AIR.defaultBlockState();
                }
            }
        } else {
            java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState random, BlockPos pos) {
        // tryGet() to avoid blocking the render thread (F3 screen runs on render thread).
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm != null && hm.isInsideIsland(pos.getX(), pos.getZ())) {
            list.add("Island surfY=" + hm.getSurfaceY(pos.getX(), pos.getZ())
                + " botY=" + hm.getBottomY(pos.getX(), pos.getZ())
                + " biome=" + hm.getBiomeKey(pos.getX(), pos.getZ()).location());
        } else {
            list.add("Island: outside");
        }
    }

    /** Vanilla mob spawning is disabled in this dimension. */
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Intentionally empty – mobs are spawned via biome spawn lists only
    }

    // ── Feature generation (huts, echo spikes, ores) ─────────────────────────
    //
    // applyBiomeDecoration runs on WORLD-GEN WORKER THREADS at the FEATURES chunk
    // status.  The level parameter is a WorldGenRegion — an in-memory buffer of
    // already-generated neighbour chunks.  Reading/writing block states here is
    // completely safe and NEVER blocks the server thread.

    /**
     * Flags for bulk block placement during worldgen.
     * UPDATE_CLIENTS (2) + UPDATE_KNOWN_SHAPE (16): send change to client, suppress
     * neighbour-shape propagation.
     */
    private static final int GEN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Place ores on top of spikes (disabled by default – heavy operation). */
    private static final boolean ENABLE_NATURAL_ORE_DECORATION =
        Boolean.parseBoolean(System.getProperty("naturalis.enableNaturalOreDecoration", "false"));

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        ChunkPos cp = chunk.getPos();
        maybeGenerateNaturalHuts(level, cp);
        EchoArenaGenerator.tryGenerate(level, cp, IslandHeightmap.tryGet());
        generateEchoSpikes(level, cp);
        if (ENABLE_NATURAL_ORE_DECORATION) {
            generateNaturalOres(level, cp);
        }
    }

    private static void maybeGenerateNaturalHuts(WorldGenLevel level, ChunkPos chunkPos) {
        for (BlockPos hutCenter : NaturalDimensionRuntime.NATURAL_HUT_CENTERS) {
            if ((hutCenter.getX() >> 4) != chunkPos.x || (hutCenter.getZ() >> 4) != chunkPos.z) {
                continue;
            }
            ensureNaturalHut(level, hutCenter);
        }
    }

    private static void ensureNaturalHut(WorldGenLevel level, BlockPos center) {
        // Use the actual terrain surface Y so the hut is never underground.
        IslandHeightmap hm = IslandHeightmap.tryGet();
        int actualSurfY = (hm != null) ? hm.getSurfaceY(center.getX(), center.getZ()) : center.getY();
        BlockPos base = new BlockPos(center.getX(), actualSurfY, center.getZ());

        // ── Materials (vanilla witch-hut palette) ──────────────────────────
        BlockState sprucePlank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState oakLog      = Blocks.OAK_LOG.defaultBlockState();
        BlockState oakFence    = Blocks.OAK_FENCE.defaultBlockState();
        BlockState spruceSlab  = Blocks.SPRUCE_SLAB.defaultBlockState();
        BlockState air         = Blocks.AIR.defaultBlockState();

        // ── Fence stilts at the four corners (down 3, flush at floor) ──────
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            for (int dy = -3; dy <= 0; dy++) {
                BlockPos sp = base.offset(c[0], dy, c[1]);
                if (level.getBlockState(sp).canBeReplaced() || level.getBlockState(sp).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(sp, oakFence, GEN_FLAGS);
                }
            }
        }

        // ── Floor (5×5 spruce planks at y+1) ───────────────────────────────
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(base.offset(x, 1, z), sprucePlank, GEN_FLAGS);
            }
        }

        // ── Clear interior (4 air layers above floor) ──────────────────────
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int dy = 2; dy <= 5; dy++) {
                    level.setBlock(base.offset(x, dy, z), air, GEN_FLAGS);
                }
            }
        }

        // ── Walls (hollow 5×5, y+2 and y+3) ───────────────────────────────
        // Corners = oak log, edges = spruce planks; front-centre open for door.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean isEdge = (Math.abs(x) == 2 || Math.abs(z) == 2);
                if (!isEdge) continue;
                boolean isCorner = (Math.abs(x) == 2 && Math.abs(z) == 2);
                BlockState wall = isCorner ? oakLog : sprucePlank;
                level.setBlock(base.offset(x, 2, z), wall, GEN_FLAGS);
                level.setBlock(base.offset(x, 3, z), wall, GEN_FLAGS);
            }
        }
        // Door opening on south face (z=+2): clear centre two blocks
        level.setBlock(base.offset(0, 2, 2), air, GEN_FLAGS);
        level.setBlock(base.offset(0, 3, 2), air, GEN_FLAGS);

        // ── Tiered spruce-slab roof ─────────────────────────────────────────
        // Layer 1: full 5×5 at y+4
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(base.offset(x, 4, z), spruceSlab, GEN_FLAGS);
            }
        }
        // Layer 2: 3×3 at y+5
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(base.offset(x, 5, z), spruceSlab, GEN_FLAGS);
            }
        }
        // Peak: single block at y+6
        level.setBlock(base.offset(0, 6, 0), spruceSlab, GEN_FLAGS);

        // ── Interior furnishings ────────────────────────────────────────────
        level.setBlock(base.offset(1, 2, -1), Blocks.CRAFTING_TABLE.defaultBlockState(), GEN_FLAGS);
        level.setBlock(base.offset(-1, 2, -1), Blocks.CAULDRON.defaultBlockState(), GEN_FLAGS);

        // ── Portal frame ────────────────────────────────────────────────────
        BlockPos portalBase = base.offset(0, 1, 5);
        NaturalDimensionRuntime.placeInactivePortalFrame(level, portalBase, Direction.WEST, Direction.EAST);
    }

    private static void generateEchoSpikes(WorldGenLevel level, ChunkPos chunkPos) {
        IslandHeightmap hm = IslandHeightmap.tryGet();

        long salt = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        RandomSource random = RandomSource.create(level.getSeed() ^ (salt * 341873128712L));
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        // Determine echo-biome proximity from the biome map.
        // - IN the echo biome       : 15% chance (biome features add 1-3 spikes, this is extra)
        // - Adjacent to echo biome  : 25% chance (1 spike per ~4 chunks)
        // - Anywhere else on island : 10% chance (approx 1 spike per 50-block radius)
        // This spreads echo spikes across the island with a gradient toward the echo.
        double spawnChance;
        if (hm != null) {
            ResourceKey<?> center  = hm.getBiomeKey(centerX, centerZ);
            boolean inEcho = NaturalDimensionKeys.NATURAL_ECHO.equals(center);

            // "near echo" = at least one corner of this chunk is echo
            boolean nearEcho = false;
            if (!inEcho) {
                for (int cx : new int[]{chunkPos.getMinBlockX(), chunkPos.getMaxBlockX()}) {
                    for (int cz : new int[]{chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ()}) {
                        if (NaturalDimensionKeys.NATURAL_ECHO.equals(hm.getBiomeKey(cx, cz))) {
                            nearEcho = true;
                        }
                    }
                }
            }

            spawnChance = inEcho ? 0.15D : nearEcho ? 0.25D : 0.10D;
        } else {
            // Heightmap not loaded yet — skip (will regenerate on next load attempt)
            return;
        }

        if (random.nextDouble() >= spawnChance) {
            return;
        }

        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);

        // Use OCEAN_FLOOR so spikes in water areas start from the sea bed, not
        // the water surface — they grow up through the water and stay submerged.
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (y < -62) {
            return;
        }

        placeLeanedSpike(level, random, new BlockPos(x, y, z));
    }

    private static BlockState spikeMaterial(RandomSource random, ResourceKey<?> biomeKey) {
        if (biomeKey != null && NaturalDimensionKeys.NATURAL_ECHO.equals(biomeKey)) {
            return random.nextFloat() < 0.67F
                ? CompatAccess.naturalisBlock("echo_block").defaultBlockState()
                : Blocks.SCULK.defaultBlockState();
        }
        return CompatAccess.naturalisBlock("echo_block").defaultBlockState();
    }

    private static void placeLeanedSpike(WorldGenLevel level, RandomSource random, BlockPos base) {
        // Height: 6–13 blocks (shorter than before).
        int height = 6 + random.nextInt(8);
        float angleDegrees = 20.0F + random.nextFloat() * 15.0F;
        double horizontalStep = Math.tan(Math.toRadians(angleDegrees));
        double azimuth = random.nextDouble() * (Math.PI * 2.0D);
        double stepX = Math.cos(azimuth) * horizontalStep;
        double stepZ = Math.sin(azimuth) * horizontalStep;

        IslandHeightmap hmSpike = IslandHeightmap.tryGet();
        ResourceKey<?> biomeKey = hmSpike != null ? hmSpike.getBiomeKey(base.getX(), base.getZ()) : null;

        for (int i = 0; i < height; i++) {
            int dx = (int) Math.round(stepX * i);
            int dz = (int) Math.round(stepZ * i);
            BlockPos center = base.offset(dx, i, dz);

            // Base is wide (radius 2 = 9-block cross), middle narrow (radius 1),
            // tip is a single block.  Produces a chunky obelisk feel.
            int radius = i < 2 ? 2 : (i < 5 ? 1 : 0);

            for (int rx = -radius; rx <= radius; rx++) {
                for (int rz = -radius; rz <= radius; rz++) {
                    if (radius > 0 && Math.abs(rx) + Math.abs(rz) > radius) {
                        continue; // diamond cross-section
                    }
                    BlockPos placeAt = center.offset(rx, 0, rz);

                    // Stay inside WorldGenRegion boundaries.
                    if (!level.hasChunk(placeAt.getX() >> 4, placeAt.getZ() >> 4)) {
                        continue;
                    }

                    // Cap underwater spikes so they never break the water surface.
                    if (placeAt.getY() >= IslandHeightmap.WATER_LEVEL) {
                        continue;
                    }

                    BlockState state = level.getBlockState(placeAt);
                    if (state.canBeReplaced() || state.isAir()
                            || state.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        level.setBlock(placeAt, spikeMaterial(random, biomeKey), GEN_FLAGS);
                    }
                }
            }
        }
    }

    private static void generateNaturalOres(WorldGenLevel level, ChunkPos chunkPos) {
        RandomSource random = RandomSource.create(level.getSeed() ^ (ChunkPos.asLong(chunkPos.x, chunkPos.z) * 117281231L));

        int goldVeins = 1 + random.nextInt(2);
        for (int i = 0; i < goldVeins; i++) {
            placeOreBlob(level, chunkPos, random,
                random.nextBoolean() ? Blocks.GOLD_ORE.defaultBlockState() : Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
                5 + random.nextInt(5), -40, 90);
        }

        if (random.nextDouble() < 0.15D) {
            placeOreBlob(level, chunkPos, random,
                random.nextBoolean() ? Blocks.IRON_ORE.defaultBlockState() : Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
                4 + random.nextInt(4), -48, 72);
        }
    }

    private static void placeOreBlob(WorldGenLevel level, ChunkPos chunkPos, RandomSource random,
                                     BlockState oreState, int size, int minY, int maxY) {
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        int y = minY + random.nextInt(Math.max(1, maxY - minY + 1));

        for (int i = 0; i < size; i++) {
            int ox = x + random.nextInt(4) - random.nextInt(4);
            int oy = y + random.nextInt(3) - random.nextInt(3);
            int oz = z + random.nextInt(4) - random.nextInt(4);
            BlockPos pos = new BlockPos(ox, oy, oz);
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (current.is(Blocks.STONE) || current.is(Blocks.DEEPSLATE)
                    || current.is(Blocks.END_STONE) || current.canBeReplaced()) {
                level.setBlock(pos, oreState, GEN_FLAGS);
            }
        }
    }

    // ── Noise helpers (for stalactites and high-peak ice) ────────────────────

    /** Natural Echo surface: mostly deepslate variants with echo block / sculk accents. */
    private static BlockState echoSurfaceTop(int bx, int bz) {
        double en = stalNoise(bx / 4.2 + 901.0, bz / 4.2 + 207.0);
        if (en < 0.4) {
            double d = stalNoise(bx / 2.1 + 55.0, bz / 2.1 + 77.0);
            if (d < -0.33) {
                return Blocks.DEEPSLATE_TILES.defaultBlockState();
            }
            if (d < 0.33) {
                return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            }
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (en < 0.8) {
            return CompatAccess.naturalisBlock("echo_block").defaultBlockState();
        }
        return Blocks.SCULK.defaultBlockState();
    }

    private static BlockState echoSubSurface(int bx, int bz, int depth) {
        double sn = stalNoise(bx / 3.3 + depth * 41.0 + 12.0, bz / 3.3 + depth * 17.0 + 88.0);
        if (sn < 0.65) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (sn < 0.88) {
            return CompatAccess.naturalisBlock("echo_block").defaultBlockState();
        }
        return Blocks.SCULK.defaultBlockState();
    }

    /** Bilinearly-interpolated value noise in [-1, 1]. */
    private static double stalNoise(double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = x - ix, fz = z - iz;
        double ux = fx * fx * (3 - 2 * fx), uz = fz * fz * (3 - 2 * fz);
        return stalHash(ix,   iz  ) * (1-ux) * (1-uz)
             + stalHash(ix+1, iz  ) * ux     * (1-uz)
             + stalHash(ix,   iz+1) * (1-ux) * uz
             + stalHash(ix+1, iz+1) * ux     * uz;
    }

    /** Pseudo-random double in [-1, 1] for integer grid point. */
    private static double stalHash(int ix, int iz) {
        long h = (long) ix * 2654435761L ^ (long) iz * 1664525L;
        h ^= (h >>> 30); h *= 0xbf58476d1ce4e5b9L;
        h ^= (h >>> 27); h *= 0x94d049bb133111ebL;
        h ^= (h >>> 31);
        return (h & 0xFFFFFFL) / (double) 0x800000L - 1.0;
    }
}
