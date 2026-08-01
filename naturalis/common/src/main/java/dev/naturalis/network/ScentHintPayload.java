package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record ScentHintPayload(int entityId, byte category, int strength, Optional<BlockPos> anchor)
    implements CustomPacketPayload {

    /** Legacy / fallback — treated as passive on the client. */
    public static final byte CATEGORY_UNKNOWN = 0;
    public static final byte CATEGORY_PREY = 1;
    public static final byte CATEGORY_HOSTILE = 2;
    public static final byte CATEGORY_PLAYER = 3;
    public static final byte CATEGORY_PASSIVE = 4;
    public static final byte CATEGORY_NATURE = 5;

    public static final Type<ScentHintPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "scent_hint"));

    public static final StreamCodec<ByteBuf, ScentHintPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ScentHintPayload::entityId,
        ByteBufCodecs.BYTE, ScentHintPayload::category,
        ByteBufCodecs.VAR_INT, ScentHintPayload::strength,
        ByteBufCodecs.VAR_LONG, ScentHintPayload::anchorLong,
        ScentHintPayload::fromParts
    );

    public ScentHintPayload(int entityId, byte category, int strength) {
        this(entityId, category, strength, Optional.empty());
    }

    public static ScentHintPayload nature(BlockPos pos, int strength) {
        return new ScentHintPayload(blockScentKey(pos), CATEGORY_NATURE, strength, Optional.of(pos.immutable()));
    }

    public static int blockScentKey(BlockPos pos) {
        return -Math.floorMod(pos.hashCode(), 1_073_741_824) - 1;
    }

    private static long anchorLong(ScentHintPayload payload) {
        return payload.anchor().map(BlockPos::asLong).orElse(0L);
    }

    private static ScentHintPayload fromParts(int entityId, byte category, int strength, long anchorLong) {
        Optional<BlockPos> anchor = anchorLong == 0L ? Optional.empty() : Optional.of(BlockPos.of(anchorLong));
        return new ScentHintPayload(entityId, category, strength, anchor);
    }

    @Override
    public Type<ScentHintPayload> type() {
        return TYPE;
    }
}
