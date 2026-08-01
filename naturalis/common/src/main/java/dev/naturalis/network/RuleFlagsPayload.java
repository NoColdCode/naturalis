package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RuleFlagsPayload(boolean colorFilterEnabled, boolean inventoryRestrictionEnabled, boolean instinctsEnabled)
    implements CustomPacketPayload {

    public static final Type<RuleFlagsPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "rule_flags"));

    public static final StreamCodec<ByteBuf, RuleFlagsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, RuleFlagsPayload::colorFilterEnabled,
        ByteBufCodecs.BOOL, RuleFlagsPayload::inventoryRestrictionEnabled,
        ByteBufCodecs.BOOL, RuleFlagsPayload::instinctsEnabled,
        RuleFlagsPayload::new
    );

    @Override
    public Type<RuleFlagsPayload> type() {
        return TYPE;
    }
}
