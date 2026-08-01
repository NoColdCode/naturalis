package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorphLevelPayload(
    int level,
    int hotbarSlots,
    boolean inventoryUnlocked,
    int utilitiesRank
) implements CustomPacketPayload {

    public static final Type<MorphLevelPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_level"));

    public static final StreamCodec<ByteBuf, MorphLevelPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MorphLevelPayload::level,
        ByteBufCodecs.VAR_INT, MorphLevelPayload::hotbarSlots,
        ByteBufCodecs.BOOL, MorphLevelPayload::inventoryUnlocked,
        ByteBufCodecs.VAR_INT, MorphLevelPayload::utilitiesRank,
        MorphLevelPayload::new
    );

    public MorphLevelPayload(int level, int hotbarSlots, boolean inventoryUnlocked) {
        this(level, hotbarSlots, inventoryUnlocked, 0);
    }

    @Override
    public Type<MorphLevelPayload> type() {
        return TYPE;
    }
}
