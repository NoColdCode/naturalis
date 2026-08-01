package dev.naturalis.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.naturalis.content.NaturalisBlocks;
import net.minecraft.core.BlockPos;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 1.21.8-specific variant of the island chunk generator.
 * API differences vs 1.21.1:
 *  - {@code applyCarvers} no longer has a {@code GenerationStep.Carving} parameter
 *  - {@code getMinBuildHeight()} / {@code getMaxBuildHeight()} renamed to {@code getMinY()} / {@code getMaxY()}
 *  - {@code ChunkAccess.setBlockState(pos, state, boolean)} removed; use {@code setBlockState(pos, state)} (no-flags overload)
 */
public final class IslandChunkGenerator extends ChunkGenerator {

    // ── Codec ────────────────────────────────────────────────────────────────
    public static final MapCodec<IslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(inst ->
        inst.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
        ).apply(inst, IslandChunkGenerator::new)
    );

    // ── Feature generation flags ──────────────────────────────────────────────
    private static final int GEN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    // ── Surface block selection ───────────────────────────────────────────────
    private static BlockState surfaceBlock(ResourceKey<?> biome) {
        String path = biome.location().getPath();
        return switch (path) {
            case "volcano"        -> Blocks.BASALT.defaultBlockState();
            case "snowy_mountain" -> Blocks.SNOW_BLOCK.defaultBlockState();
            case "high_peak"      -> Blocks.STONE.defaultBlockState();
            case "dark_caves"     -> Blocks.GRAVEL.defaultBlockState();
            case "coral_water"    -> Blocks.SAND.defaultBlockState();   // reef floor
            case "deep_water"     -> Blocks.GRAVEL.defaultBlockState(); // deep sea bed
            case "natural_beach"  -> Blocks.SAND.defaultBlockState();
            case "arid_savanna"   -> Blocks.COARSE_DIRT.defaultBlockState();
            case "ender_forest"   -> Blocks.END_STONE.defaultBlockState();
            case "natural_echo"   -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            default               -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

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
        return IslandHeightmap.Y_MAX - IslandHeightmap.Y_MIN;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ── Core generation ───────────────────────────────────────────────────────

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {

        IslandHeightmap hm = IslandHeightmap.getOrLoad();
        ChunkPos        cp = chunk.getPos();
        // 1.21.8: getMinY() / getMaxY() on LevelHeightAccessor
        int minBuild = chunk.getMinY();
        int maxBuild = chunk.getMaxY() + 1;

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int bx = cp.getBlockX(lx);
                int bz = cp.getBlockZ(lz);

                if (!hm.isInsideIsland(bx, bz)) continue;

                int surfY  = hm.getSurfaceY(bx, bz);
                int botY   = hm.getBottomY(bx, bz);
                int waterY = IslandHeightmap.WATER_LEVEL;

                int top = Math.min(surfY, maxBuild - 1);
                int bot = Math.max(botY, minBuild);

                // 1.21.8: setBlockState(pos, state) — no boolean/flags parameter
                for (int y = bot; y <= top; y++) {
                    chunk.setBlockState(new BlockPos(bx, y, bz), stone);
                }

                if (surfY < waterY) {
                    int waterTop = Math.min(waterY, maxBuild - 1);
                    for (int y = surfY + 1; y <= waterTop; y++) {
                        chunk.setBlockState(new BlockPos(bx, y, bz), water);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

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

                // Dress land above water AND submerged ocean-floor biomes.
                if (surfY < IslandHeightmap.WATER_LEVEL && !isOceanFloor) continue;

                BlockState top = surfaceBlock(biome);
                // Snowy mountain: ice concentrates at high altitude, not at the base
                if (biomePath.equals("snowy_mountain")) {
                    if (surfY > 155) top = Blocks.PACKED_ICE.defaultBlockState();
                    else if (surfY > 135) top = Blocks.ICE.defaultBlockState();
                }
                if (biomePath.equals("natural_echo")) {
                    top = echoSurfaceTop(bx, bz);
                }
                chunk.setBlockState(new BlockPos(bx, surfY, bz), top);

                if (biomePath.equals("natural_echo")) {
                    for (int d = 1; d <= 3; d++) {
                        int y = surfY - d;
                        if (y >= chunk.getMinY()) {
                            chunk.setBlockState(new BlockPos(bx, y, bz), echoSubSurface(bx, bz, d));
                        }
                    }
                } else {
                    BlockState sub = subSurfaceBlock(biome);
                    for (int d = 1; d <= 3; d++) {
                        int y = surfY - d;
                        if (y >= chunk.getMinY()) {
                            chunk.setBlockState(new BlockPos(bx, y, bz), sub);
                        }
                    }
                }
            }
        }
    }

    /**
     * No vanilla carvers – island has hand-crafted cave structures.
     * 1.21.8: signature has no GenerationStep.Carving parameter.
     */
    @Override
    public void applyCarvers(
            WorldGenRegion region, long seed, RandomState random,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk) {
        // Intentionally empty
    }

    // ── Height queries ───────────────────────────────────────────────────────

    @Override
    public int getBaseHeight(
            int x, int z, Heightmap.Types type,
            LevelHeightAccessor level, RandomState random) {
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm == null || !hm.isInsideIsland(x, z)) return level.getMinY();
        return hm.getSurfaceY(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        IslandHeightmap hm = IslandHeightmap.tryGet();
        int minY   = level.getMinY();
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
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm != null && hm.isInsideIsland(pos.getX(), pos.getZ())) {
            list.add("Island surfY=" + hm.getSurfaceY(pos.getX(), pos.getZ())
                + " botY=" + hm.getBottomY(pos.getX(), pos.getZ())
                + " biome=" + hm.getBiomeKey(pos.getX(), pos.getZ()).location());
        } else {
            list.add("Island: outside");
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Intentionally empty
    }

    // ── Feature generation (huts, echo spikes) ────────────────────────────────

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        ChunkPos cp = chunk.getPos();
        maybeGenerateNaturalHuts(level, cp);
        EchoArenaGenerator.tryGenerate(level, cp, IslandHeightmap.tryGet());
        generateEchoSpikes(level, cp);
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

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos floor = base.offset(x, 0, z);
                if (level.getBlockState(floor).canBeReplaced()) {
                    level.setBlock(floor, Blocks.SPRUCE_PLANKS.defaultBlockState(), GEN_FLAGS);
                }
                for (int y = 1; y <= 4; y++) {
                    BlockPos air = floor.above(y);
                    if (!level.getBlockState(air).is(Blocks.AIR)) {
                        level.setBlock(air, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
                    }
                }
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    BlockPos wall = base.offset(x, 1, z);
                    level.setBlock(wall, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), GEN_FLAGS);
                    level.setBlock(wall.above(), Blocks.SPRUCE_LOG.defaultBlockState(), GEN_FLAGS);
                }
            }
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) > 6) continue;
                level.setBlock(base.offset(x, 4, z), Blocks.SPRUCE_SLAB.defaultBlockState(), GEN_FLAGS);
            }
        }

        BlockPos door = base.offset(0, 1, -3);
        level.setBlock(door, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
        level.setBlock(door.above(), Blocks.AIR.defaultBlockState(), GEN_FLAGS);

        BlockPos portalBase = base.offset(0, 0, 5);
        NaturalDimensionRuntime.placeInactivePortalFrame(level, portalBase, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST);
    }

    private static BlockState spikeMaterial(RandomSource random, ResourceKey<?> biomeKey) {
        if (biomeKey != null && NaturalDimensionKeys.NATURAL_ECHO.equals(biomeKey)) {
            return random.nextFloat() < 0.67F
                ? NaturalisBlocks.ECHO_BLOCK.get().defaultBlockState()
                : Blocks.SCULK.defaultBlockState();
        }
        return NaturalisBlocks.ECHO_BLOCK.get().defaultBlockState();
    }

    private static void generateEchoSpikes(WorldGenLevel level, ChunkPos chunkPos) {
        IslandHeightmap hm = IslandHeightmap.tryGet();

        long salt = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        RandomSource random = RandomSource.create(level.getSeed() ^ (salt * 341873128712L));
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        double spawnChance;
        if (hm != null) {
            ResourceKey<?> center  = hm.getBiomeKey(centerX, centerZ);
            boolean inEcho = NaturalDimensionKeys.NATURAL_ECHO.equals(center);

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
            return;
        }

        if (random.nextDouble() >= spawnChance) return;

        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (y < -62) return;

        placeLeanedSpike(level, random, new BlockPos(x, y, z));
    }

    private static void placeLeanedSpike(WorldGenLevel level, RandomSource random, BlockPos base) {
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
            int radius = i < 2 ? 2 : (i < 5 ? 1 : 0);

            for (int rx = -radius; rx <= radius; rx++) {
                for (int rz = -radius; rz <= radius; rz++) {
                    if (radius > 0 && Math.abs(rx) + Math.abs(rz) > radius) continue;
                    BlockPos placeAt = center.offset(rx, 0, rz);
                    if (!level.hasChunk(placeAt.getX() >> 4, placeAt.getZ() >> 4)) continue;
                    if (placeAt.getY() >= IslandHeightmap.WATER_LEVEL) continue;
                    BlockState state = level.getBlockState(placeAt);
                    if (state.canBeReplaced() || state.isAir()
                            || state.is(Blocks.WATER)) {
                        level.setBlock(placeAt, spikeMaterial(random, biomeKey), GEN_FLAGS);
                    }
                }
            }
        }
    }

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
            return NaturalisBlocks.ECHO_BLOCK.get().defaultBlockState();
        }
        return Blocks.SCULK.defaultBlockState();
    }

    private static BlockState echoSubSurface(int bx, int bz, int depth) {
        double sn = stalNoise(bx / 3.3 + depth * 41.0 + 12.0, bz / 3.3 + depth * 17.0 + 88.0);
        if (sn < 0.65) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (sn < 0.88) {
            return NaturalisBlocks.ECHO_BLOCK.get().defaultBlockState();
        }
        return Blocks.SCULK.defaultBlockState();
    }

    private static double stalNoise(double x, double z) {
        int ix = Mth.floor(x), iz = Mth.floor(z);
        double fx = x - ix, fz = z - iz;
        double ux = fx * fx * (3 - 2 * fx), uz = fz * fz * (3 - 2 * fz);
        return stalHash(ix, iz) * (1 - ux) * (1 - uz)
            + stalHash(ix + 1, iz) * ux * (1 - uz)
            + stalHash(ix, iz + 1) * (1 - ux) * uz
            + stalHash(ix + 1, iz + 1) * ux * uz;
    }

    private static double stalHash(int ix, int iz) {
        long h = (long) ix * 2654435761L ^ (long) iz * 1664525L;
        h ^= (h >>> 30);
        h *= 0xbf58476d1ce4e5b9L;
        h ^= (h >>> 27);
        h *= 0x94d049bb133111ebL;
        h ^= (h >>> 31);
        return (h & 0xFFFFFFL) / (double) 0x800000L - 1.0;
    }
}
