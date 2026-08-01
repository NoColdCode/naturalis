# Troubleshooting

Common issues and fixes for players.

## Burst says unavailable

Symptom:

- You cannot trigger `/morph resonance burst`.

Checklist:

1. Make sure a bonded morph is set.
2. Make sure Resonance is active.
3. Make sure you are currently transformed into the bonded morph.
4. Check cooldown status with `/morph resonance status`.

## Resonance collapsed

Symptom:

- Message: `Resonance collapsed. You lost alignment with your bonded form.`

Meaning:

- Strain reached the maximum.

Fix:

1. Re-enter bonded form.
2. Reactivate Resonance.
3. Spend more time aligned with the bonded form to control strain.

## Bond command fails

Symptom:

- Bond cannot be set.

Likely causes:

- Morph is not fully mastered yet.
- Target ID is invalid or not a valid living morph.
- You used alias/current bond form while not morphed.

## Brewed Morph potion does nothing

Symptom:

- Potion appears unusable or has no effect.

Likely causes:

- No valid morph target was stored in potion data.
- Target entity ID is invalid.

## Morph acquisition fails

Symptom:

- Message: could not acquire morph.

Fix path:

1. Confirm entity ID is correct (`namespace:entity`).
2. Confirm target is a valid living morph entry.
3. Retry in a clean environment if other mods change entity registration.

## Still stuck

When reporting an issue to the mod author, include:

- Exact command used
- Exact in-game error message
- Current morph
- Bonded morph
- Whether Resonance is active
- Whether Burst cooldown is running
