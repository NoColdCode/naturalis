package dev.naturalis.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads the island heightmap and biome map PNGs once at startup and provides
 * fast per-block queries used by {@link IslandChunkGenerator} and
 * {@link IslandBiomeSource}.
 *
 * <p>PNG files must be placed in:
 * <pre>
 *   resources/assets/naturalis/island/heightmap.png  (grayscale, 1px = 1 block)
 *   resources/assets/naturalis/island/biome_map.png  (colour-coded biomes)
 * </pre>
 *
 * Height mapping: gray 0 → Y_MIN (-64), gray 255 → Y_MAX (320).
 * "Outside island" is detected by the biome map being black (R+G+B &lt; 30).
 */
public final class IslandHeightmap {

    private static final Logger LOGGER = LoggerFactory.getLogger(IslandHeightmap.class);
    /** Guards the one-shot WARN when get() is called before loading completes. */
    private static final AtomicBoolean EARLY_GET_WARNED = new AtomicBoolean(false);

    // ── Y range ─────────────────────────────────────────────────────────────
    public static final int Y_MIN      = -64;
    public static final int Y_MAX      = 320;
    /**
     * The highest Y that the heightmap PNG can produce.  By keeping this at
     * Y_MIN + 256 = 192 we get exactly one block per gray level (0→-64, 255→192),
     * so the terrain never has the 2-block stair-step artefact.  Players can still
     * build above y=192; the dimension height is unchanged.
     * Paint your mountain peaks white (gray=255) → y=192.
     * Paint average open terrain at gray≈128 → y=64 (sea level).
     */
    public static final int HEIGHT_MAP_MAX = Y_MIN + 256; // = 192
    /** Water level inside the island (fills depressions up to this Y). */
    public static final int WATER_LEVEL = 64;

    // ── Island centering offset (override if you want the island off-centre) ─
    public static final int ISLAND_OFFSET_X = 0;
    public static final int ISLAND_OFFSET_Z = 0;

    // ── Biome colour table ───────────────────────────────────────────────────
    // Each entry is 0xRRGGBB → biome ResourceKey.
    // Uses nearest-colour matching so approximate values work fine.
    private static final Map<Integer, ResourceKey<Biome>> COLOUR_MAP = new LinkedHashMap<>();
    static {
        COLOUR_MAP.put(0x006400, NaturalDimensionKeys.DENSE_FOREST);   // dark green
        COLOUR_MAP.put(0x3C8C28, NaturalDimensionKeys.NATURAL_PLAIN);  // medium green
        COLOUR_MAP.put(0x00DC3C, NaturalDimensionKeys.JUNGLE_REAL);    // bright green
        COLOUR_MAP.put(0xC80000, NaturalDimensionKeys.VOLCANO);        // red
        COLOUR_MAP.put(0x7850A0, NaturalDimensionKeys.ENDER_FOREST);   // purple
        COLOUR_MAP.put(0xC8B464, NaturalDimensionKeys.SNOWY_MOUNTAIN); // light yellow
        COLOUR_MAP.put(0x966432, NaturalDimensionKeys.ARID_SAVANNA);   // tan/brown
        COLOUR_MAP.put(0x00C8C8, NaturalDimensionKeys.CORAL_WATER);    // cyan
        COLOUR_MAP.put(0x000096, NaturalDimensionKeys.DEEP_WATER);     // dark blue
        COLOUR_MAP.put(0x3C3C3C, NaturalDimensionKeys.DARK_CAVES);     // very dark grey
        COLOUR_MAP.put(0xB4B4B4, NaturalDimensionKeys.NATURAL_ECHO);   // light grey
        COLOUR_MAP.put(0x646469, NaturalDimensionKeys.HIGH_PEAK);      // medium dark grey
        COLOUR_MAP.put(0xF0E8C8, NaturalDimensionKeys.NATURAL_BEACH);  // sandy white
    }

    // ── Cached singleton ─────────────────────────────────────────────────────
    private static volatile IslandHeightmap instance;

    // ── Internal state ───────────────────────────────────────────────────────
    private final int imgWidth;
    private final int imgHeight;
    /** surfaceY per pixel, or Integer.MIN_VALUE if outside island. */
    private final int[] surfaceY;
    /** Biome key index per pixel (into BIOME_KEYS). Integer.MIN_VALUE if outside. */
    private final ResourceKey<Biome>[] biomeKeys;
    /**
     * "Edge distance" per pixel: how many pixels away from the nearest outside
     * pixel. Used to taper the island bottom.
     */
    private final int[] edgeDist;
    /** Mass centroid (block X/Z) of {@link NaturalDimensionKeys#NATURAL_ECHO} on the biome map — arena & boss anchor. */
    private final int echoArenaBlockX;
    private final int echoArenaBlockZ;

    // ────────────────────────────────────────────────────────────────────────
    //  Factory
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Non-blocking. Returns the loaded heightmap if the background prewarm is done,
     * otherwise returns a flat fallback immediately — NEVER blocks any thread.
     * Logs a one-shot WARN with full stack trace the first time it is called before
     * loading completes, so you can identify unexpected callers in the logs.
     */
    public static IslandHeightmap get() {
        IslandHeightmap hm = instance;
        if (hm != null) return hm;
        // Not loaded yet — log once so we can identify the caller, then return fallback
        if (EARLY_GET_WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[naturalis-heightmap] get() called on thread '{}' before load completed — "
                + "returning flat fallback. Stack trace:",
                Thread.currentThread().getName(),
                new Exception("IslandHeightmap.get() called before prewarm finished"));
        }
        return buildFallback();
    }

    /**
     * Blocking accessor for world-gen threads. Blocks until the heightmap is fully
     * loaded. Must NOT be called from the render/client thread.
     */
    public static IslandHeightmap getOrLoad() {
        if (instance == null) {
            synchronized (IslandHeightmap.class) {
                if (instance == null) {
                    instance = load();
                }
            }
        }
        return instance;
    }

    /**
     * Non-blocking accessor: returns the loaded instance, or {@code null} if
     * the background prewarm has not yet finished.  Safe to call from any thread.
     */
    public static IslandHeightmap tryGet() {
        return instance;
    }

    /** Called on server shutdown / world unload to free memory. */
    public static void reset() {
        instance = null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Construction
    // ────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private IslandHeightmap(int w, int h, int[] surfaceY, ResourceKey<Biome>[] biomeKeys, int[] edgeDist,
            int echoArenaBlockX, int echoArenaBlockZ) {
        this.imgWidth  = w;
        this.imgHeight = h;
        this.surfaceY  = surfaceY;
        this.biomeKeys = biomeKeys;
        this.edgeDist  = edgeDist;
        this.echoArenaBlockX = echoArenaBlockX;
        this.echoArenaBlockZ = echoArenaBlockZ;
    }

    @SuppressWarnings("unchecked")
    private static IslandHeightmap load() {
        long t0 = System.currentTimeMillis();
        LOGGER.info("[naturalis-heightmap] load() starting on thread '{}'", Thread.currentThread().getName());
        BufferedImage heightImg = readImage("/assets/naturalis/island/heightmap.png");
        BufferedImage biomeImg  = readImage("/assets/naturalis/island/biome_map.png");
        long tPngMs = System.currentTimeMillis() - t0;
        LOGGER.info("[naturalis-heightmap] PNG read in {}ms (height={}x{} biome={}x{})",
            tPngMs,
            heightImg != null ? heightImg.getWidth() : 0, heightImg != null ? heightImg.getHeight() : 0,
            biomeImg  != null ? biomeImg.getWidth()  : 0, biomeImg  != null ? biomeImg.getHeight()  : 0);

        if (heightImg == null || biomeImg == null) {
            LOGGER.error("[Naturalis] Missing island PNG! Generating fallback (flat stone island 256x256).");
            return buildFallback();
        }

        int w = biomeImg.getWidth();
        int h = biomeImg.getHeight();

        // Resize heightmap to match biome map if needed
        if (heightImg.getWidth() != w || heightImg.getHeight() != h) {
            java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.drawImage(heightImg, 0, 0, w, h, null);
            g.dispose();
            heightImg = scaled;
        }

        int size = w * h;
        int[]                surfaceY  = new int[size];
        ResourceKey<Biome>[] biomeKeys = new ResourceKey[size];
        boolean[]            inside    = new boolean[size];

        // Bulk-read all pixels at once — getRGB(x,y) in a loop is ~100x slower
        int[] biomePixels  = biomeImg.getRGB(0, 0, w, h, null, 0, w);
        int[] heightPixels = heightImg.getRGB(0, 0, w, h, null, 0, w);

        for (int idx = 0; idx < size; idx++) {
            int bRgb = biomePixels[idx] & 0xFFFFFF;
            int r = (bRgb >> 16) & 0xFF;
            int g = (bRgb >>  8) & 0xFF;
            int b =  bRgb        & 0xFF;

            if (r + g + b < 30) {
                // Outside island
                surfaceY[idx]  = Integer.MIN_VALUE;
                biomeKeys[idx] = NaturalDimensionKeys.NATURAL_ECHO;
                inside[idx]    = false;
            } else {
                inside[idx]    = true;
                biomeKeys[idx] = nearestBiome(bRgb);

                int gray = (heightPixels[idx] >> 16) & 0xFF; // R channel = gray
                // Math.round gives symmetric rounding — reduces the 1-vs-2-block step
                // artifact that arises because 384 world blocks map to only 256 gray levels.
                surfaceY[idx] = (int) Math.round(Y_MIN + (gray / 255.0) * (HEIGHT_MAP_MAX - Y_MIN));
            }
        }

        long tLoopMs = System.currentTimeMillis() - t0 - tPngMs;
        LOGGER.info("[naturalis-heightmap] pixel loop done in {}ms", tLoopMs);

        // ── Patch isolated outside pixels (stray "holes" at island edge) ────
        // Any outside pixel fully surrounded by inside pixels is filled in.
        for (int pz = 1; pz < h - 1; pz++) {
            for (int px = 1; px < w - 1; px++) {
                int idx = pz * w + px;
                if (inside[idx]) continue;
                boolean allIn = inside[pz*w+(px-1)] && inside[pz*w+(px+1)]
                             && inside[(pz-1)*w+px] && inside[(pz+1)*w+px];
                if (allIn) {
                    inside[idx]    = true;
                    biomeKeys[idx] = biomeKeys[pz*w+(px-1)]; // inherit neighbour
                    // Average height of 4 neighbours
                    surfaceY[idx]  = (surfaceY[pz*w+(px-1)] + surfaceY[pz*w+(px+1)]
                                    + surfaceY[(pz-1)*w+px] + surfaceY[(pz+1)*w+px]) / 4;
                }
            }
        }

        // ── Clean stray biome pixels (3-pass majority-vote) ──────────────────
        // Pixels whose biome doesn't match any cardinal neighbour are replaced
        // with the most-common neighbour biome.  Three passes eliminate clusters
        // up to 3 pixels wide, removing JPEG/PNG edge artefacts in the biome map.
        for (int pass = 0; pass < 3; pass++) {
            @SuppressWarnings("unchecked")
            ResourceKey<Biome>[] cleaned = (ResourceKey<Biome>[]) new ResourceKey[size];
            System.arraycopy(biomeKeys, 0, cleaned, 0, size);
            for (int pz = 1; pz < h - 1; pz++) {
                for (int px = 1; px < w - 1; px++) {
                    int idx = pz * w + px;
                    if (!inside[idx]) continue;
                    ResourceKey<Biome> me = biomeKeys[idx];
                    int[] ns = {pz*w+(px-1), pz*w+(px+1), (pz-1)*w+px, (pz+1)*w+px};
                    boolean hasMatch = false;
                    for (int ni : ns) {
                        if (inside[ni] && me.equals(biomeKeys[ni])) { hasMatch = true; break; }
                    }
                    if (hasMatch) continue;
                    // Replace with most-frequent neighbour biome
                    java.util.Map<ResourceKey<Biome>, Integer> freq = new java.util.HashMap<>(4);
                    for (int ni : ns) {
                        if (inside[ni]) freq.merge(biomeKeys[ni], 1, Integer::sum);
                    }
                    ResourceKey<Biome> best = me;
                    int bestCnt = 0;
                    for (java.util.Map.Entry<ResourceKey<Biome>, Integer> e : freq.entrySet()) {
                        if (e.getValue() > bestCnt) { bestCnt = e.getValue(); best = e.getKey(); }
                    }
                    cleaned[idx] = best;
                }
            }
            System.arraycopy(cleaned, 0, biomeKeys, 0, size);
        }

        // ── Box-blur the heightmap (radius 5) for smooth biome transitions ──
        // Runs entirely at load time so there is no per-block cost in-game.
        // Dramatic biomes (high_peak, volcano, snowy_mountain) keep 80% of their
        // original height so blur does not shave their peaks; all others fully blur.
        int[] blurred = blurHeightmap(surfaceY, inside, w, h, 5);
        for (int idx = 0; idx < size; idx++) {
            if (!inside[idx]) continue;
            int orig = surfaceY[idx], bv = blurred[idx];
            ResourceKey<Biome> bk = biomeKeys[idx];
            boolean isDramatic = NaturalDimensionKeys.HIGH_PEAK.equals(bk)
                || NaturalDimensionKeys.VOLCANO.equals(bk)
                || NaturalDimensionKeys.SNOWY_MOUNTAIN.equals(bk);
            if (isDramatic && orig > 100) {
                // 80% original, 20% blur — keeps transition smooth but preserves
                // the full altitude of mountain and volcano peaks.
                surfaceY[idx] = (orig * 4 + bv) / 5;
            } else if (orig > 120) {
                // 40% original, 60% blurred for other tall areas
                surfaceY[idx] = (orig * 2 + bv * 3) / 5;
            } else {
                surfaceY[idx] = bv;
            }
        }

        // ── Mountain peak roughening ──────────────────────────────────────────
        // Re-add coherent short-range height variation to high-altitude pixels
        // that the blur smoothed out. Creates spiky, irregular summit ridges.
        // HIGH_PEAK gets a separate strong short-wavelength pass for abrupt cliffs.
        final int PEAK_THRESHOLD = 110; // surfY above this gets generic roughening
        for (int idx = 0; idx < size; idx++) {
            if (!inside[idx]) continue;
            int sy = surfaceY[idx];
            int px = idx % w, pz = idx / w;

            // Ender forest stays smooth — skip all roughening.
            if (NaturalDimensionKeys.ENDER_FOREST.equals(biomeKeys[idx])) continue;

            // HIGH_PEAK: strong short-wavelength noise for dramatic spikes & pits.
            if (NaturalDimensionKeys.HIGH_PEAK.equals(biomeKeys[idx]) && sy > 70) {
                double hp1 = smoothNoise(px / 3.0 + 137.0, pz / 3.0 + 211.0);
                double hp2 = smoothNoise(px / 1.8 + 53.0,  pz / 1.8 + 97.0);
                double hp3 = smoothNoise(px / 5.5 + 331.0, pz / 5.5 + 179.0);
                int hpDelta = (int)((hp1 * 0.45 + hp2 * 0.35 + hp3 * 0.20) * 28.0);
                surfaceY[idx] = Math.min(HEIGHT_MAP_MAX, Math.max(50, sy + hpDelta));
                continue; // skip generic roughening for this pixel
            }

            // Generic: medium ridges + fine spikes for all other high-altitude pixels.
            if (sy <= PEAK_THRESHOLD) continue;
            // Two octaves: medium ridges + fine spikes
            double n1 = smoothNoise(px / 6.0,         pz / 6.0);
            double n2 = smoothNoise(px / 2.5 + 47.0,  pz / 2.5 + 83.0);
            double combined = n1 * 0.6 + n2 * 0.4;
            // Strength grows with altitude; max ±10 blocks of variation
            double strength = Math.min(1.0, (sy - PEAK_THRESHOLD) / 60.0);
            int delta = (int)(combined * 10.0 * strength);
            surfaceY[idx] = Math.min(HEIGHT_MAP_MAX, Math.max(PEAK_THRESHOLD, sy + delta));
        }

        // ── Force coral reef floor below water level ──────────────────────────
        // If the PNG paints coral_water at or above sea level (gray ≥ 128), lower
        // it to 5 blocks below water so the reef is always submerged.
        for (int idx = 0; idx < size; idx++) {
            if (!inside[idx]) continue;
            if (NaturalDimensionKeys.CORAL_WATER.equals(biomeKeys[idx])
                    && surfaceY[idx] >= WATER_LEVEL - 3) {
                // Add a tiny noise variation so the reef floor isn't perfectly flat
                int px = idx % w, pz = idx / w;
                double fn = smoothNoise(px / 10.0 + 91.0, pz / 10.0 + 47.0);
                surfaceY[idx] = WATER_LEVEL - 6 + (int)(fn * 2.0);
            }
        }

        long tBfs0 = System.currentTimeMillis();
        int[] edgeDist = computeEdgeDistance(inside, w, h);
        long tBfsMs = System.currentTimeMillis() - tBfs0;

        long echoSumX = 0L;
        long echoSumZ = 0L;
        int echoCnt = 0;
        for (int pz = 0; pz < h; pz++) {
            for (int px = 0; px < w; px++) {
                int idx = pz * w + px;
                if (!inside[idx]) {
                    continue;
                }
                if (!NaturalDimensionKeys.NATURAL_ECHO.equals(biomeKeys[idx])) {
                    continue;
                }
                int bx = px - w / 2 - ISLAND_OFFSET_X;
                int bz = pz - h / 2 - ISLAND_OFFSET_Z;
                echoSumX += bx;
                echoSumZ += bz;
                echoCnt++;
            }
        }
        int echoArenaX = echoCnt > 0 ? (int) Math.round(echoSumX / (double) echoCnt) : 0;
        int echoArenaZ = echoCnt > 0 ? (int) Math.round(echoSumZ / (double) echoCnt) : 0;
        if (echoCnt > 0) {
            LOGGER.info("[naturalis-heightmap] NATURAL_ECHO centroid block=({}, {}) from {} columns",
                echoArenaX, echoArenaZ, echoCnt);
        }

        long elapsed = System.currentTimeMillis() - t0;
        LOGGER.info("[naturalis-heightmap] loaded ({}x{}) in {}ms total (png={}ms loop={}ms bfs={}ms) on thread '{}'.",
            w, h, elapsed, tPngMs, tLoopMs, tBfsMs, Thread.currentThread().getName());
        return new IslandHeightmap(w, h, surfaceY, biomeKeys, edgeDist, echoArenaX, echoArenaZ);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Public queries
    // ────────────────────────────────────────────────────────────────────────

    /** True if this block column is part of the floating island. */
    public boolean isInsideIsland(int blockX, int blockZ) {
        int idx = blockToIndex(blockX, blockZ);
        return idx >= 0 && surfaceY[idx] != Integer.MIN_VALUE;
    }

    /**
     * Surface Y for this column (the topmost solid block before surface
     * dressing). Returns {@code Y_MIN} if outside the island.
     */
    public int getSurfaceY(int blockX, int blockZ) {
        int idx = blockToIndex(blockX, blockZ);
        if (idx < 0 || surfaceY[idx] == Integer.MIN_VALUE) return Y_MIN;
        return surfaceY[idx];
    }

    /**
     * Bottom Y of the floating island for this column (below this is void).
     * Tapers toward the island edge to create a classic floating-island look,
     * with organic noise added so the underside looks irregular rather than
     * mathematically smooth.
     */
    public int getBottomY(int blockX, int blockZ) {
        int idx = blockToIndex(blockX, blockZ);
        if (idx < 0 || surfaceY[idx] == Integer.MIN_VALUE) return Y_MIN;

        int surf = surfaceY[idx];
        int dist = edgeDist[idx]; // pixels from nearest outside pixel

        // edgeFactor: 0 at the very edge pixel, 1 when ≥ 100 pixels from edge.
        // Quintic curve gives a smooth knife-edge at the rim that fattens quickly
        // toward the island centre.
        double ef  = Math.min(1.0, dist / 100.0);
        double ef5 = ef * ef * ef * ef * ef; // quintic — nearly zero at rim

        // Thickness: 0 at rim → 180 at deep centre.
        // Starting at 0 makes the island edge look like a thin blade, not a slab.
        int thickness = (int)(ef5 * 180);

        // ── Organic underside noise (two-frequency value noise) ──────────────
        // First octave – large swells (~48-block wavelength)
        double n1 = smoothNoise(blockX / 48.0, blockZ / 48.0);
        // Second octave – medium bumps (~20-block wavelength), half amplitude
        double n2 = smoothNoise(blockX / 20.0 + 73.3, blockZ / 20.0 + 31.7);
        // Third octave – fine detail (~9-block wavelength), quarter amplitude
        double n3 = smoothNoise(blockX / 9.0  + 157.1, blockZ / 9.0  + 89.5);
        // Combine, scale by thickness so edges are still thin
        int noiseOffset = (int)((n1 * 0.5 + n2 * 0.3 + n3 * 0.2) * ef5 * 28);

        int bottom = surf - thickness - noiseOffset;

        // Must always be at least 4 blocks below surface (never poke through top)
        return Math.min(bottom, surf - 4);
    }

    // ── Smooth value noise helper ─────────────────────────────────────────────

    /** Bilinearly-interpolated value noise in [-1, 1]. */
    private static double smoothNoise(double x, double z) {
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double fx = x - ix, fz = z - iz;
        // Smoothstep
        double ux = fx * fx * (3 - 2 * fx);
        double uz = fz * fz * (3 - 2 * fz);
        double n00 = hash2d(ix,   iz  );
        double n10 = hash2d(ix+1, iz  );
        double n01 = hash2d(ix,   iz+1);
        double n11 = hash2d(ix+1, iz+1);
        return n00*(1-ux)*(1-uz) + n10*ux*(1-uz) + n01*(1-ux)*uz + n11*ux*uz;
    }

    /** Returns a pseudo-random double in [-1, 1] for integer grid point (ix, iz). */
    private static double hash2d(int ix, int iz) {
        long h = (long)ix * 2654435761L ^ (long)iz * 1664525L;
        h ^= (h >>> 30); h *= 0xbf58476d1ce4e5b9L;
        h ^= (h >>> 27); h *= 0x94d049bb133111ebL;
        h ^= (h >>> 31);
        return (h & 0xFFFFFFL) / (double)0x800000L - 1.0; // [-1, 1]
    }

    /** Biome key at this block column (from the colour-coded biome map). */
    public ResourceKey<Biome> getBiomeKey(int blockX, int blockZ) {
        int idx = blockToIndex(blockX, blockZ);
        if (idx < 0) return NaturalDimensionKeys.NATURAL_ECHO;
        return biomeKeys[idx];
    }

    /** Block X of the NATURAL_ECHO biome centroid (surface Y via {@link #getSurfaceY(int, int)}). */
    public int getEchoArenaBlockX() {
        return echoArenaBlockX;
    }

    /** Block Z of the NATURAL_ECHO biome centroid. */
    public int getEchoArenaBlockZ() {
        return echoArenaBlockZ;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ────────────────────────────────────────────────────────────────────────

    private int blockToIndex(int blockX, int blockZ) {
        int px = blockX + ISLAND_OFFSET_X + imgWidth  / 2;
        int pz = blockZ + ISLAND_OFFSET_Z + imgHeight / 2;
        if (px < 0 || px >= imgWidth || pz < 0 || pz >= imgHeight) return -1;
        return pz * imgWidth + px;
    }

    private static ResourceKey<Biome> nearestBiome(int rgb) {
        int bestDist  = Integer.MAX_VALUE;
        ResourceKey<Biome> best = NaturalDimensionKeys.NATURAL_ECHO;
        int r1 = (rgb >> 16) & 0xFF;
        int g1 = (rgb >>  8) & 0xFF;
        int b1 =  rgb        & 0xFF;

        for (Map.Entry<Integer, ResourceKey<Biome>> e : COLOUR_MAP.entrySet()) {
            int c  = e.getKey();
            int dr = r1 - ((c >> 16) & 0xFF);
            int dg = g1 - ((c >>  8) & 0xFF);
            int db = b1 - (c & 0xFF);
            int d  = dr*dr + dg*dg + db*db;
            if (d < bestDist) {
                bestDist = d;
                best     = e.getValue();
            }
        }
        return best;
    }

    /**
     * Box-blur pass over the surfaceY array.  Outside pixels (Integer.MIN_VALUE)
     * are excluded from the average so the island edge stays sharp, but interior
     * heights near biome boundaries are smoothed.
     */
    private static int[] blurHeightmap(int[] surfaceY, boolean[] inside, int w, int h, int radius) {
        int[] result = new int[w * h];
        for (int pz = 0; pz < h; pz++) {
            for (int px = 0; px < w; px++) {
                int idx = pz * w + px;
                if (!inside[idx]) {
                    result[idx] = Integer.MIN_VALUE;
                    continue;
                }
                long sum = 0;
                int  cnt = 0;
                for (int dz = -radius; dz <= radius; dz++) {
                    int nz = pz + dz;
                    if (nz < 0 || nz >= h) continue;
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = px + dx;
                        if (nx < 0 || nx >= w) continue;
                        int ni = nz * w + nx;
                        if (inside[ni]) { sum += surfaceY[ni]; cnt++; }
                    }
                }
                result[idx] = cnt > 0 ? (int)(sum / cnt) : surfaceY[idx];
            }
        }
        return result;
    }

    /**
     * BFS from outside pixels to compute per-pixel distance to nearest outside.
     * Cap at 200 to avoid huge int values; the tap formula only needs ~100.
     */
    private static int[] computeEdgeDistance(boolean[] inside, int w, int h) {
        int[] dist = new int[w * h];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();

        // Seed: every inside pixel adjacent to an outside pixel gets dist=1
        for (int pz = 0; pz < h; pz++) {
            for (int px = 0; px < w; px++) {
                int idx = pz * w + px;
                if (!inside[idx]) {
                    dist[idx] = 0;
                } else {
                    dist[idx] = Integer.MAX_VALUE; // unvisited
                    // Check if adjacent to outside
                    boolean edgePixel = false;
                    if (px > 0   && !inside[pz*w+(px-1)]) edgePixel = true;
                    if (px < w-1 && !inside[pz*w+(px+1)]) edgePixel = true;
                    if (pz > 0   && !inside[(pz-1)*w+px]) edgePixel = true;
                    if (pz < h-1 && !inside[(pz+1)*w+px]) edgePixel = true;
                    if (edgePixel) {
                        dist[idx] = 1;
                        queue.add(idx);
                    }
                }
            }
        }

        // BFS inward — each pixel enqueued at most once (unit-weight BFS is O(V))
        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int pz  = idx / w;
            int px  = idx % w;
            int d   = dist[idx];
            if (d >= 200) continue;  // cap propagation at 200
            int nd  = d + 1;

            if (px > 0) {
                int n = idx - 1;
                if (inside[n] && dist[n] == Integer.MAX_VALUE) { dist[n] = nd; queue.add(n); }
            }
            if (px < w - 1) {
                int n = idx + 1;
                if (inside[n] && dist[n] == Integer.MAX_VALUE) { dist[n] = nd; queue.add(n); }
            }
            if (pz > 0) {
                int n = idx - w;
                if (inside[n] && dist[n] == Integer.MAX_VALUE) { dist[n] = nd; queue.add(n); }
            }
            if (pz < h - 1) {
                int n = idx + w;
                if (inside[n] && dist[n] == Integer.MAX_VALUE) { dist[n] = nd; queue.add(n); }
            }
        }
        // Replace any still-unvisited inside pixels (isolated) with distance 1
        for (int i = 0; i < dist.length; i++) {
            if (inside[i] && dist[i] == Integer.MAX_VALUE) dist[i] = 1;
        }
        return dist;
    }

    private static BufferedImage readImage(String resourcePath) {
        try (InputStream is = IslandHeightmap.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.error("[Naturalis] Could not find resource: {}", resourcePath);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            LOGGER.error("[Naturalis] Failed to read image {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }

    /** Flat 256×256 stone island centred at origin, used as a safety fallback. */
    @SuppressWarnings("unchecked")
    private static IslandHeightmap buildFallback() {
        int w = 256, h = 256;
        int size = w * h;
        int[] sy = new int[size];
        ResourceKey<Biome>[] bk = new ResourceKey[size];
        boolean[] inside = new boolean[size];

        java.util.Arrays.fill(sy, 64);
        java.util.Arrays.fill(bk, NaturalDimensionKeys.NATURAL_PLAIN);
        java.util.Arrays.fill(inside, true);

        int[] ed = computeEdgeDistance(inside, w, h);
        return new IslandHeightmap(w, h, sy, bk, ed, 0, 0);
    }
}
