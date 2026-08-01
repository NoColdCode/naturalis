# Resonance Quick Explanation

Resonance is your advanced identity system.

You choose one bonded morph and become stronger while playing in that body, but you also risk losing humanity if you push too far.

## Requirements

To set a bond:
- The target must be a valid living morph.
- That morph must have at least `1400 XP`.

## Mastery tiers

- Tier 0: `0 XP`
- Tier 1: `700 XP`
- Tier 2: `1400 XP`
- Tier 3: `2100 XP`

## Humanity scale

- Humanity starts at `100` when Resonance is first enabled.
- Minimum is `0`, maximum is `100`.
- Stage ranges:
  - `80-100`: Grounded
  - `60-79`: Drifting
  - `40-59`: Split
  - `20-39`: Feral
  - `1-19`: Primal
  - `0`: Lost

## Drift and recovery

Aligned and morphed:
- `-1 humanity` every `10 min`
- `-3 humanity` every `60 min`
- Extra `-1` every `60 min` if humanity is below `50`

In human form:
- `+1 recovery progress` every `6 s`
- `10 progress` = `+1 humanity`
- Effective passive pace: `+1 humanity` per `60 s`

## Damage scaling while active and aligned

Your final multiplier combines:
- Archetype base
- Humanity factor
- Mastery factor

Archetype base:
- Predator: `1.22x`
- Survivor: `1.14x`
- Aquatic in water: `1.18x`
- Aquatic on land: `1.06x`
- Other: `1.12x`

Humanity factor:
- `80-100`: `1.00`
- `60-79`: `1.05`
- `40-59`: `1.10`
- `20-39`: `1.16`
- `0-19`: `1.22`

Mastery factor:
- `1.00 + (0.04 x tier)`

## Active Instinct cooldown

Base cooldown by humanity:
- `45 s`, `38 s`, `32 s`, `26 s`, `20 s` (from highest to lowest humanity)

Then reduced by mastery:
- `-2 s` per tier
- Minimum `12 s`

## Lost state and rebirth

At humanity `0` (Lost), you can trigger rebirth under the right conditions.

Rebirth effects:
- Humanity reset to `100`
- Forced back to human form
- Old body remains with your inventory stored inside
- You respawn within about `50 blocks`

Recovery windows:
- Recovery window: `5 s`
- Human-form lock: `10 s`
