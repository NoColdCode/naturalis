package dev.naturalis.compat;

import net.minecraft.nbt.CompoundTag;

/**
 * Fabric-backed persistent entity tag storage (NeoForge/Forge expose {@code Entity#getPersistentData()} natively).
 */
public interface NaturalisPersistentDataHolder {

    CompoundTag naturalis$getPersistentData();
}
