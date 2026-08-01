# Mechanics and Stats (User)

This page lists player-facing Naturalis mechanics with exact values from current gameplay.

## 1. Morph Knowledge (per morph)

Each morph has its own XP track, display level, and branch-point progression.

- Max level: 5
- XP cap: 3200
- Level thresholds (total XP):

| Level | Total XP required |
|---|---:|
| 0 | 0 |
| 1 | 180 |
| 2 | 360 |
| 3 | 720 |
| 4 | 1280 |
| 5 | 2000 |

Important:
- Display level stops at 5, but XP can continue to 3200 for branch-point progression.
- Max point budget per morph: 119 points.

### Knowledge perks by level

| Branch rank | Vitality (health bonus) | Handling (hotbar slots) | Inventory access | Instinct interval |
|---|---:|---:|---|---:|
| 0 | +0% | 3 | No | every 1 tick |
| 1 | +10% | 4 | No | every 2 ticks |
| 2 | +25% | 6 | No | every 4 ticks |
| 3 | +45% | 8 | No | every 8 ticks |
| 4 | +70% | 9 | No | every 14 ticks |
| 5 | +100% | 9 | Yes | disabled |

### Instinct timing by level

| Wander rank | AFK time before wander |
|---|---:|
| 0 | 300 ticks (15s) |
| 1 | 450 ticks (22.5s) |
| 2 | 700 ticks (35s) |
| 3 | 1000 ticks (50s) |
| 4 | 1400 ticks (70s) |
| 5 | disabled |

## 2. Knowledge XP gain

You gain XP while morphed from movement, actions, and passive gain.

- Passive gain: +1 XP every 600 ticks (30 seconds)

| Activity stat | XP gain rate |
|---|---|
| Walk distance | 1 XP per 3500 cm |
| Jump count | 1 XP per 12 jumps |
| Fall distance | 1 XP per 1800 cm |
| Sprint distance (monster/creature) | 1 XP per 5000 cm |
| Crouch distance (monster) | 1 XP per 2600 cm |
| Swim distance | 1 XP per 2200 cm |
| Walk on water | 1 XP per 2400 cm |
| Aviate distance | 1 XP per 2800 cm |
| Climb distance | 1 XP per 1800 cm |
| Runner sprint | 1 XP per 3800 cm |
| Runner walk | 1 XP per 3000 cm |
| Sneak focus | 1 XP per 1800 cm |
| Jumpy morphs | 1 XP per 6 jumps |

Modded morph bonus:
- Non-minecraft namespace morphs: +1 XP every 1200 ticks (60 seconds)

## 3. Diet system

Diet depends on your current morph body.

- Carnivore: prefers meat
- Herbivore: prefers veggies/neutral
- Omnivore: can eat all food safely

Disliked-food penalty rolls:
- 12%: Poison (120 ticks)
- 28%: Hunger (220 ticks)
- 28%: Nausea (180 ticks)
- 32%: no extra debuff roll

## 4. Resonance and Humanity

Resonance is bonded endgame identity gameplay. Humanity replaces old strain mechanics.

### Bonding and mastery

Requirements to bond:
- Bond target must be a valid living morph
- That morph must have at least 1400 XP

Resonance mastery tiers:

| Tier | XP required |
|---|---:|
| 0 | 0 |
| 1 | 700 |
| 2 | 1400 |
| 3 | 2100 |

Mastery XP from kills while aligned and active:
- Base +12
- +3 more when humanity <= 40
- +4 more when humanity <= 20

### Humanity stages

| Humanity | Stage |
|---|---|
| 80-100 | Grounded |
| 60-79 | Drifting |
| 40-59 | Split |
| 20-39 | Feral |
| 1-19 | Primal |
| 0 | Lost |

### Humanity drift and recovery while active

Aligned and morphed:
- -1 humanity every 12000 ticks (10 minutes)
- -3 humanity every 72000 ticks (60 minutes)
- Additional -1 every 72000 ticks when humanity < 50

Human form (not morphed):
- +1 recovery progress every 120 ticks (6s), converts to +1 humanity per 10 progress
- Effective passive recovery pace: +1 humanity every 60 seconds

Extra pressure in human form:
- At 60-79 humanity: +0.08 exhaustion every 40 ticks (2s)
- At <=19 humanity (without recovery/lock windows): -1 humanity every 200 ticks (10s)

### Resonance damage multiplier

Applies while active and aligned to bonded form.

Archetype base:
- Predator: 1.22x
- Survivor: 1.14x
- Aquatic (in water): 1.18x
- Aquatic (dry): 1.06x
- Other: 1.12x

Humanity factor:
- 80-100: 1.00
- 60-79: 1.05
- 40-59: 1.10
- 20-39: 1.16
- 0-19: 1.22

Mastery factor:
- 1.00 + 0.04 x mastery tier

Instinct active cooldown:
- 80-100 humanity: 45s
- 60-79: 38s
- 40-59: 32s
- 20-39: 26s
- 0-19: 20s
- Then reduced by 2s per mastery tier, minimum 12s

### Morph Binding surge chance during Resonance

Checked every 40 ticks (2s) while aligned and morphed.

By knowledge level:
- Level 0-1: 18%
- Level 2: 12%
- Level 3: 7%
- Level 4: 3%
- Level 5: 1%

Low-humanity modifier:
- +3% flat chance when humanity <= 20

## 5. Rebirth, tokens, and fracture thresholds

### Rebirth (Lost only)

When humanity reaches 0 and you are morphed:
- Action-bar fracture prompt appears in red for 10 seconds
- Curl Up key can trigger rebirth only when curl cannot start
- Rebirth command also works: /morph resonance rebirth

Rebirth effects:
- Humanity set to 100
- Forced back to human form
- Old morph body spawns at your previous position
- Your full inventory is stored in that body
- You respawn randomly within 50 blocks of old body
- Kill old body to recover stored items

Recovery windows after rebirth:
- Recovery window: 5 seconds
- Human-form lock: 10 seconds

### Humanity tokens

- Minor Humanity Token: +5 humanity
- Greater Humanity Token: +10 humanity
- Memory Token: +15 humanity, opens short human recovery window

Restriction:
- Minor/Greater Humanity Tokens cannot be used while Lost
- Memory Token can still be used while not Lost conditions permit

## 6. Human-form penalties by humanity

These apply while Resonance is active and you are in human form.

### At <=80 humanity

If you eat food outside your bonded morph diet:
- You lose extra nutrition/exhaustion after eating

### At <=60 humanity

Wrong-diet penalty becomes stronger.

Tools also decay in effectiveness:
- Break speed and attack output are reduced
- Tier capability is preserved (for example a diamond pick can still mine obsidian)
- Additional instability: tool actions can fumble in human form
	- 60-41 humanity: 30% fumble chance
	- 40-21 humanity: 55% fumble chance

### At <=40 humanity

Wrong-diet food is blocked completely in human form.

### At <=20 humanity

Human tools are rejected:
- Tool use always fumbles/gets blocked
- Mining speed is effectively zero with tools
- Tool attack behaves like empty-hands intent

Additional sleep lock:
- While morphed, you cannot sleep in beds anymore

## 7. Morph potions (all values)

### Morph Binding potions

Effect: Morph Binding
- Drink duration: 8 minutes (9600 ticks)
- Splash applies around impact radius: 4 blocks
- Lingering cloud radius: 6 blocks
- Lingering cloud duration: 200 ticks (10s)
- Cloud reapplies every 10 ticks (0.5s)

### Brewed Morph potions

Effect: Brewed Morph (target from potion data)
- Default duration: 60 seconds (1200 ticks)
- Minimum allowed duration: 20 ticks (1s)
- Splash radius: 4 blocks
- Lingering radius: 6 blocks
- Lingering cloud duration: 200 ticks (10s)
- Cloud apply interval: every 10 ticks (0.5s)

Targets:
- Splash/Lingering can transform players and valid mobs
- Excluded targets: bosses and mini-boss style entities (for example Ender Dragon, Wither, Warden, Ravager, Elder Guardian, high-health monsters)

## 8. Natural attacks

Natural attacks trigger when morphed and attacking without a tool-type weapon.

Guardian/Elder Guardian beam:
- Type: Guardian Beam
- Guardian default config: 5 damage, 18 range, 32 tick cooldown
- Elder Guardian default config: 8 damage, 24 range, 36 tick cooldown
- Beam now always emits visible spark beam particles and applies damage reliably when in range

## 9. Morphed music alteration

When you are morphed, background music volume is altered by humanity stage.

Music volume multiplier while morphed:
- Humanity <=20: 0.35x
- Humanity <=40: 0.50x
- Humanity <=60: 0.65x
- Humanity <=80: 0.80x
- Humanity >80: 0.90x

When you unmorph or log out, music volume is restored to your normal configured Music slider value.

## 10. Language readability while morphed

Sign rewriting has been removed (for performance reasons).

New behavior:
- While morphed with reduced literacy and looking at a sign, you get a lightweight action-bar cue instead of world text mutation.
- Chat comprehension behavior remains active through morph literacy (clear/partial/garbled based on morph profile).

## 11. Species communication call (shift)

The social group-call now only triggers if same-species allies are actually nearby.

- Search radius: 24 blocks
- Trigger requirement: at least one same-species ally within 12 blocks
- If this requirement is not met, no call message, no call particles, and no rally effects are emitted.

## 12. Morph-specific movement (Primal Movement key)

Default key: G (Primal Movement)

While morphed, the following extra movement types are available for valid species sets:
- Dive Momentum: flying forms gain speed while diving (elytra-like dive acceleration)
- Quadruped Burst: extra ground burst while sprinting
- Leap Bound: jump + primal movement gives forward pounce
- Wall Scramble: wall-contact upward scramble for agile/climbing forms
- Aquatic Jet: sprint + primal movement in water for dash-like swim acceleration

## 13. Advancement background

Naturalis advancements now use a custom mod background texture instead of vanilla advancement backgrounds.
