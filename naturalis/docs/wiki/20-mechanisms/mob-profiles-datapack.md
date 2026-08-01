# Mob profiles (datapack)

Naturalis can tune **per-entity morph behaviour** through JSON datapacks — the same dimensions used for vanilla mobs (diet, mass/inertia, instincts, environment, vision filters, quick-slot categories, and more).

Profiles load from **all datapacks** (mod jar, world datapack, resource pack) and reload with `/reload`.

## File locations

| Purpose | Path |
|---------|------|
| Reusable templates | `data/<any_namespace>/mob_archetypes/<name>.json` |
| Per-entity overrides | `data/<any_namespace>/mob_profiles/<entity_namespace>/<entity_path>.json` |
| Mod-wide defaults | `data/<any_namespace>/mob_namespace_defaults/<mod_namespace>.json` |

**Bundled with Naturalis:**

- `data/naturalis/mob_archetypes/` — generic archetype bank (predator, undead, draconic, etc.)
- `data/naturalis/mob_profiles/mowziesmobs/` — Mowzie's Mobs
- `data/naturalis/mob_profiles/iceandfire/` — Ice and Fire / Ice and Fire CE
- `data/naturalis/mob_profiles/born_in_chaos_v1/` and `born_in_chaos/` — Born in Chaos
- `data/naturalis/mob_profiles/naturalist/` — Naturalist
- `data/naturalis/mob_profiles/aquamirae/` — Aquamirae
- `data/naturalis/mob_profiles/cataclysm/` — L_Ender's Cataclysm
- `data/naturalis/mob_profiles/friendsandfoes/` — Friends & Foes
- `data/naturalis/mob_profiles/crittersandcompanions/` — Critters and Companions
- `data/naturalis/mob_profiles/twilightforest/` — The Twilight Forest
- `data/naturalis/mob_profiles/goety/` — Goety
- `data/naturalis/mob_profiles/aether/` — The Aether

**Natural attacks** for these mods ship as Java defaults in `NaturalAttackManager` (types include fireball, freeze zone, lightning strike, poison bite, charge, sonic blast, etc.). Override via `config/naturalis/natural_attacks.json`.

## Quick start — add your own mob

1. Create a datapack folder (or edit an existing one).
2. Add a profile file, for example:

`data/mypack/mob_profiles/crittersandcompanions/red_panda.json`

```json
{
  "entity": "crittersandcompanions:red_panda",
  "inherits": "naturalis:ground_herbivore",
  "diet": "frugivore",
  "mass": 2.1,
  "vision": {
    "shader": "naturalis:mammal_vision",
    "chromatic_mode": "dichrome"
  },
  "tags": ["cute", "arboreal"]
}
```

3. Run `/reload` (singleplayer) or restart the server.
4. Test: `/morph acquire crittersandcompanions:red_panda` and `/morph debug diet crittersandcompanions:red_panda minecraft:apple`

## Inheritance

Use `"inherits": "naturalis:<archetype_id>"` to extend a template. Child fields **override** parent values.

Archetypes can also inherit other archetypes.

Example — boss based on draconic template:

```json
{
  "entity": "mymod:thunder_serpent",
  "inherits": "naturalis:draconic",
  "mass": 9.5,
  "quick_slot": {
    "categories": ["aerial", "hostile", "high_damage"],
    "primary": "high_damage"
  }
}
```

## Namespace defaults

For mods with many mobs, set a fallback archetype for the whole namespace:

`data/mypack/mob_namespace_defaults/mymod.json`

```json
{
  "namespace": "mymod",
  "default_inherits": "naturalis:large_predator",
  "overrides": {
    "tiny_mite": {
      "inherits": "naturalis:insect_arthropod",
      "mass": 0.4
    }
  }
}
```

Any entity in `mymod:*` without its own profile uses `default_inherits`. Entries in `overrides` are merged per entity path.

## Diet types

| Value | Behaviour |
|-------|-----------|
| `carnivore` | Meat + neutral foods |
| `herbivore` | Vegetables + neutral |
| `omnivore` | Everything |
| `piscivore` | Fish / seafood |
| `insectivore` | Insects, spider eyes |
| `necrovore` | Rotten flesh, carrion |
| `frugivore` | Fruit, berries |
| `fungivore` | Mushrooms |
| `hematophage` | Raw meat only |
| `scavenger` | Meat, rotten flesh, opportunistic |
| `lithovore` | Minerals (iron, stone, nuggets) |
| `florivore` | Flowers + plants |
| `nectarivore` | Honey, flowers, fruit |

## Full profile schema

```json
{
  "entity": "namespace:path",
  "inherits": "naturalis:archetype_name",
  "diet": "carnivore",
  "mass": 4.5,
    "instinct": {
    "wander": true,
    "flight_only": false,
    "static": false,
    "walk_speed": 1.0,
    "smell_strength": 0,
    "nyctalop_hostile": false,
    "fears": ["water", "cats", "wolves", "iron_golem", "zoglins", "bees"],
    "hunt_prey": ["minecraft:sheep", "minecraft:rabbit"]
  },
  "environment": {
    "cold_vulnerable": false,
    "hot_vulnerable": false,
    "wet_vulnerable": false,
    "dry_vulnerable": false,
    "sunlight_sensitive": false
  },
  "biome_suitability": {
    "volcanic": false,
    "snow_adapted": false,
    "ender_adapted": false,
    "cave_adapted": false
  },
  "vision": {
    "shader": "naturalis:mammal_vision",
    "chromatic_mode": "dichrome",
    "kaleido_strength": 0.0,
    "kaleido_folds": 0,
    "spectral_profile": 0.0,
    "motion_trail": 0.5,
    "canine_predator": false
  },
  "quick_slot": {
    "categories": ["ground", "hostile"],
    "primary": "hostile"
  },
  "resonance": {
    "archetype": "predator"
  },
  "tags": ["custom", "tags"]
}
```

### Vision shaders (`vision.shader`)

Built-in Naturalis shader families:

- `naturalis:wolf_vision`, `mammal_vision`, `avian_vision`, `aquatic_vision`
- `reptile_vision`, `undead_vision`, `nether_vision`, `arcane_vision`
- `insect_vision`, `cephalopod_vision`, `abyssal_vision`, `fungal_vision`
- `crystalline_vision`, `ferrous_vision`, `fae_vision`, `tempest_vision`
- `viscous_vision`, `void_vision`

`chromatic_mode`: `dichrome`, `mono`, or `quad`.

### Quick-slot categories

`ground`, `aerial`, `aquatic`, `nether`, `hostile`, `high_damage` (aliases: `air`, `flying`, `water`, `boss`).

## Resolution order

For a morph `modid:mob`:

1. Explicit `mob_profiles` entry (or namespace `overrides`)
2. Namespace `default_inherits` archetype chain
3. Hardcoded Naturalis logic (vanilla tables, Alex's Mobs, keyword heuristics)

Datapack values always win when present.

## Tips

- Use **`/morph debug diet <morph> <item>`** to test food compatibility.
- Mass affects **metabolism** and **movement inertia** (via `MassInertiaManager`).
- `instinct.walk_speed` is a species gait multiplier (e.g. snails `0.18`) applied on top of mass inertia; values under `~0.52` also block sprinting.
- If a mob still feels wrong, copy the nearest archetype and tweak one field at a time.
- For modpacks: ship a small datapack instead of forking the mod jar.

## Included mod packs

Naturalis 1.6.2+ ships profiles for:

- **Mowzie's Mobs** (`mowziesmobs:*`) — foliaath, naga, frostmaw, wroughtnaut, umvuthana, barako, etc.
- **Ice and Fire** (`iceandfire:*`) — dragons, hippogryph, cyclops, myrmex, sea serpent, etc.
- **Born in Chaos** (`born_in_chaos_v1:*` and `born_in_chaos:*`) — skeleton variants, spirits, pumpkin bosses, etc.

Missing mobs from those mods still receive the namespace default archetype until you add a profile.
