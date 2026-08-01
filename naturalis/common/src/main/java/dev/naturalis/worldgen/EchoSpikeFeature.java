package dev.naturalis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates tilted sculk spikes for the Natural Echo biome.
 *
 * <ul>
 *   <li>Height: 8–20 blocks</li>
 *   <li>Tilt: 20–40° from vertical (random horizontal direction)</li>
 *   <li>Shape: sharp – radius-2 base, radius-1 for 2 more layers, single-block shaft</li>
 *   <li>Material: {@link Blocks#SCULK} shaft + {@link Blocks#SCULK_CATALYST} base</li>
 * </ul>
 */
public final class EchoSpikeFeature extends Feature<NoneFeatureConfiguration> {

    private static final BlockState SCULK          = Blocks.SCULK.defaultBlockState();
    private static final BlockState SCULK_CATALYST = Blocks.SCULK_CATALYST.defaultBlockState();

    public EchoSpikeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level  = ctx.level();
        BlockPos       origin = ctx.origin();
        RandomSource   random = ctx.random();

        int   height   = 8 + random.nextInt(13);                          // 8–20
        float tiltDeg  = 20f + random.nextFloat() * 20f;                  // 20°–40°
        float tiltSin  = (float) Math.sin(Math.toRadians(tiltDeg));       // horizontal step per Y block
        float dirAngle = random.nextFloat() * (float) (2 * Math.PI);
        float stepX    = tiltSin * (float) Math.cos(dirAngle);
        float stepZ    = tiltSin * (float) Math.sin(dirAngle);

        // Mark the root with a sculk catalyst – also acts as a feature trigger anchor
        level.setBlock(origin, SCULK_CATALYST, 2);

        for (int y = 0; y < height; y++) {
            int cx = origin.getX() + Math.round(y * stepX);
            int cz = origin.getZ() + Math.round(y * stepZ);
            int cy = origin.getY() + y;

            // Taper profile: wide base → single shaft
            int radius = (y == 0) ? 2 : (y <= 2 ? 1 : 0);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue; // circular section
                    BlockPos bp = new BlockPos(cx + dx, cy, cz + dz);
                    BlockState existing = level.getBlockState(bp);
                    // Place sculk in air / non-solid; don't overwrite solid terrain
                    if (!existing.blocksMotion() || existing.isAir()) {
                        level.setBlock(bp, SCULK, 2);
                    }
                }
            }
        }
        return true;
    }
}
