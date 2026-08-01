package dev.naturalis.network;



import net.minecraft.network.FriendlyByteBuf;



/** Client → Server: refresh quick-slot assignments before the wheel opens. */

public final class MorphQuickSlotResyncPayload {



    public MorphQuickSlotResyncPayload() {

    }



    public static MorphQuickSlotResyncPayload decode(FriendlyByteBuf buf) {

        return new MorphQuickSlotResyncPayload();

    }



    public static void encode(MorphQuickSlotResyncPayload payload, FriendlyByteBuf buf) {

    }

}


