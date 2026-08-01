package dev.naturalis.compat.walkers;

import dev.naturalis.NaturalisMod;
import dev.naturalis.instinct.InstinctManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import tocraft.walkers.integrations.Integrations;
import tocraft.walkers.traits.TraitRegistry;
import tocraft.walkers.traits.impl.AquaticTrait;
import tocraft.walkers.traits.impl.CantSwimTrait;
import tocraft.walkers.traits.impl.ClimbBlocksTrait;
import tocraft.walkers.traits.impl.FlyingTrait;
import tocraft.walkers.traits.impl.SlowFallingTrait;
import tocraft.walkers.traits.impl.StandOnFluidTrait;
import tocraft.walkers.traits.impl.UndrownableTrait;
import tocraft.walkers.traits.impl.WalkOnPowderSnow;

/**
 * Registers Naturalis ShapeTraits and supplements Walkers wiki traits for profiled / heuristic morphs.
 *
 * @see <a href="https://github.com/ToCraft/woodwalkers-mod/wiki/Traits">Walkers Traits wiki</a>
 */
@EventBusSubscriber(modid = NaturalisMod.ID, bus = EventBusSubscriber.Bus.MOD)
public final class NaturalisWalkersTraits {

    private static boolean integrationRegistered;

    private NaturalisWalkersTraits() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ensureIntegrationRegistered();
            register();
        });
    }

    /** Hooks Walkers so predicates are restored after every {@code TraitDataManager} reload. */
    public static void ensureIntegrationRegistered() {
        if (integrationRegistered) {
            return;
        }
        try {
            Integrations.register(NaturalisMod.ID, NaturalisWalkersIntegration::new);
            integrationRegistered = true;
        } catch (Throwable ignored) {
            // Walkers absent / older API — datapack JSON + one-shot register still apply.
        }
    }

    public static void register() {
        TraitRegistry.registerCodec(StaticShapeTrait.ID, StaticShapeTrait.CODEC);
        TraitRegistry.registerCodec(ScentboundShapeTrait.ID, ScentboundShapeTrait.CODEC);
        TraitRegistry.registerCodec(PhotophobicShapeTrait.ID, PhotophobicShapeTrait.CODEC);
        TraitRegistry.registerCodec(FloatingShapeTrait.ID, FloatingShapeTrait.CODEC);

        // Naturalis custom bubbles
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isStaticEntity, new StaticShapeTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isScentboundEntity, new ScentboundShapeTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isPhotophobicEntity, new PhotophobicShapeTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isFloatingEntity, new FloatingShapeTrait<>());

        // Walkers wiki traits — filled for vanilla + integration morphs via heuristics / profiles
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isFlightProfileEntity, new FlyingTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isAquaticDiverEntity, new AquaticTrait<>(true, false));
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isClimbEntity, new ClimbBlocksTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isCantSwimEntity, new CantSwimTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isUndrownableEntity, new UndrownableTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isSlowFallEntity, new SlowFallingTrait<>());
        TraitRegistry.registerByPredicate(NaturalisWalkersTraits::isPowderSnowEntity, new WalkOnPowderSnow<>());
        TraitRegistry.registerByPredicate(
            NaturalisWalkersTraits::isFloatingEntity,
            new StandOnFluidTrait<>(FluidTags.WATER)
        );
        TraitRegistry.registerByPredicate(
            NaturalisWalkersTraits::isLavaWalkEntity,
            new StandOnFluidTrait<>(FluidTags.LAVA)
        );
    }

    private static ResourceLocation typeId(LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    private static boolean isStaticEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isStaticMorph(id);
    }

    private static boolean isScentboundEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isScentbound(id);
    }

    private static boolean isPhotophobicEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isPhotophobic(id);
    }

    private static boolean isFloatingEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isFloatingMorph(id);
    }

    private static boolean isFlightProfileEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isFlightOnly(id);
    }

    private static boolean isAquaticDiverEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isAquaticDiver(id);
    }

    private static boolean isClimbEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isClimbMorph(id);
    }

    private static boolean isCantSwimEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isCantSwimMorph(id);
    }

    private static boolean isUndrownableEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isUndrownableMorph(id);
    }

    private static boolean isSlowFallEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isSlowFallingMorph(id);
    }

    private static boolean isPowderSnowEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isPowderSnowWalker(id);
    }

    private static boolean isLavaWalkEntity(LivingEntity entity) {
        ResourceLocation id = typeId(entity);
        return id != null && InstinctManager.isLavaWalker(id);
    }
}
