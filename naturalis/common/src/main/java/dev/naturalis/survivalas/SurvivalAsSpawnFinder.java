package dev.naturalis.survivalas;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Finds a spawn suited to the locked morph (plains for cows, ocean for fish, nether/end for endermen, etc.).
 */
public final class SurvivalAsSpawnFinder {

    private SurvivalAsSpawnFinder() {
    }

    public static void relocatePlayer(ServerPlayer player, ResourceLocation morphId) {
        MinecraftServer server = player.getServer();
        if (server == null || morphId == null) {
            return;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        Preference pref = Preference.forMorph(morphId, type);

        ServerLevel targetLevel = server.getLevel(pref.dimension);
        if (targetLevel == null) {
            targetLevel = server.overworld();
            pref = Preference.overworldFallback(morphId, type);
        }

        BlockPos found = search(targetLevel, pref, morphId, type);
        if (found == null) {
            found = targetLevel.getSharedSpawnPos();
        }
        int y = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
        if (pref.preferCave) {
            y = Math.min(y, targetLevel.getSeaLevel() - 8);
            y = Math.max(CompatAccess.getMinBuildHeight(targetLevel) + 8, y);
        }
        BlockPos spawn = new BlockPos(found.getX(), y, found.getZ());
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (targetLevel != player.level()) {
            CompatAccess.teleportCrossDimension(
                player, targetLevel,
                spawn.getX() + 0.5D, spawn.getY() + 0.1D, spawn.getZ() + 0.5D,
                yaw, pitch
            );
        } else {
            player.teleportTo(spawn.getX() + 0.5D, spawn.getY() + 0.1D, spawn.getZ() + 0.5D);
        }

        CompatAccess.setRespawnPosition(player, targetLevel.dimension(), spawn, yaw, true, false);
        try {
            targetLevel.setDefaultSpawnPos(spawn, yaw);
        } catch (Throwable ignored) {
        }
    }

    private static BlockPos search(ServerLevel level, Preference pref, ResourceLocation morphId, EntityType<?> type) {
        BlockPos origin = level.getSharedSpawnPos();
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        int step = 48;
        int maxR = 2048;

        for (int r = 0; r <= maxR; r += step) {
            int samples = r == 0 ? 1 : 8;
            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2.0D * i) / samples;
                int x = origin.getX() + (int) Math.round(Math.cos(angle) * r);
                int z = origin.getZ() + (int) Math.round(Math.sin(angle) * r);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                int score = score(level, pos, pref, morphId, type);
                if (score > bestScore) {
                    bestScore = score;
                    best = pos;
                }
            }
            if (bestScore >= 80) {
                break;
            }
        }
        return best;
    }

    private static int score(ServerLevel level, BlockPos pos, Preference pref, ResourceLocation morphId, EntityType<?> type) {
        Holder<Biome> biome = level.getBiome(pos);
        int score = 0;
        String path = morphId.getPath();

        if (pref.aquatic) {
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) {
                score += 90;
            }
            if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                || level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER)) {
                score += 40;
            }
        }
        if (pref.netherNative) {
            if (biome.is(BiomeTags.IS_NETHER)) {
                score += 70;
            }
            ResourceLocation biomeId = biome.unwrapKey().map(ResourceKey::location).orElse(null);
            if (biomeId != null) {
                String bp = biomeId.getPath();
                if (bp.contains("crimson") || bp.contains("soul") || bp.contains("basalt") || bp.contains("wastes")) {
                    score += 20;
                }
                if (path.contains("enderman") && bp.contains("warped")) {
                    score += 50;
                }
            }
        }
        if (pref.ender) {
            if (level.dimension() == ServerLevel.END) {
                score += 90;
            }
        }
        if (pref.cold) {
            if (biome.is(BiomeTags.IS_MOUNTAIN) || isColdBiome(biome.value(), pos)) {
                score += 70;
            }
        }
        if (pref.desert) {
            if (biome.value().getBaseTemperature() >= 1.5F) {
                score += 70;
            }
        }
        if (pref.forest) {
            if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)) {
                score += 70;
            }
        }
        if (pref.preferCave) {
            score += 20;
            if (path.contains("skeleton") || path.contains("zombie") || path.contains("creeper") || path.contains("spider")) {
                score += 30;
            }
        }
        if (pref.plainsLike && !pref.aquatic && !pref.netherNative && !pref.ender) {
            float temp = biome.value().getBaseTemperature();
            if (temp > 0.3F && temp < 1.2F && !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER)) {
                score += 55;
            }
        }
        if (type != null && type.getCategory() == MobCategory.CREATURE && !pref.aquatic) {
            float temp = biome.value().getBaseTemperature();
            if (temp >= 0.5F && temp <= 1.0F) {
                score += 15;
            }
        }
        return score;
    }

    private static boolean isColdBiome(Biome biome, BlockPos pos) {
        try {
            Object raw = biome.getClass().getMethod("coldEnoughToSnow", BlockPos.class).invoke(biome, pos);
            if (raw instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return biome.getBaseTemperature() < 0.15F;
    }

    private record Preference(
        ResourceKey<net.minecraft.world.level.Level> dimension,
        boolean aquatic,
        boolean netherNative,
        boolean ender,
        boolean cold,
        boolean desert,
        boolean forest,
        boolean plainsLike,
        boolean preferCave
    ) {
        static Preference forMorph(ResourceLocation id, EntityType<?> type) {
            String path = id.getPath();
            String ns = id.getNamespace();
            MobCategory cat = type == null ? MobCategory.CREATURE : type.getCategory();

            boolean aquatic = cat == MobCategory.WATER_AMBIENT || cat == MobCategory.WATER_CREATURE
                || path.contains("fish") || path.contains("squid") || path.contains("dolphin")
                || path.contains("guardian") || path.contains("axolotl") || path.contains("turtle")
                || path.contains("leviathan") || path.contains("deepling") || path.contains("whale")
                || path.contains("shark") || path.contains("seal") || path.contains("crocodile")
                || path.contains("alligator") || path.contains("hippo") || path.contains("otter");

            boolean nether = path.contains("blaze") || path.contains("ghast") || path.contains("magma")
                || path.contains("strider") || path.contains("piglin") || path.contains("hoglin")
                || path.contains("wither_skeleton") || path.contains("zoglin")
                || path.contains("ignis") || path.contains("monstrosity") || path.contains("harbinger")
                || (ns.equals("cataclysm") && (path.contains("nether") || path.contains("ignited")));

            boolean ender = path.contains("enderman") || path.contains("endermite") || path.contains("shulker")
                || path.contains("ender_dragon") || path.contains("ender_golem") || path.contains("ender_guardian")
                || path.contains("endermaptera");

            boolean cold = path.contains("polar") || path.contains("stray") || path.contains("snow")
                || path.contains("goat");

            boolean desert = path.contains("husk") || path.contains("camel")
                || path.contains("remnant") || path.contains("kobole");

            boolean forest = path.contains("wolf") || path.contains("fox") || path.contains("bear")
                || path.contains("deer") || path.contains("parrot") || path.contains("ocelot")
                || path.contains("panda") || path.contains("bee");

            boolean cave = path.contains("skeleton") || path.contains("zombie") || path.contains("creeper")
                || path.contains("spider") || path.contains("silverfish") || path.contains("bat")
                || path.contains("warden") || path.contains("cave");

            if (path.contains("enderman")) {
                return new Preference(ServerLevel.NETHER, false, true, true, false, false, false, false, false);
            }
            if (nether) {
                return new Preference(ServerLevel.NETHER, false, true, false, false, false, false, false, false);
            }
            if (ender) {
                return new Preference(ServerLevel.END, false, false, true, false, false, false, false, false);
            }
            if (aquatic) {
                return new Preference(ServerLevel.OVERWORLD, true, false, false, false, false, false, false, false);
            }
            return new Preference(
                ServerLevel.OVERWORLD,
                false, false, false, cold, desert, forest,
                !cold && !desert && !forest && !cave,
                cave
            );
        }

        static Preference overworldFallback(ResourceLocation id, EntityType<?> type) {
            Preference p = forMorph(id, type);
            return new Preference(
                ServerLevel.OVERWORLD, p.aquatic, false, false, p.cold, p.desert, p.forest, p.plainsLike, p.preferCave
            );
        }
    }
}
