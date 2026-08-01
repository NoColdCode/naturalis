package dev.naturalis.content;

import dev.naturalis.Naturalis;
import dev.naturalis.entity.EchoSovereignEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisEntityTypes {

    private NaturalisEntityTypes() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Naturalis.MOD_ID);

    public static final RegistryObject<EntityType<EchoSovereignEntity>> ECHO_SOVEREIGN =
        ENTITY_TYPES.register("echo_sovereign",
            () -> EntityType.Builder.of(EchoSovereignEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .build(new ResourceLocation(Naturalis.MOD_ID, "echo_sovereign").toString()));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(NaturalisEntityTypes::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ECHO_SOVEREIGN.get(), Evoker.createAttributes().build());
    }
}
