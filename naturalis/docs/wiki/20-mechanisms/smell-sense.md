# Smell Sense

Smell Sense is a passive biological sense for specific morphs.

It is not blocked by instinct mastery.

## Scan behavior

- Scan interval: every `8 ticks` (`0.4 s`)
- Target cap per scan: `16`
- Range formula: `8 + (smellStrength x 6)` blocks

Range by smell strength:
- Strength 1: `14 blocks`
- Strength 2: `20 blocks`
- Strength 3: `26 blocks`

## Common smell strengths

Strength 3 examples:
- Wolf
- Fox
- Warden

Strength 2 examples:
- Cat
- Ocelot
- Spider
- Cave Spider

Strength 1 example:
- Pig

Strength 0:
- No scent hint system

## What scent hints classify

Each detected target is marked as one of:
- Prey
- Hostile
- Unknown

This gives directional awareness before direct visual confirmation.
