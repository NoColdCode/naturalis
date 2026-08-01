package dev.naturalis.content;

import dev.naturalis.Naturalis;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class NaturalisMobEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, Naturalis.MOD_ID);

    public static final RegistryObject<MobEffect> MORPH_BINDING =
        MOB_EFFECTS.register("morph_binding", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x7284A2) {
        });

    public static final RegistryObject<MobEffect> BREWED_MORPH =
        MOB_EFFECTS.register("brewed_morph", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x7B9A4A) {
        });

    public static final RegistryObject<MobEffect> STORM_ATTUNEMENT =
        MOB_EFFECTS.register("storm_attunement", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x63C7FF) {
        });

    private NaturalisMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
