package dev.naturalis.world;

import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisBlocks;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.world.menu.MorphBeaconMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

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

    /** UUIDs of players currently beacon-morphed, used for auto-unmorph on range exit. */
    private final Set<UUID> morphedPlayers = new HashSet<>();
    /** UUIDs of mobs transformed by this beacon (reverted when they leave range). */
    private final Set<UUID> morphedMobs = new HashSet<>();

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
        super(NaturalisBlockEntities.MORPH_BEACON.get(), pos, blockState);
    }

    public ContainerData getData() {
        return data;
    }

    public void setTargetMode(int index) {
        data.set(2, index);
        setChanged();
        syncClientState();
    }

    public String getTargetMorphId() {
        return targetMorphId;
    }

    public void setTargetMorphId(String id) {
        targetMorphId = MorphBeaconMorphIds.normalizeString(id);
        setChanged();
        syncClientState();
    }

    private void syncClientState() {
        if (level != null && !level.isClientSide()) {
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

        if (be.pyramidLevel <= 0 || be.targetMorphId.isEmpty()) {
            be.unMorphAll(level);
            return;
        }

        ResourceLocation morphId = MorphBeaconMorphIds.normalize(be.targetMorphId);
        if (morphId == null || !MorphMobTransformUtil.isValidLivingMorph(morphId)) {
            be.unMorphAll(level);
            return;
        }

        int range = be.getRange();
        TargetMode mode = TargetMode.fromIndex(be.targetMode);
        AABB area = new AABB(pos).inflate(range);

        Set<UUID> inRangePlayers = new HashSet<>();
        Set<UUID> inRangeMobs = new HashSet<>();
        for (Entity entity : level.getEntities(null, area)) {
            if (!matchesMode(entity, mode)) {
                continue;
            }

            if (entity instanceof ServerPlayer player) {
                inRangePlayers.add(player.getUUID());
                MorphEffectEvents.applyBossRingMorphEffect(player, morphId, 80);
                be.morphedPlayers.add(player.getUUID());
                continue;
            }

            if (!(entity instanceof Mob mob) || mode == TargetMode.PLAYERS) {
                continue;
            }

            ResourceLocation currentType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (morphId.equals(currentType)) {
                if (MorphMobTransformUtil.isBeaconTagged(mob, pos)) {
                    inRangeMobs.add(mob.getUUID());
                }
                continue;
            }

            LivingEntity transformed = MorphMobTransformUtil.transformForBeacon(mob, morphId, pos);
            if (transformed != null) {
                inRangeMobs.add(transformed.getUUID());
                be.morphedMobs.add(transformed.getUUID());
            }
        }

        Iterator<UUID> mobIter = be.morphedMobs.iterator();
        while (mobIter.hasNext()) {
            UUID uuid = mobIter.next();
            if (inRangeMobs.contains(uuid)) {
                continue;
            }
            if (level instanceof ServerLevel serverLevel) {
                Entity e = serverLevel.getEntity(uuid);
                if (e instanceof LivingEntity living) {
                    LivingEntity restored = MorphMobTransformUtil.revertBeaconTransform(living, pos);
                    if (restored != null) {
                        mobIter.remove();
                    }
                } else {
                    mobIter.remove();
                }
            }
        }

        // Instantly unmorph players who left the range.
        Iterator<UUID> iter = be.morphedPlayers.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            if (!inRangePlayers.contains(uuid)) {
                if (level instanceof ServerLevel serverLevel) {
                    Entity e = serverLevel.getEntity(uuid);
                    if (e instanceof ServerPlayer sp) {
                        MorphEffectEvents.clearBeaconMorph(sp);
                    }
                }
                iter.remove();
            }
        }
    }

    private void unMorphAll(Level level) {
        unMorphAllPlayers(level);
        revertAllMobs(level);
    }

    private void revertAllMobs(Level level) {
        if (morphedMobs.isEmpty()) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            morphedMobs.clear();
            return;
        }
        for (UUID uuid : morphedMobs) {
            Entity e = serverLevel.getEntity(uuid);
            if (e instanceof LivingEntity living) {
                MorphMobTransformUtil.revertBeaconTransform(living, worldPosition);
            }
        }
        morphedMobs.clear();
    }

    private void unMorphAllPlayers(Level level) {
        if (morphedPlayers.isEmpty()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (UUID uuid : morphedPlayers) {
            Entity e = serverLevel.getEntity(uuid);
            if (e instanceof ServerPlayer sp) {
                MorphEffectEvents.clearBeaconMorph(sp);
            }
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
            if (y < level.getMinY()) break;
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
        return state.is(Blocks.IRON_BLOCK)
            || state.is(Blocks.GOLD_BLOCK)
            || state.is(Blocks.DIAMOND_BLOCK)
            || state.is(Blocks.EMERALD_BLOCK)
            || state.is(Blocks.NETHERITE_BLOCK)
            || state.is(NaturalisBlocks.ECHO_BLOCK.get());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("PyramidLevel", pyramidLevel);
        output.putInt("TargetMode", targetMode);
        output.putString("TargetMorphId", targetMorphId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pyramidLevel = input.getIntOr("PyramidLevel", 0);
        targetMode = input.getIntOr("TargetMode", 0);
        targetMorphId = input.getStringOr("TargetMorphId", "");
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
    public void handleUpdateTag(ValueInput input) {
        pyramidLevel = input.getIntOr("PyramidLevel", pyramidLevel);
        targetMode = input.getIntOr("TargetMode", targetMode);
        targetMorphId = input.getStringOr("TargetMorphId", targetMorphId);
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
