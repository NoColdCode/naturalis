package dev.naturalis.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

/** Server → Client: scent detection hint for the trail renderer. */
public final class ScentHintPayload {

    public static final byte CATEGORY_UNKNOWN = 0;
    public static final byte CATEGORY_PREY = 1;
    public static final byte CATEGORY_HOSTILE = 2;
    public static final byte CATEGORY_PLAYER = 3;
    public static final byte CATEGORY_PASSIVE = 4;
    public static final byte CATEGORY_NATURE = 5;

    private final int entityId;
    private final byte category;
    private final int strength;
    private final Optional<BlockPos> anchor;

    public ScentHintPayload(int entityId, byte category, int strength) {
        this(entityId, category, strength, Optional.empty());
    }

    public ScentHintPayload(int entityId, byte category, int strength, Optional<BlockPos> anchor) {
        this.entityId = entityId;
        this.category = category;
        this.strength = strength;
        this.anchor = anchor == null ? Optional.empty() : anchor;
    }

    public static ScentHintPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        byte category = buf.readByte();
        int strength = buf.readVarInt();
        long anchorLong = buf.readVarLong();
        Optional<BlockPos> anchor = anchorLong == Long.MIN_VALUE
            ? Optional.empty()
            : Optional.of(BlockPos.of(anchorLong));
        return new ScentHintPayload(entityId, category, strength, anchor);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(category);
        buf.writeVarInt(strength);
        buf.writeVarLong(anchor.map(BlockPos::asLong).orElse(Long.MIN_VALUE));
    }

    public int entityId() { return entityId; }
    public byte category() { return category; }
    public int strength() { return strength; }
    public Optional<BlockPos> anchor() { return anchor; }
}
