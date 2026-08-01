package dev.naturalis.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

/**
 * Flat circular arena (Ø 20) for the Echo Sovereign at the {@link NaturalDimensionKeys#NATURAL_ECHO}
 * biome centroid (from the island biome map), not necessarily world origin.
 * Ancient-city palette: deepslate bricks / cobbled floor, ring wall with cardinal gateways.
 */
public final class EchoArenaGenerator {

    private static final int GEN_FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;

    private EchoArenaGenerator() {
    }

    public static void tryGenerate(WorldGenLevel level, ChunkPos chunkPos, IslandHeightmap hm) {
        if (hm == null) {
            return;
        }
        int cx = hm.getEchoArenaBlockX();
        int cz = hm.getEchoArenaBlockZ();
        ChunkPos arenaChunk = new ChunkPos(cx >> 4, cz >> 4);
        if (chunkPos.x != arenaChunk.x || chunkPos.z != arenaChunk.z) {
            return;
        }
        if (!hm.isInsideIsland(cx, cz)) {
            return;
        }
        if (!NaturalDimensionKeys.NATURAL_ECHO.equals(hm.getBiomeKey(cx, cz))) {
            return;
        }

        int surfY = hm.getSurfaceY(cx, cz);
        BlockState floorMain = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState floorAlt = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState wallCracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState deco = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int R = 10;
        int wallH = 3;

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > (R + 2) * (R + 2)) {
                    continue;
                }

                double dist = Math.sqrt(distSq);
                boolean gateway = isGatewayOpening(dx, dz, R);

                // Floor disk — flatten interior
                if (distSq <= R * R) {
                    BlockPos fp = new BlockPos(cx + dx, surfY, cz + dz);
                    BlockState top = ((dx + dz) & 1) == 0 ? floorMain : floorAlt;
                    level.setBlock(fp, top, GEN_FLAGS);
                    for (int dy = 1; dy <= 2; dy++) {
                        level.setBlock(fp.below(dy), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), GEN_FLAGS);
                    }
                }

                // Wall ring (approximate circle), openings at ±Z and ±X
                if (dist >= R - 0.35 && dist <= R + 1.25 && !gateway) {
                    for (int h = 1; h <= wallH; h++) {
                        BlockPos wp = new BlockPos(cx + dx, surfY + h, cz + dz);
                        boolean accent = h == wallH && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1);
                        level.setBlock(wp, accent ? wallCracked : wall, GEN_FLAGS);
                    }
                    // Crenellation / cap
                    if ((dx + dz + surfY) % 3 == 0) {
                        BlockPos cp = new BlockPos(cx + dx, surfY + wallH + 1, cz + dz);
                        level.setBlock(cp, deco, GEN_FLAGS);
                    }
                }
            }
        }

        // Lanterns outside gateways
        placeLantern(level, new BlockPos(cx, surfY + 4, cz + R + 2), lantern);
        placeLantern(level, new BlockPos(cx, surfY + 4, cz - R - 2), lantern);
        placeLantern(level, new BlockPos(cx + R + 2, surfY + 4, cz), lantern);
        placeLantern(level, new BlockPos(cx - R - 2, surfY + 4, cz), lantern);

        // Clear column above arena centre for boss headroom
        for (int dy = 1; dy <= 8; dy++) {
            level.setBlock(new BlockPos(cx, surfY + dy, cz), air, GEN_FLAGS);
        }
        // Clear gateway arches (two blocks wide, three tall)
        clearGateway(level, cx, cz, surfY, R, air);
    }

    private static void clearGateway(WorldGenLevel level, int cx, int cz, int surfY, int r, BlockState air) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int gx = d[0] * r;
            int gz = d[1] * r;
            for (int o = -1; o <= 1; o++) {
                for (int h = 1; h <= 4; h++) {
                    int px = cx + gx + (d[0] == 0 ? o : 0);
                    int pz = cz + gz + (d[1] == 0 ? o : 0);
                    level.setBlock(new BlockPos(px, surfY + h, pz), air, GEN_FLAGS);
                }
            }
        }
    }

    private static boolean isGatewayOpening(int dx, int dz, int r) {
        // Four cardinal openings, two blocks wide at the ring
        if (Math.abs(dx) <= 1 && dz >= r - 1 && dz <= r + 1) {
            return true;
        }
        if (Math.abs(dx) <= 1 && dz <= -r + 1 && dz >= -r - 1) {
            return true;
        }
        if (Math.abs(dz) <= 1 && dx >= r - 1 && dx <= r + 1) {
            return true;
        }
        return Math.abs(dz) <= 1 && dx <= -r + 1 && dx >= -r - 1;
    }

    private static void placeLantern(WorldGenLevel level, BlockPos pos, BlockState lantern) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        level.setBlock(pos, lantern, GEN_FLAGS);
    }

    public static BlockPos arenaBossSpawn(WorldGenLevel level, IslandHeightmap hm) {
        if (hm == null) {
            return BlockPos.ZERO;
        }
        int ax = hm.getEchoArenaBlockX();
        int az = hm.getEchoArenaBlockZ();
        if (!hm.isInsideIsland(ax, az)) {
            return BlockPos.ZERO;
        }
        int y = hm.getSurfaceY(ax, az);
        return new BlockPos(ax, y + 1, az);
    }
}
