package dev.naturalis.compat.walkers;

import tocraft.walkers.integrations.AbstractIntegration;

/**
 * Walkers re-invokes {@link #registerTraits()} from {@code TraitRegistry.registerDefault()} after
 * every trait datapack reload. Registering here keeps Naturalis predicates (and codecs) durable.
 */
public final class NaturalisWalkersIntegration extends AbstractIntegration {

    @Override
    public void registerTraits() {
        NaturalisWalkersTraits.register();
    }
}
