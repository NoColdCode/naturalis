package dev.naturalis.mixin;

import dev.naturalis.compat.NaturalisPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void naturalis$saveFabricPersist(ValueOutput output, CallbackInfo ci) {
        if (naturalis$persistentData != null && !naturalis$persistentData.isEmpty()) {
            output.store(NATURALIS_PERSIST_KEY, CompoundTag.CODEC, naturalis$persistentData);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    protected void naturalis$loadFabricPersist(ValueInput input, CallbackInfo ci) {
        input.read(NATURALIS_PERSIST_KEY, CompoundTag.CODEC).ifPresent(tag -> naturalis$persistentData = tag);
    }
}
