package dev.naturalis.network;



import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;



public final class MorphQuickSlotSelectPayload {



    private final int slotIndex;

    @Nullable

    private final ResourceLocation morphId;



    public MorphQuickSlotSelectPayload(int slotIndex) {

        this(slotIndex, null);

    }



    public MorphQuickSlotSelectPayload(int slotIndex, @Nullable ResourceLocation morphId) {

        this.slotIndex = slotIndex;

        this.morphId = morphId;

    }



    public static MorphQuickSlotSelectPayload decode(FriendlyByteBuf buf) {

        int slotIndex = buf.readVarInt();

        String raw = buf.readUtf();

        ResourceLocation morphId = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);

        return new MorphQuickSlotSelectPayload(slotIndex, morphId);

    }



    public static void encode(MorphQuickSlotSelectPayload payload, FriendlyByteBuf buf) {

        buf.writeVarInt(payload.slotIndex);

        buf.writeUtf(payload.morphId == null ? "" : payload.morphId.toString());

    }



    public int slotIndex() {

        return slotIndex;

    }



    @Nullable

    public ResourceLocation morphId() {

        return morphId;

    }

}


