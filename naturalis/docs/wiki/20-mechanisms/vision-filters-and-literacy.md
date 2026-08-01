# Vision Filters and Literacy

Naturalis can alter your perception while morphed.

## Color/vision filters

Vision filter system can be enabled or disabled with gamerule:
- `naturalisEnableColorFilter` (default: enabled)

Main filter families:
- Wolf vision
- Mammal vision
- Avian vision
- Aquatic vision
- Reptile vision
- Undead vision
- Nether vision
- Arcane vision

Special vanilla effects:
- Creeper filter
- Spider filter
- Enderman invert effect

Humanoid forms keep normal vision (no filter), including villager/witch/illager-like forms.

## Literacy states

Each morph falls into one literacy profile:
- Clear
- Partial
- Garbled

Partial literacy:
- Message keeps some letters and punctuation, with fragmented words.

Garbled literacy:
- Letters/numbers are replaced with symbol glyphs.

## Sign readability behavior

- Heavy world sign rewriting was removed.
- If your current morph cannot read clearly, you get an action-bar cue instead.

## Morphed music perception (humanity-linked)

While morphed, your music volume multiplier is:
- `<=20 humanity`: `0.35x`
- `<=40`: `0.50x`
- `<=60`: `0.65x`
- `<=80`: `0.80x`
- `>80`: `0.90x`

When you return to human form or leave, music returns to your normal slider value.
