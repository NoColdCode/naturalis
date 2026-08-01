package dev.naturalis.network;



import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;



public final class MorphQuickSlotAssignPayload {



    private final int slotIndex;

    private final String morphId;



    public MorphQuickSlotAssignPayload(int slotIndex, ResourceLocation morphId) {

        this(slotIndex, morphId.toString());

    }



    private MorphQuickSlotAssignPayload(int slotIndex, String morphId) {

        this.slotIndex = slotIndex;

        this.morphId = morphId;

    }



    public static MorphQuickSlotAssignPayload decode(FriendlyByteBuf buf) {

        return new MorphQuickSlotAssignPayload(buf.readVarInt(), buf.readUtf());

    }



    public static void encode(MorphQuickSlotAssignPayload payload, FriendlyByteBuf buf) {

        buf.writeVarInt(payload.slotIndex);

        buf.writeUtf(payload.morphId);

    }



    public int slotIndex() {

        return slotIndex;

    }



    public ResourceLocation morphId() {

        return ResourceLocation.isValidResourceLocation(morphId) ? new ResourceLocation(morphId) : null;

    }

}


