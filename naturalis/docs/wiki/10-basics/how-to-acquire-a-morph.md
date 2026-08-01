# How to Acquire a Morph

Standard progression has 3 steps.

## Step 1: Capture echo

Required:
- Echo Collector
- Empty Echo Vials

Capture rules:
- Target must be alive.
- Target HP must be below `30%`.
- You must have at least 1 Empty Echo Vial.

On success:
- Consumes `1` Empty Echo Vial
- Gives `1` Filled Echo Vial with the creature ID

## Step 2: Use the Echo Forge

Forge rules:
- `5` input slots
- All 5 must be Filled Echo Vials of the same creature
- Output slot must be empty

Forge time:
- `800 ticks` (`40 seconds`)

Result:
- Consumes all 5 vials
- Produces 1 Morph Orb for that creature

## Step 3: Use the Morph Orb

- Hold Morph Orb and right-click.
- If the stored target is valid, you permanently unlock that morph.

Consumption:
- Survival/Adventure: orb is consumed
- Creative: orb is not consumed

## Quick troubleshooting

If capture fails:
- Re-check the `30%` HP threshold
- Confirm vials in inventory

If forge fails:
- Confirm all 5 vials match
- Confirm output slot is empty

If orb fails:
- Stored ID is likely invalid
- Craft a new orb from valid filled vials
