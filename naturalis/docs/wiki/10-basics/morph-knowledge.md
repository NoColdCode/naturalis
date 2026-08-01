# Morph Knowledge

Each morph has its own XP, points, and branch ranks.

Current system is not only "level up to 5" anymore. It is a branch-based progression model.

## XP, display levels, and points

Per morph:
- XP clamp: `0` to `3200`
- Display levels: `0` to `5`
- Display level milestones:
  - Level 0: `0 XP`
  - Level 1: `180 XP`
  - Level 2: `360 XP`
  - Level 3: `720 XP`
  - Level 4: `1280 XP`
  - Level 5: `2000 XP`

Important:
- You can keep gaining morph XP past display level 5.
- Branch-point progression continues up to `3200 XP`.

Point budget:
- Max branch points per morph: `119`
- Point progress scales with XP from `0` to `3200`.

## Branch list

- Vitality
- Handling
- Instinct
- Wander
- Resonance (Human Connection)
- Damage
- Morph Resistance
- Utilities
- Social

## Practical branch effects

Vitality rank (health bonus):
- Rank 0: `+0%`
- Rank 1: `+10%`
- Rank 2: `+25%`
- Rank 3: `+45%`
- Rank 4: `+70%`
- Rank 5: `+100%`

Handling rank (hotbar and inventory):
- Rank 0: `3 hotbar slots`
- Rank 1: `4 slots`
- Rank 2: `6 slots`
- Rank 3: `8 slots`
- Rank 4-5: `9 slots`
- Inventory unlock: handling rank `5`

Instinct rank (check interval):
- Rank 0: every `1 tick`
- Rank 1: every `2 ticks`
- Rank 2: every `4 ticks`
- Rank 3: every `8 ticks`
- Rank 4: every `14 ticks`
- Rank 5: disabled

Wander rank (AFK threshold):
- Rank 0: `300 ticks` (`15 s`)
- Rank 1: `450 ticks` (`22.5 s`)
- Rank 2: `700 ticks` (`35 s`)
- Rank 3: `1000 ticks` (`50 s`)
- Rank 4: `1400 ticks` (`70 s`)
- Rank 5: disabled

Utilities rank gates:
- Rank 1: tool use as morph
- Rank 2: block placement as morph
- Rank 3: world interactions as morph

Social rank highlights:
- Group call cooldown: `60 ticks` at low rank, `30 ticks` at rank `2+`
- Pack assist radius: `20` blocks, then `30` blocks at rank `4+`

## Upgrade costs by branch

Vitality:
- Rank cost pattern: `1, 2, 3, 4, 5`

Handling:
- Rank cost pattern: `2, 3, 3, 5, 5`

Instinct:
- Rank cost pattern: `1, 1, 1, 1, 5`

Wander:
- Rank cost pattern: `1, 1, 1, 1, 5`

Resonance (Human Connection):
- Rank cost pattern: `1, 2, 3, 4, 5`

Damage:
- Rank cost pattern: `2, 2, 2, 2, 2`

Morph Resistance:
- Rank cost pattern: `1, 2, 3, 4, 5`

Utilities:
- Rank cost pattern: `3, 4, 5`

Social:
- Rank cost pattern: `1, 2, 3, 4, 6`

## Resonance eligibility and mastery link

- A morph becomes resonance-eligible at `1400 XP`.
- Resonance mastery tiers:
  - Tier 0: `0 XP`
  - Tier 1: `700 XP`
  - Tier 2: `1400 XP`
  - Tier 3: `2100 XP`

## XP gain rhythm (player-facing)

- Passive gain: `+1 XP` every `600 ticks` (`30 s`) while morphed.
- Movement and behavior stats add extra XP over time.

## Temporary full unlock behavior

Knowledge Elixir can temporarily force one target morph to max branch ranks for a limited duration, then normal ranks return when the effect expires.
