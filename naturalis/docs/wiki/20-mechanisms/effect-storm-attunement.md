# Storm Attunement (`naturalis:storm_attunement`)

**Storm Attunement** is a **neutral** Naturalis status effect that marks the player as **aligned with the Natural Dimension’s storm state**. It has **no built-in attribute modifiers**; gameplay code uses it as a flag.

## Where it comes from

While you are in the **Natural Dimension** and the dimension is **thundering**, the dimension tick refreshes **Storm Attunement** (and **Morph Binding**) on all players there: each refresh applies **6 seconds** of duration (`120` ticks). Thunder is intentionally common there; normal rain is suppressed in favor of storm-heavy weather.

See also: [Natural Dimension Overview](../30-lore-and-risks/natural-dimension-overview.md).

## What it does in gameplay

### 1. Morph swap rules (Walkers `SWAP_SHAPE`)

When **Storm Attunement** is active:

- **You cannot clear your morph to human** — swapping your shape to **`null`** is rejected (`InteractionResult.FAIL`). You stay in a morph until the storm attunement wears off or you leave the storm context.
- **Morph Binding’s “single allowed morph” lock is ignored** for swaps. Normally, with **Morph Binding** and **without** storm attunement, you may only swap to the entity type recorded as your binding target. With storm attunement, that restriction is **skipped**, so you can swap among valid morphs more freely—**except** you still cannot drop to human while attuned.

Unlocking **new** shapes (`UNLOCK_SHAPE`) while binding is active still requires matching the binding target; storm attunement does not change that rule.

### 2. ReMorphed menu (client)

The mixin that blocks opening the **ReMorphed** radial/menu while **Morph Binding** or **0 humanity** applies **does not block** when **Storm Attunement** is active. So during Natural storms, that menu can remain usable even if you would normally be treated as “locked down.”

### 3. Walkers transform key

The **Walkers** transform key is still suppressed whenever **Morph Binding** is present (storm attunement does **not** change that mixin).

## Design intent (player-facing)

Storm Attunement ties **identity pressure** in the Natural Dimension to **weather**: thunderstorms are not just ambience—they **change the rules** for how tightly morph binding grips you, and they **forbid** ducking back to human form for the duration of that attunement. It stacks narratively with the dimension’s frequent thunder and passive **Morph Binding** refresh during storms.
