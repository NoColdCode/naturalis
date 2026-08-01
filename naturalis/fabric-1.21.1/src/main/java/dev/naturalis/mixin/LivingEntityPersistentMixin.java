package dev.naturalis.mixin;

import dev.naturalis.compat.NaturalisPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPersistentMixin implements NaturalisPersistentDataHolder {

    private static final String NATURALIS_PERSIST_KEY = "NaturalisPersist";

    @Unique
    private CompoundTag naturalis$persistentData;

    @Override
    public CompoundTag naturalis$getPersistentData() {
        if (naturalis$persistentData == null) {
            naturalis$persistentData = new CompoundTag();
        }
        return naturalis$persistentData;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    protected void naturalis$saveFabricPersist(CompoundTag compoundTag, CallbackInfo ci) {
        if (naturalis$persistentData != null && !naturalis$persistentData.isEmpty()) {
            compoundTag.put(NATURALIS_PERSIST_KEY, naturalis$persistentData);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    protected void naturalis$loadFabricPersist(CompoundTag compoundTag, CallbackInfo ci) {
        if (compoundTag.contains(NATURALIS_PERSIST_KEY)) {
            naturalis$persistentData = compoundTag.getCompound(NATURALIS_PERSIST_KEY).copy();
        }
    }
}
