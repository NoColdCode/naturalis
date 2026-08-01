# Patch Notes 1.6.0

This page summarizes the gameplay and presentation changes for version **1.6.0**.

## Headline features

### Experience modes

On first join (or when unset), choose how “physical” morphs feel:

| Mode | What you get |
| --- | --- |
| **Realistic** | Full embodiment: neck limits, gait camera sway, dig shake, first-person morph body, vibration tremor, scratch feedback |
| **Softened** | Vision filters, scent trails & ribbons, cinematic dig-view blend, inventory, diet, and Knowledge — without the strongest nausea-prone head/gait effects |

Set per world with `/naturalis experience realistic` or `/naturalis experience softened`.

### Morph head & camera movement

First-person movement now follows morph archetype:

- **Canine / feline** — sniff prowl bob and sway
- **Equine** — trot rhythm
- **Spider** — skitter motion
- **Avian** — peck and alert idle
- **Aquatic** — swim sway (stronger in water)
- **Generic quadruped** — baseline bob

Realistic mode also applies neck limits, dig-view pull toward blocks you break, and related embodiment cues. See [Smell Sense](../20-mechanisms/smell-sense.md) and controls below for sniff input.

### Scent vision & ribbons

Passive **Smell Sense** still scans on an interval for eligible morphs (see wiki values).

**Active sniff** (Shift + use / secondary on wolf, fox, and other scent-capable forms):

- Brief **scent vision** shader overlay
- Head-down sniff animation pulses
- **Scent ribbons** — one continuous trail per tracked target (prey, hostile, or deep-sniff anchor), rendered as meandering paths in the world

Client option: `scent_trails` under Naturalis client HUD config.

### Morph quick-slot wheel

| Input | Action |
| --- | --- |
| **Hold G** | Open radial quick-slot wheel |
| **Move mouse / number keys** | Highlight slot |
| **Release G** | Morph into highlighted slot (if assigned & unlocked) |
| **Tap G** | Toggle human ↔ current morph (ReMorphed transform) |

- Slots are organized by category (ground, aerial, aquatic, nether, hostile, high damage).
- Assign morphs from the wheel UI or dedicated assign screen.
- Works alongside Morph Binding rules; a short server session pauses binding enforcement while the wheel is open.

### Species secondary actions (WIP)

Right-click while morphed (main hand, empty or non-blocking item):

| Action | Examples |
| --- | --- |
| **Sniff** | Wolf, fox, bear, pig, high smell-strength morphs |
| **Listen** | Bat, rabbit, enhanced-hearing morphs |
| **Peck** | Chicken, parrot, avian-style morphs |

Shift + right-click remains **active sniff** where smell sense applies. More species-specific secondaries are planned.

## Config reminders

- Common: `scent_hints` — server sends scent trail hints
- Client: `scent_trails` — render ribbon particles/paths

## Fixes (technical)

- Forge 1.20.1 jar task no longer fails on duplicate mixin refmap.
- Quick-slot network encoding and transform-key / Morph Binding interaction fixes on Forge.

## What's next

Secondary mechanics and ribbon polish are still **WIP**. Expect more morph-specific actions and tuning in 1.6.x patches.
