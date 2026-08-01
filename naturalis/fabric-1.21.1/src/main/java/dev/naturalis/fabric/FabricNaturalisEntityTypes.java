package dev.naturalis.fabric;

import dev.naturalis.NaturalisMod;
import dev.naturalis.entity.EchoSovereignEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;

public final class FabricNaturalisEntityTypes {

    public static final EntityType<EchoSovereignEntity> ECHO_SOVEREIGN = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "echo_sovereign"),
        EntityType.Builder.of(EchoSovereignEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .build(ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "echo_sovereign").toString())
    );

    private FabricNaturalisEntityTypes() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(ECHO_SOVEREIGN, Evoker.createAttributes().build());
    }
}
