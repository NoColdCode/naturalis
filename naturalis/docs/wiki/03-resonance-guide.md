# Resonance Guide

Resonance is the advanced identity system in Naturalis.

It is designed so you feel like the bonded morph is your true body, not just a costume.

## Prerequisite

You need a valid living morph with at least `1400 XP` before bonding it.

## Core concepts

- `Bonded Morph`: your chosen primary morph identity
- `Resonance Active`: bonded mode enabled
- `Humanity`: identity stability meter from `0` to `100`
- `Active Instinct`: resonance skill trigger with a dynamic cooldown
- `Archetype`: bonded morph style (`predator`, `survivor`, `aquatic`, or `other`)

## How it works

1. Bond a resonance-eligible morph (`1400 XP` or more).
2. Activate Resonance.
3. Stay in bonded form to remain aligned.
4. Fight and survive as that form to gain mastery XP.

## Alignment and Humanity

While Resonance is active:

- Humanity drifts downward over time while aligned and morphed.
- Human-form recovery can restore humanity gradually.
- As humanity drops, penalties intensify and instincts become harsher.
- At `0` humanity (`Lost`), strict locks apply until recovery/rebirth conditions are met.

This is the main mechanic that enforces embodiment gameplay in the current version.

## Humanity stages

- `80-100`: Grounded
- `60-79`: Drifting
- `40-59`: Split
- `20-39`: Feral
- `1-19`: Primal
- `0`: Lost

## Drift and recovery values

Aligned and morphed:
- `-1 humanity` every `12000 ticks` (`10 min`)
- `-3 humanity` every `72000 ticks` (`60 min`)
- Extra `-1` every `72000 ticks` when humanity is below `50`

Human form recovery:
- `+1 recovery progress` every `120 ticks` (`6 s`)
- Every `10 progress` converts to `+1 humanity`
- Effective passive pace: about `+1 humanity` per `60 s`

## Active Instinct cooldown

Base cooldown by humanity:
- `80-100`: `45 s`
- `60-79`: `38 s`
- `40-59`: `32 s`
- `20-39`: `26 s`
- `0-19`: `20 s`

Mastery reduction:
- `-2 s` per mastery tier
- Minimum cooldown: `12 s`

## Damage profile

Damage is no longer one flat value for all morphs.

It now depends on:

- Bonded archetype
- Current humanity stage
- Mastery tier

This creates different combat identities for different morph families.

Archetype base multipliers:
- `predator`: `1.22x`
- `survivor`: `1.14x`
- `aquatic` in water: `1.18x`
- `aquatic` dry: `1.06x`
- `other`: `1.12x`

Humanity factor:
- `80-100`: `1.00`
- `60-79`: `1.05`
- `40-59`: `1.10`
- `20-39`: `1.16`
- `0-19`: `1.22`

Mastery factor:
- `1.00 + (0.04 x mastery tier)`

## Mastery progression in Resonance

- Kills in aligned bonded form grant mastery XP
- Milestones:
	- Tier 0: `0 XP`
	- Tier 1: `700 XP`
	- Tier 2: `1400 XP`
	- Tier 3: `2100 XP`

## Lost and rebirth

When humanity reaches `0` while morphed:
- You enter Lost state.
- Rebirth can reset your run state and restore humanity to `100`.

Rebirth flow includes:
- Forced return to human form
- Old morph body left behind with your stored inventory
- Respawn around `50 blocks` away

Post-rebirth windows:
- Recovery window: `5 s`
- Human-form lock: `10 s`

## Practical playstyle (current)

- Pick one morph you truly want to main
- Build your routes and fights around that body
- Use Active Instinct windows intentionally
- Keep humanity stable before taking high-risk fights
- Avoid prolonged misalignment while Resonance is active
