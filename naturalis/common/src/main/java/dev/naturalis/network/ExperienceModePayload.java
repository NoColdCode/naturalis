package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import dev.naturalis.experience.NaturalisExperienceMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExperienceModePayload(byte modeId, boolean showPrompt)
    implements CustomPacketPayload {

    public static final Type<ExperienceModePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "experience_mode"));

    public static final StreamCodec<ByteBuf, ExperienceModePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE, ExperienceModePayload::modeId,
        ByteBufCodecs.BOOL, ExperienceModePayload::showPrompt,
        ExperienceModePayload::new
    );

    public ExperienceModePayload(NaturalisExperienceMode mode, boolean showPrompt) {
        this(mode.id(), showPrompt);
    }

    public NaturalisExperienceMode mode() {
        return NaturalisExperienceMode.fromId(modeId);
    }

    @Override
    public Type<ExperienceModePayload> type() {
        return TYPE;
    }
}
