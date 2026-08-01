# Humanity and Loss of Self

This is the central risk system of Naturalis endgame.

## Humanity values and stages

Range: `0` to `100`

Stages:
- `80-100`: Grounded
- `60-79`: Drifting
- `40-59`: Split
- `20-39`: Feral
- `1-19`: Primal
- `0`: Lost

## Passive drift while aligned and morphed

- `-1 humanity` every `10 min`
- `-3 humanity` every `60 min`
- Extra `-1` every `60 min` when humanity `<50`

## Human-form behavior under pressure

Recovery baseline:
- `+1 recovery progress` every `6 s`
- `10 progress` => `+1 humanity`

Extra pressure in human form:
- At `60-79`: exhaustion pressure pulses can appear
- At very low humanity (`<=19`) and outside recovery windows, further rapid decay can occur

## Tool and diet collapse thresholds

At `<=80`:
- Wrong-diet eating penalties begin in human form

At `<=60`:
- Wrong-diet penalties intensify
- Tool stability decays
- Fumble rates:
  - `60-41`: `30%`
  - `40-21`: `55%`

At `<=40`:
- Wrong-diet food is fully blocked in human form

At `<=20`:
- Human tools are effectively rejected
- Tool mining becomes near-zero effectiveness
- While morphed, bed sleep can be blocked

## Lost state (0 humanity)

At Lost:
- You are fully humanity-locked until recovery/rebirth conditions are met.
- Form control becomes extremely restrictive.

Rebirth effects:
- Humanity reset to `100`
- Forced human return
- Old body stores your inventory
- Respawn around `50 blocks` from old body

Post-rebirth windows:
- Recovery window: `5 s`
- Human-form lock: `10 s`

## Recovery items

- Minor Humanity Token: `+5`
- Greater Humanity Token: `+10`
- Memory Token: `+15` and opens a short recovery window

Restriction:
- Minor and Greater tokens cannot be used in Lost lock state.
