# Naturalis mob effects (overview)

Naturalis registers three custom status effects. They are **markers and rules engines** more than vanilla-style attribute buffs: most behavior is implemented in gameplay code that checks for these effects.

| Registry ID | Display name | Category | Dedicated page |
|-------------|----------------|----------|------------------|
| `naturalis:storm_attunement` | Storm Attunement | Neutral | [Storm Attunement](effect-storm-attunement.md) |
| `naturalis:morph_binding` | Morph Binding | Neutral | [Morph Binding](effect-morph-binding.md) |
| `naturalis:brewed_morph` | Brewed Morph | Beneficial | [Brewed Morph](effect-brewed-morph.md) |

## Quick summary

- **Storm Attunement** — Applied during **thunderstorms in the Natural Dimension**. Marks you as storm-tuned: it **relaxes** some morph-binding UI locks, **bypasses** the usual “locked to one morph” swap rule, but **blocks** clearing your morph to human (`null` shape) while the storm attunement is active.
- **Morph Binding** — Locks you to a **stored morph identity** (Walkers/ReMorphed shape changes and the transform key are restricted). Sources include **potions**, **resonance surges**, **Natural Dimension thunder**, and some **entity interactions**.
- **Brewed Morph** — Carries a **temporary morph** from **potion custom data**; **overrides** morph binding when applied (binding is cleared first).

For morph progression and resonance context, see [Morph Knowledge](../10-basics/morph-knowledge.md) and [Resonance Quick Explanation](../10-basics/resonance-quick-explanation.md).
