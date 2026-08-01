package dev.naturalis.content;

import dev.naturalis.NaturalisMod;
import dev.naturalis.entity.EchoSovereignEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisEntityTypes {

    private NaturalisEntityTypes() {
    }

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, NaturalisMod.ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EchoSovereignEntity>> ECHO_SOVEREIGN =
        ENTITY_TYPES.register("echo_sovereign",
            () -> EntityType.Builder.of(EchoSovereignEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "echo_sovereign"))));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(NaturalisEntityTypes::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ECHO_SOVEREIGN.get(), Evoker.createAttributes().build());
    }
}
