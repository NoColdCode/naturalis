package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Opens the Survival-as permanent traits popup on the client. */
public record SurvivalAsTraitsPayload(
    String morphId,
    double mass,
    String dietId,
    List<String> traitIds,
    /** Parallel to traitIds: creature lists / extras (empty string when none). */
    List<String> traitExtras
) implements CustomPacketPayload {

    public static final Type<SurvivalAsTraitsPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "survival_as_traits"));

    public static final StreamCodec<ByteBuf, SurvivalAsTraitsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SurvivalAsTraitsPayload::morphId,
        ByteBufCodecs.DOUBLE, SurvivalAsTraitsPayload::mass,
        ByteBufCodecs.STRING_UTF8, SurvivalAsTraitsPayload::dietId,
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), SurvivalAsTraitsPayload::traitIds,
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), SurvivalAsTraitsPayload::traitExtras,
        SurvivalAsTraitsPayload::new
    );

    @Override
    public Type<SurvivalAsTraitsPayload> type() {
        return TYPE;
    }
}
