# Patch Notes 1.4.0

This page summarizes the gameplay changes and values for version **1.4.0**.

## New and expanded systems

- Humanity-driven Resonance progression now defines your risk/reward loop.
- Natural movement system expanded with Primal Movement actions.
- Morph communication call now requires nearby same-species allies.
- Morph music and readability behavior now depend on your current state.
- Natural Dimension survival pressure and weather identity were reinforced.

## Resonance and Humanity values

- Humanity range: `0` to `100`
- Humanity stages:
  - `80-100`: Grounded
  - `60-79`: Drifting
  - `40-59`: Split
  - `20-39`: Feral
  - `1-19`: Primal
  - `0`: Lost
- Bond requirement: your bond target must have at least `1400` morph XP.
- Mastery tiers:
  - Tier 0: `0 XP`
  - Tier 1: `700 XP`
  - Tier 2: `1400 XP`
  - Tier 3: `2100 XP`

## Humanity drift and recovery values

While Resonance is active and you are aligned:
- `-1 humanity` every `12000 ticks` (`10 min`)
- `-3 humanity` every `72000 ticks` (`60 min`)
- Additional `-1` every `72000 ticks` when humanity is below `50`

In human form while Resonance is active:
- Recovery progress: `+1 progress` every `120 ticks` (`6 s`)
- `10 progress` converts to `+1 humanity`
- Effective passive pace: `+1 humanity / 60 s`

## Active Instinct cooldown in Resonance

Base cooldown by humanity:
- `80-100`: `45 s`
- `60-79`: `38 s`
- `40-59`: `32 s`
- `20-39`: `26 s`
- `0-19`: `20 s`

Mastery reduction:
- `-2 s` per mastery tier
- Minimum cooldown: `12 s`

## Potion values (Morph Binding and Brewed Morph)

Morph Binding:
- Duration: `8 min` (`9600 ticks`)
- Splash radius: `4 blocks`
- Lingering radius: `6 blocks`
- Lingering duration: `200 ticks` (`10 s`)
- Reapply interval: every `10 ticks` (`0.5 s`)

Brewed Morph:
- Default duration: `60 s` (`1200 ticks`)
- Minimum duration: `20 ticks` (`1 s`)
- Splash radius: `4 blocks`
- Lingering radius: `6 blocks`
- Lingering duration: `200 ticks` (`10 s`)
- Reapply interval: every `10 ticks` (`0.5 s`)

## Knowledge and control updates

- Max morph level remains `5`.
- Inventory unlock remains at top handling rank.
- Instinct pressure still weakens as your instinct rank climbs and fully stops at rank `5`.

## Sound and readability updates

Morphed music multiplier by humanity:
- `<=20`: `0.35x`
- `<=40`: `0.50x`
- `<=60`: `0.65x`
- `<=80`: `0.80x`
- `>80`: `0.90x`

Sign behavior:
- World sign rewriting was removed.
- Morphed low-literacy players now receive a lightweight action-bar cue instead.

## Natural Dimension pressure

- While inside the Natural Dimension, if Resonance is active: `-1 humanity` every `60 s`.
- Thunderstorms are frequent and can apply both:
  - Storm Attunement
  - Morph Binding

## Witch and caster pressure tuning

- Witch hit can apply Morph Binding for a random `5` to `30` seconds.
- Witch binding re-application cooldown: `60 ticks` (`3 s`) on the same player.
- Witch transmutation gift chance: `10%` with `14 s` internal gift cooldown.
- Evoker transmutation gift chance: `22%` with `9 s` internal gift cooldown.

## Combat identity

Resonance damage now combines:
- Archetype base multiplier
- Humanity stage factor
- Mastery tier factor
- Morph-size factor

This makes each bonded path feel distinct instead of using one flat damage profile.
