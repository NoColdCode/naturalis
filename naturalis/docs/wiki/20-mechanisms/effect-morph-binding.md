# Morph Binding (`naturalis:morph_binding`)

**Morph Binding** is a **neutral** Naturalis status effect that **locks your morph identity** for as long as it lasts. Like other Naturalis effects, it has **no default attribute map** in code; enforcement is done in events and mixins.

## Behavior summary

- While active, the mod **stores a binding target** morph id (from your current morph when binding starts, or from recovery data) and **forces** that morph every tick if needed.
- **Walkers** shape rules: unlocking a new shape can **fail** unless it matches the binding target; swapping shapes can **fail** unless you are allowed to take that shape (see [Storm Attunement](effect-storm-attunement.md) for the storm exception).
- **Walkers** **transform key** (client) is **disabled** while binding is active—pending key presses are swallowed so you cannot self-serve out of the lock via the usual transform bind.
- **ReMorphed** menu open is **blocked** while binding is active **unless** you also have **Storm Attunement** (Natural Dimension thunder).

## Sources and durations

| Source | Approximate duration | Notes |
|--------|----------------------|--------|
| **Morph Binding potion** (drink / splash / lingering) | **8 minutes** (`8 × 20 × 60` ticks) | From `MorphEffectEvents` / gameplay potion hits |
| **Resonance “binding surge”** | **8 seconds** (`8 × 20` ticks) | Random while bonded morph matches you, low humanity increases chance (`ResonanceEvents`) |
| **Natural Dimension thunder** | **6 seconds** per refresh (`120` ticks), refreshed often while storming | Applied together with Storm Attunement (`NaturalDimensionRuntime`) |
| **Witch interaction** (`WitchMorphBindingEvents`) | **Configurable seconds × 20** | Witch-applied binding gift |
| **Other gameplay** (e.g. lightning / potion clouds) | Short (e.g. **60** ticks in some paths) | See `MorphEffectEvents` for cloud and strike handling |

Exact numbers live in `MorphEffectEvents`, `NaturalisGameplayEvents`, `ResonanceEvents`, and `NaturalDimensionRuntime` if you need modpack parity.

## Removal and conflicts

- **Brewed Morph** wins: when **Brewed Morph** is applied, **Morph Binding** is **removed immediately** and binding NBT is cleared so the two systems do not fight.
- **Morph Binding** while you already have **Brewed Morph**: on tick, binding is stripped if both were somehow present (brewed morph takes priority).

## Related content

- [Brewed Morph](effect-brewed-morph.md) — temporary morph from potions  
- [Storm Attunement](effect-storm-attunement.md) — thunder exception for binding and human-clear  
- [Morph Knowledge](../10-basics/morph-knowledge.md) — knowledge reduces binding surge chance  
- [Witch mechanism](../30-lore-and-risks/witch-mechanism.md) — narrative side of binding from witches  
