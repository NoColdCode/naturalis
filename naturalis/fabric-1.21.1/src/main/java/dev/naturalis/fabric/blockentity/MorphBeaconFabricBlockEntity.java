package dev.naturalis.fabric.blockentity;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.fabric.FabricNaturalisBlockEntities;
import dev.naturalis.fabric.menu.MorphBeaconFabricMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MorphBeaconFabricBlockEntity extends BlockEntity implements MenuProvider {

    public enum TargetMode {
        ENEMY_MOBS,
        PLAYERS,
        PASSIVE_MOBS,
        ALL_MOBS,
        ALL_LIVING;

        public static TargetMode fromIndex(int index) {
            TargetMode[] values = values();
            if (index < 0 || index >= values.length) return PLAYERS;
            return values[index];
        }
    }

    private int pyramidLevel;
    private int targetMode;
    private String targetMorphId = "";

    /** UUIDs of players currently beacon-morphed, used for auto-unmorph on range exit. */
    private final Set<UUID> morphedPlayers = new HashSet<>();

    private final ContainerData data = new SimpleContainerData(3) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> pyramidLevel;
                case 1 -> getRange();
                case 2 -> targetMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 2) {
                targetMode = Math.max(0, Math.min(TargetMode.values().length - 1, value));
            }
        }
    };

    public MorphBeaconFabricBlockEntity(BlockPos pos, BlockState blockState) {
        super(FabricNaturalisBlockEntities.MORPH_BEACON, pos, blockState);
    }

    public ContainerData getData() {
        return data;
    }

    public void setTargetMode(int index) {
        data.set(2, index);
        setChanged();
    }

    public String getTargetMorphId() {
        return targetMorphId;
    }

    public void setTargetMorphId(String id) {
        targetMorphId = id == null ? "" : id.trim();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getPyramidLevel() {
        return pyramidLevel;
    }

    public int getRange() {
        return pyramidLevel <= 0 ? 0 : 16 + pyramidLevel * 16;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, MorphBeaconFabricBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return;

        int prevLevel = be.pyramidLevel;
        be.pyramidLevel = computePyramidLevel(level, pos);
        if (be.pyramidLevel != prevLevel) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }

        if (be.pyramidLevel <= 0 || be.targetMorphId.isEmpty()) {
            be.unMorphAllPlayers(level);
            return;
        }

        int range = be.getRange();
        TargetMode mode = TargetMode.fromIndex(be.targetMode);
        AABB area = new AABB(pos).inflate(range);

        Set<UUID> inRangePlayers = new HashSet<>();
        for (Entity entity : level.getEntities(null, area)) {
            if (!(entity instanceof ServerPlayer player)) continue;
            if (!matchesMode(entity, mode)) continue;
            inRangePlayers.add(player.getUUID());
            // TODO: Apply beacon morph effect (will need MorphEffectEvents implementation)
            // For now, just track the player
            be.morphedPlayers.add(player.getUUID());
        }

        // Instantly unmorph players who left the range.
        Iterator<UUID> iter = be.morphedPlayers.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            if (!inRangePlayers.contains(uuid)) {
                // TODO: Clear beacon morph when leaving range
                iter.remove();
            }
        }
    }

    private void unMorphAllPlayers(Level level) {
        if (morphedPlayers.isEmpty()) return;
        for (UUID uuid : morphedPlayers) {
            // TODO: Clear beacon morph when disabling
        }
        morphedPlayers.clear();
    }

    private static boolean matchesMode(Entity entity, TargetMode mode) {
        return switch (mode) {
            case ENEMY_MOBS -> entity instanceof Enemy && !(entity instanceof Player);
            case PLAYERS -> entity instanceof Player;
            case PASSIVE_MOBS -> entity instanceof Mob mob && !(mob instanceof Enemy);
            case ALL_MOBS -> entity instanceof Mob;
            case ALL_LIVING -> entity instanceof LivingEntity;
        };
    }

    private static int computePyramidLevel(Level level, BlockPos beaconPos) {
        int validLevels = 0;
        for (int layer = 1; layer <= 4; layer++) {
            int y = beaconPos.getY() - layer;
            if (y < CompatAccess.getMinBuildHeight(level)) break;
            boolean layerValid = true;
            for (int x = beaconPos.getX() - layer; x <= beaconPos.getX() + layer && layerValid; x++) {
                for (int z = beaconPos.getZ() - layer; z <= beaconPos.getZ() + layer; z++) {
                    if (!isBeaconBaseBlock(level.getBlockState(new BlockPos(x, y, z)))) {
                        layerValid = false;
                        break;
                    }
                }
            }
            if (layerValid) validLevels = layer;
            else break;
        }
        return validLevels;
    }

    private static boolean isBeaconBaseBlock(BlockState state) {
        Block echoBlock = CompatAccess.naturalisBlock("echo_block");
        return state.is(Blocks.IRON_BLOCK)
            || state.is(Blocks.GOLD_BLOCK)
            || state.is(Blocks.DIAMOND_BLOCK)
            || state.is(Blocks.EMERALD_BLOCK)
            || state.is(Blocks.NETHERITE_BLOCK)
            || (echoBlock != null && echoBlock != Blocks.AIR && state.is(echoBlock));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.morph_beacon");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MorphBeaconFabricMenu(containerId, inventory, this);
    }
}
