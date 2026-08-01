# Morph Armor Forge

The Morph Armor Forge crafts a Morph Armor item tied to a specific morph.

## What it does

The forge creates one Morph Armor item that:
- Stores a target mob ID from a Morph Orb.
- Uses a selected material tier.
- Applies armor stats from that tier.

## Required inputs

The forge has 3 slots:

1. Morph Orb slot
- Must contain a Morph Orb with a valid mob ID.

2. Material slot
- Must contain the material for the selected tier.

3. Output slot
- Must be empty to start crafting.

## Craft time

- 600 ticks (30 seconds)

## Tier materials and stats

| Tier | Material | Cost multiplier | Armor | Toughness | Knockback resist |
|---|---|---:|---:|---:|---:|
| Leather | Leather | 1.0 | 7 | 0 | 0.0 |
| Chainmail | Iron Nugget | 2.0 | 12 | 0 | 0.0 |
| Iron | Iron Ingot | 1.0 | 15 | 0 | 0.0 |
| Gold | Gold Ingot | 1.0 | 11 | 0 | 0.0 |
| Diamond | Diamond | 1.5 | 20 | 8 | 0.0 |
| Netherite | Netherite Ingot | 2.0 | 20 | 12 | 0.1 |

## Material cost formula

Cost depends on morph mass and selected tier.

Formula:
- Base mass cost = max(4, ceil(mass))
- Final cost = ceil(base mass cost x tier multiplier)

This means even very light morphs cost at least 4 material units.

## Example costs

Example: Wolf (mass 2.5)
- Base mass cost = 4
- Leather 4, Chainmail 8, Iron 4, Gold 4, Diamond 6, Netherite 8

Example: Warden (mass 9.0)
- Base mass cost = 9
- Leather 9, Chainmail 18, Iron 9, Gold 9, Diamond 14, Netherite 18

## Craft flow

1. Insert Morph Orb.
2. Select desired tier in the forge UI.
3. Insert enough material for that tier.
4. Wait 30 seconds.
5. Take Morph Armor from output.

## Common failure causes

Craft does not start when:
- Morph Orb slot is empty.
- Orb has no valid mob ID.
- Material does not match selected tier.
- Material count is below required cost.
- Output slot is occupied.
