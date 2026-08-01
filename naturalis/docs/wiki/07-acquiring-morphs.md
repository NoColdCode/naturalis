# Acquiring Morphs

This page explains the normal player path to unlock a morph in Naturalis.

## Overview

You unlock most morphs in 3 steps:

1. Capture creature echo in Filled Echo Vials.
2. Infuse those vials in the Echo Forge to create a Morph Orb.
3. Use the Morph Orb to permanently acquire that morph.

## Step 1: Capture echo with the Echo Collector

Required items:
- Echo Collector
- Empty Echo Vials

How to capture:
1. Find a living target mob you want.
2. Weaken it first.
3. Right-click the target with Echo Collector.

Capture rules:
- Target must be alive.
- Target must be below 30% HP.
- Full-health targets cannot be captured.
- You must have at least one Empty Echo Vial in your inventory.

On success:
- 1 Empty Echo Vial is consumed.
- You receive 1 Filled Echo Vial with that mob ID stored.

## Step 2: Create a Morph Orb in the Echo Forge

How the Echo Forge works:
- It has 5 input slots for Filled Echo Vials.
- All 5 vials must be Filled Echo Vials of the same mob.
- Output slot must be empty.
- Forge time is 800 ticks (40 seconds).

Result:
- Consumes all 5 vials.
- Produces 1 Morph Orb with the same stored mob ID.

## Step 3: Use the Morph Orb

1. Hold the Morph Orb.
2. Right-click to use it.

Behavior:
- If the orb has a valid mob ID, the morph is acquired.
- In survival/adventure, the orb is consumed on success.
- In creative, the orb is not consumed.

## Alternative methods

Servers may allow command-based acquisition.

Common admin/test command:
- /morph acquire <namespace:entity_id>

## Troubleshooting

If capture does not work:
- Check the target HP is under 30%.
- Make sure you have Empty Echo Vials available.
- Ensure the target is a valid living entity.

If Echo Forge does not start:
- Ensure all 5 vials are the same mob.
- Ensure output slot is empty.

If Morph Orb does nothing:
- The stored mob ID may be invalid.
- Recraft a new orb from correctly captured vials.
