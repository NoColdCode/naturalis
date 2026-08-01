# Natural Attacks

Natural Attacks are morph-based combat moves that replace or enhance normal punches when you are morphed.

## When Natural Attacks trigger

Natural Attacks trigger when all of these are true:

- You are currently morphed.
- You attack a living target.
- You are not using a tool/weapon item.

If you are holding a normal tool/weapon, vanilla combat is used instead.

## Items that disable Natural Attack replacement

If your main hand item is one of these categories, Naturalis keeps normal vanilla behavior:

- Tiered weapons/tools (sword, pickaxe, axe, etc.)
- Digger tools
- Shears
- Bow
- Crossbow
- Trident

Practical rule:
- Empty hand or non-tool item -> Natural Attack system.
- Real weapon/tool -> vanilla hit logic.

## Cooldown behavior

Each Natural Attack has a cooldown.

- If attack is still on cooldown, the hit is blocked.
- You receive a short action-bar cooldown message.
- Cooldown display is in seconds.

## Default attack style by morph

Naturalis uses built-in defaults per morph type.

### Fireball users
- ghast
- wither
- blaze (small fireball variant)

### Arrow users
- skeleton
- stray
- pillager

### Dash bite users
- wolf
- fox

### Leap attack users
- spider
- cave_spider
- rabbit

### Enhanced melee users
- enderman
- iron_golem
- zombie

### Fallback for other morphs
If a morph has no specific profile, it gets an enhanced melee fallback profile.

## What each attack type does

### Fireball
- Launches a large fireball projectile.
- Strong cooldown and ranged pressure.

### Small fireball
- Faster, lighter projectile than large fireball.

### Arrow
- Fires an arrow projectile with morph-defined damage.

### Dash bite
- You lunge forward.
- If target is close enough, it deals damage and knockback.

### Leap attack
- Applies a forward/upward leap.
- Deals melee damage in close range.

### Enhanced melee
- Short-range heavy melee hit.
- Higher knockback than normal punch.

## Tuning values (damage/range/cooldown)

Natural Attacks are data-configurable.

Config file:
- config/naturalis/natural_attacks.json

What can be tuned per morph:
- type
- damage
- range
- velocity
- cooldown
- replace_normal_attack

Notes:
- Cooldown is measured in ticks (20 ticks = 1 second).
- If replace_normal_attack is true, vanilla attack is canceled after the Natural Attack is used.

## Quick combat tips

- Keep your hand free to use morph attacks.
- Respect cooldown windows and avoid spam clicks.
- Use range-based morphs (arrow/fireball) to open fights.
- Use dash/leap morphs for gap-closing and pressure.
