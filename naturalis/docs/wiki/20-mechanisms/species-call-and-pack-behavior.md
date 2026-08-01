# Species Call and Pack Behavior

This mechanism controls same-species ally reactions around you.

## Passive pack attraction

Every `20 ticks` (`1 s`), nearby same-species allies can adjust toward your position.

Base radii:
- Attraction radius: `20 blocks`
- Soft orbit radius: `8 blocks`

Social rank effects:
- Rank `>=1`: radius boosts to `26` and soft radius to `9.5`
- Rank `>=3`: short regeneration pulses can affect you and close allies
- Rank `>=4`: assist radius can reach `30 blocks`

## Species call (Shift)

Call conditions:
- You must hold Shift.
- Same-species allies are searched in `24 blocks`.
- At least one same-species ally must be within `12 blocks`.

If conditions fail:
- No call message
- No rally particles
- No ally rally effects

If call succeeds:
- Up to `20` allies are rallied.
- Cooldown:
  - Social rank 0-1: `60 ticks` (`3 s`)
  - Social rank 2+: `30 ticks` (`1.5 s`)

At social rank `5`:
- Nearby rallied allies gain short Speed and Strength buffs (`100 ticks` / `5 s`).
- You also gain a short Speed buff (`100 ticks` / `5 s`).
