package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.world.menu.MorphBeaconMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MorphBeaconBlockEntity extends BlockEntity implements MenuProvider {

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

    private final ContainerData data = new ContainerData() {
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

        @Override
        public int getCount() {
            return 3;
        }
    };

    public MorphBeaconBlockEntity(BlockPos pos, BlockState blockState) {
        super(CompatAccess.naturalisBlockEntityType("morph_beacon"), pos, blockState);
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
        targetMorphId = MorphBeaconMorphIds.normalizeString(id);
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

    public static void tickServer(Level level, BlockPos pos, BlockState state, MorphBeaconBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return;

        int prevLevel = be.pyramidLevel;
        be.pyramidLevel = computePyramidLevel(level, pos);
        if (be.pyramidLevel != prevLevel) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        // Platform-specific morph application is done in the neoforge-1.21.8 override.
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
            if (y < level.getMinBuildHeight()) break;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PyramidLevel", pyramidLevel);
        tag.putInt("TargetMode", targetMode);
        tag.putString("TargetMorphId", targetMorphId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pyramidLevel = tag.getInt("PyramidLevel");
        targetMode = tag.getInt("TargetMode");
        targetMorphId = tag.contains("TargetMorphId") ? tag.getString("TargetMorphId") : "";
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PyramidLevel", pyramidLevel);
        tag.putInt("TargetMode", targetMode);
        tag.putString("TargetMorphId", targetMorphId);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        if (tag.contains("PyramidLevel")) {
            pyramidLevel = tag.getInt("PyramidLevel");
        }
        if (tag.contains("TargetMode")) {
            targetMode = tag.getInt("TargetMode");
        }
        if (tag.contains("TargetMorphId")) {
            targetMorphId = tag.getString("TargetMorphId");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.morph_beacon");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MorphBeaconMenu(containerId, inventory, this);
    }
}
