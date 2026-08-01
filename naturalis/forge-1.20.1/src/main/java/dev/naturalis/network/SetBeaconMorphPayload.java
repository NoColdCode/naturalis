package dev.naturalis.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Client → Server: sets morph id and target mode on a morph-beacon block entity. */
public final class SetBeaconMorphPayload {

    private final BlockPos pos;
    private final String morphId;
    private final int targetMode;

    public SetBeaconMorphPayload(BlockPos pos, String morphId, int targetMode) {
        this.pos = pos;
        this.morphId = morphId;
        this.targetMode = targetMode;
    }

    public static SetBeaconMorphPayload decode(FriendlyByteBuf buf) {
        return new SetBeaconMorphPayload(buf.readBlockPos(), buf.readUtf(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(morphId);
        buf.writeVarInt(targetMode);
    }

    public BlockPos pos() {
        return pos;
    }

    public String morphId() {
        return morphId;
    }

    public int targetMode() {
        return targetMode;
    }
}
