package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.entity.EchoSovereignEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisEntityTypes {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Naturalis.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<EchoSovereignEntity>> ECHO_SOVEREIGN =
        ENTITY_TYPES.register("echo_sovereign", () -> EntityType.Builder
            .of(EchoSovereignEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .build(ResourceLocation.fromNamespaceAndPath(Naturalis.MOD_ID, "echo_sovereign").toString()));

    private NaturalisEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(NaturalisEntityTypes::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ECHO_SOVEREIGN.get(), Evoker.createAttributes().build());
    }
}
