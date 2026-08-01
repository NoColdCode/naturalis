package dev.naturalis.content;

import dev.naturalis.NaturalisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisMobEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, NaturalisMod.ID);

    public static final DeferredHolder<MobEffect, MobEffect> MORPH_BINDING =
        MOB_EFFECTS.register("morph_binding", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x7284A2) {
        });

    public static final DeferredHolder<MobEffect, MobEffect> BREWED_MORPH =
        MOB_EFFECTS.register("brewed_morph", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x7B9A4A) {
        });

    public static final DeferredHolder<MobEffect, MobEffect> STORM_ATTUNEMENT =
        MOB_EFFECTS.register("storm_attunement", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x63C7FF) {
        });

    private NaturalisMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}