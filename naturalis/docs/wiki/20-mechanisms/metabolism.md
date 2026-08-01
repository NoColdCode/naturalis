# Metabolism

Metabolism represents how demanding a morph body is.

## Metabolism multiplier

Metabolism multiplier uses morph mass:

- Formula: `0.5 + (log(mass + 1) / log(8)) x 1.5`
- Clamp: minimum `0.5`, maximum `2.0`

Higher values mean a body with higher biological demand in the mod's survival loop.

## Example mass anchors (used by Naturalis)

Very light:
- Bee: `0.3`
- Endermite: `0.3`
- Silverfish: `0.4`
- Rabbit: `0.5`

Light:
- Cave Spider: `0.8`
- Spider: `1.0`
- Skeleton: `1.2`
- Fox: `1.5`

Medium:
- Creeper: `1.8`
- Sheep: `2.0`
- Zombie: `2.0`
- Wolf: `2.5`
- Pig: `2.5`

Heavy:
- Cow: `4.0`
- Enderman: `4.0`
- Horse: `5.0`
- Guardian: `6.0`
- Ender Dragon: `7.0`

Very heavy:
- Ravager: `8.0`
- Shulker: `8.5`
- Warden: `9.0`
- Iron Golem: `10.0`

## Unknown/modded morph fallback

If a creature is not on the explicit list, Naturalis estimates mass from its size and creature category, then clamps fallback mass into `0.3` to `12.0`.
