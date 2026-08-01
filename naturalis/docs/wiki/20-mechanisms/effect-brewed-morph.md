# Brewed Morph (`naturalis:brewed_morph`)

**Brewed Morph** is a **beneficial** Naturalis status effect that represents a **potion-driven temporary morph**. Duration and target species are **not** encoded in the effect amplifier; they come from **item custom data** and player NBT under the mod’s effect root tag.

## Behavior summary

- On apply, the server stores the target mob id (e.g. `minecraft:wolf`) in the player’s Naturalis effect tag as **`brewed_morph_id`** and applies the **Brewed Morph** effect for the potion’s duration.
- Each tick while the effect is active, the server **enforces** that morph (same family of logic as binding enforcement).
- **Morph Binding is cleared** when a brewed morph is applied: binding effect removal + binding fields wiped so **brew always overrides bind**.

## Duration and items

- **Default brewed duration** (when not overridden on the stack): **60 seconds** (`20 × 60` ticks).  
- **Minimum** valid duration when applying from code: **1 second** (`20` ticks); shorter requests are rejected.

Applies from:

- Drinking a **Potion of Brewed Morph** (must have a valid morph target set on the stack).
- **Splash** / **lingering** variants hitting entities (players get full morph logic; other livings may get effect-only behavior—see `MorphEffectEvents.applyBrewedMorphPotionToLiving`).

Tooltip and messaging use `item.naturalis.brewed_morph_potion.*` language keys when the potion has no target.

## Brewing and progression

Morph binding and brewed morph potions participate in Naturalis **brewing** recipes (`BrewedMorphBrewingEvents`). Advancements such as `naturalis:resonance/potion_brewed` track first crafts.

## Related content

- [Morph Binding](effect-morph-binding.md) — cleared when brewed morph wins  
- [Morph Knowledge](../10-basics/morph-knowledge.md)  
- Legacy overview: [Morphing and Effects](../02-morphing-and-effects.md)  
