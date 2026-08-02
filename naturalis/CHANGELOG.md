# Changelog

All notable changes to Naturalis are documented here.

## [1.7.0] — 2026-08-02

### Changed

- **Version** — release line is now **1.7.0** (was 1.6.2.1 / 1.6.3 WIP).
- **Forge 1.20.1** — Survival as… is not available (no Create World mode, no `/morph survival_as` tree). Habit Chrysalis remains a placeholder item.

### Fixed

- **Natural Dimension FPS** — Echo Sovereign boss fog disabled (vanilla world-fog was crushing client FPS nearby); Forge no longer double-scans a ±2048 AABB for the boss every tick; storm effects reapplied every 40 ticks only when missing; entity budget culls near players only (cap 100).

### Added

- Fabric 1:1 gameplay hooks parity batch (Survival as…, inventory/mouth, instinct/diet/resonance/HUD/quick-slot) on 1.21.1 / 1.21.8.

---

## [1.6.3] — 2026-08-01

### Added

- **Bundled mob profiles (batch expansion)** — complete Naturalis profiles + namespace defaults for:
  - Naturalist, Aquamirae, L_Ender's Cataclysm, Friends & Foes
  - Critters and Companions, Twilight Forest, Goety, The Aether
  - (existing) Mowzie's Mobs, Ice and Fire / Ice and Fire CE, Born in Chaos
- **Natural attack types** — `poison_bite`, `freeze_zone`, `lightning_strike`, `charge`, `sonic_blast`, with Java defaults for signature mobs across the integrated mods.
- **Morph shape persistence** — captures and restores entity NBT shape data per morph (e.g. Cobblemon species) so forms stay stable instead of changing every tick.
- **Sovereign Amulet instant capture** — normal use fills a vial; shift-click acquires a morph orb unlock for mid/late-game skips of the Echo Forge grind.
- **Walkers ShapeTraits** — Static (anchored, cannot walk), Scentbound (keen smell), Photophobic (sun-stressed), plus flying trait datapacks for Aether and Naturalist flyers (Remorphed menu bubbles).
- **Survival as… game mode** — Create World cycle includes **Survival as…** (between Survival and Hardcore). Mob picker only when that mode is selected; creating without a pick opens the picker. Locked morph identity for the save (no Remorphed menu / acquire except `/morph survival_as unlock`). First spawn relocates to a fitting biome; lore greeting; humanity gauge disabled. Knowledge XP banks at ×20 slower. Hard diet; mild circadian; Habit Chrysalis to imprint a new locked form.

### Fixed

- **Echo Forge** — now mineable with iron pickaxe+ (`mineable/pickaxe`, `needs_iron_tool`); breaking drops stored inventory contents.
- **Forge 1.20.1 Echo Forge** — inventory drop uses `ItemStackHandler` correctly when the block is removed.
- **Flight for profiled flyers** — Naturalist birds/insects and Aether flyers keep creative-style flight (`mayfly`) from `flight_only` / aerial profiles instead of being grounded.
- **Wing animation sync** — flying morph shapes stay marked airborne while the player is flying so wing flaps play.
- **Static morphs** — shulker / aechor plant (and profile `instinct.static`) lock movement in place.
- **Large morph third-person camera** — Leviathan and other oversized morphs pull the 3rd / reverse-3rd (F5) camera farther out and up so the view is no longer inside the model.
- **Species walk speed** — profile `instinct.walk_speed` (plus snail/tortoise/slug heuristics) slows crawl-gait morphs and blocks sprint when gait is too low.
- **Integration inertia** — mass/jump/gravity now apply reliably for all bundled mod morphs (profile mass + heuristics + jump motion scaling on every loader).
- **Integration first-person hands** — modded morphs (e.g. Naturalist snail) no longer show the vanilla player arm; Naturalis hides it and draws the morph limb/shape instead (NeoForge 1.21.1+).
- **Aether morph wings** — Moa/Cockatrice (and other Aether winged birds) sync `NotGrounded` + `animateWings` while morphed so wings fold on the ground and flap in the air instead of staying locked vertical.
- **Static morphs** — all instincts disabled (hunt, fear, wander, smell, group); sessile shapes stay anchored only. Generic monster hunt prey no longer applies to shulker / aechor / `instinct.static`.
- **Remorphed traits guide** — “Traits Info” button on the morph select menu opens a scrollable encyclopedia of Naturalis + Walkers traits with icons; trait bubbles stay visible on that screen.
- **Floating trait** — `naturalis:floating` (+ Walkers `stand_on_fluid` water): chickens, ducks, and waterfowl stay on the surface and cannot dive; duck profile no longer forces flight-only.
- **Walkers wiki traits coverage** — datapacks + predicates fill aquatic, climb, cant_swim/undrownable, slow falling, powder-snow/cant_freeze, lava walk, and burn-in-daylight across vanilla and bundled integration morphs.
- **Performance** — instinct fear/hunt no longer run every tick (floor ≥10); fear uses one entity scan; smell/hearing ranges capped; Natural Dimension dropped ±512/±2048 island-wide entity scans; wander proxy no longer double-ticks AI.
- **1 FPS / Natural Dimension lag** — vision post-effect no longer reloads every tick when scent ribbons flicker; island passive boost no longer spawns persistence-required mobs without a cap (culls excess on existing worlds).
- **Natural Dimension entity flood** — water/fish/squid spawners were listed under `creature`, bypassing mob caps (NeoForge warned; ~5k entities). Categories fixed; hard entity budget cull; Echo Sovereign no longer duplicates when the arena chunk unloads; Evoker vex/fang AI removed from the boss.
- **Stale platform biomes** — NeoForge/Forge/Fabric `src/main/resources` biome copies were shadowing fixed `common` datapacks (`DuplicatesStrategy.EXCLUDE`). Removed duplicates so common biomes load; platform keeps loader-specific `naturalis.mixins.json`.
- **Scentbound / Photophobic bubbles** — datapack trait lists + Walkers `AbstractIntegration` so predicate traits survive reload.
- **Cataclysm bosses (full morph kit)** — all eight bosses get complete profiles (diet, mass, instinct/prey/fears, environment, vision filters, quick-slot, resonance); new profiles for baby Leviathan, Acropolis mobs, drowned host; Walkers aquatic/lava/flying/scent/photophobic/sink traits; expanded natural attacks + third-person camera for oversized bosses.
- **NeoForge 1.21.8 Survival as…** — mixin/config drift, Walkers package move (`dev.tocraft`), traits payload registration, Create World game-mode cycle, and ARGB text rendering on picker/traits screens.
- **Survival as… lock** — world storage lock instead of Morph Binding potion; Remorphed/Walkers menus blocked while locked; Realistic/Softened choice skipped (auto Realistic).
- **Survival as… traits popup** — no longer chased/overwritten by Remorphed auto-open; panel UI with trait icons, clean titles, hunter prey list, and Walkers feared/reinforcements/prey creature lists.
- **Survival as… morph knowledge XP** — remainder bank + NBT writes persist correctly so ×20 slowdown still grants XP over time.
- **Quadruped inventory** — when mouth/unlocked hotbar slots are empty, items cascade forward from locked hotbar/backpack slots so pickups are not lost until Handling is fully unlocked.

---

## [1.6.2.1] — 2026-06-24

### Fixed

- **Crafting recipes** — all Naturalis recipes now use the correct ingredient JSON (`{ "item": ... }` / `{ "tag": ... }`) on NeoForge 1.21.1 and 1.21.8; items and tools craft again instead of only blocks showing in the book.
- **Forge 1.20.1** — Echo Forge log tag fixed (`minecraft:logs`); quick-slot wheel morph selection works (`CompatAccess.getEntityType` registry fallbacks).
- **NeoForge 1.21.8** — client mixin config aligned with compiled classes (fixes startup crash); quick-slot and Morph Beacon target mode intact.

---

## [1.6.2] — 2026-06-03

### Added

- **Mob profile datapacks** — tune per-entity morph behaviour (diet, mass, instincts, environment, vision, quick-slot categories) via JSON in `mob_archetypes/`, `mob_profiles/`, and `mob_namespace_defaults/`. Reloads with `/reload`. See [mob profiles wiki](docs/wiki/20-mechanisms/mob-profiles-datapack.md).
- **Expanded diet types** — piscivore, insectivore, necrovore, frugivore, fungivore, hematophage, scavenger, lithovore, florivore, nectarivore (13 total).
- **Bundled mod profiles** — Mowzie's Mobs, Ice and Fire, and Born in Chaos ship with Naturalis archetypes and per-mob overrides.

### Fixed

- **Forge 1.20.1** — recipes now use 1.20.1 JSON format (crafting no longer fails); morph acquisition works for modded entities ("not a living entity" resolved).
- **NeoForge 1.21.1** — dedicated server no longer crashes from client-only classes loading on startup.
- **NeoForge 1.21.8** — build restored; reload listeners, entity spawn compat, and Morph Beacon GUI aligned with current APIs.

---

## [1.6.1] — 2026-06-03

### Fixed

- **Morph Beacon** — hostile/passive/all-mob modes now replace mobs in range (e.g. zombies → sheep); reverts when they leave the zone. Players still use Walkers morph.
- **Morph Beacon GUI** — target mode buttons visible again (empty vanilla widgets no longer cover custom labels); Morph ID label no longer overlaps the input field.
- **Morph Beacon** — witches, evokers, and ravagers now morph in range (same result as brewed morph, but immediate); tick rate doubled for faster application.
- **Wander instinct** — head/body turn toward movement for all morphs (not only flying); invisible AI proxy re-enabled in-world for ground-mob pathfinding without camera teleports.
- **Morph Beacon** — morph ids normalized (`sheep` → `minecraft:sheep`); entity spawn uses direct `EntityType.create` on 1.20.1.
- **Wander instinct** — runs every tick while AFK/feral instead of only on the throttled instinct interval (smoother movement).
- **Wander eligibility** — unknown modded morph types no longer default to wander.

### Added

- **Instinct debug** — launch with `-Dnaturalis.instinct.debug=true` for wander/fear/AFK state logs (`naturalis-instinct` logger).
- **Vanilla instinct coverage** — iron golem fear (zombies), zoglin fear (piglins), angry bee fear (bears), expanded hunt prey (pillagers, iron golem, dolphin, phantom, warden, etc.).

---

## [1.6.0] — 2026-06-03

### Added

- **Experience modes** — choose **Realistic** or **Softened** per world on first join (or via `/naturalis experience`). Realistic enables full embodiment; Softened keeps vision, scent, and dig blend while toning down nausea-prone camera effects.
- **Morph head & camera movement** — archetype gait sway (sniff prowl, trot, skitter, peck, swim sway), neck limits, dig-view pull, first-person morph body, and related embodiment feedback in Realistic mode.
- **Scent vision** — active sniff (Shift + use on supported morphs) briefly overlays scent-aware vision and head motion; intensity scales with nearby trails, prey, and hostiles.
- **Scent ribbons** — continuous meandering trail ribbons for prey, hostiles, and deep-sniff targets (configurable via client scent-trail HUD option).
- **Morph quick-slot wheel** — hold **G** to open a radial wheel, aim with the mouse (or number keys), release to morph; quick **tap G** toggles human ↔ current morph. Slots are grouped by category and assignable from the wheel UI.
- **Species secondary actions (WIP)** — right-click interactions while morphed: **Sniff**, **Listen**, and **Peck** for supported bodies (more species coverage coming).

### Changed

- Smell sense scans continue in the background for eligible morphs; ribbons and scent vision layer on top for active sniff moments.
- Morph Binding and Resonance guards cooperate with the quick-slot session so the wheel can use the transform key without breaking bonded-morph rules.

### Known / WIP

- Secondary species actions are early — not every morph has a unique interaction yet.
- Some embodiment and ribbon tuning is still in progress; report rough edges on GitHub or Discord.

### Fixed

- Forge 1.20.1 packaging duplicate `naturalis.mixins.refmap.json` on jar build.
- Quick-slot morph selection and transform-key conflicts on Forge (network payloads, binding session sync, hold-vs-tap G handling).

---

## [1.4.0]

See [patch notes 1.4.0](docs/wiki/00-welcome/patch-notes-1.4.0.md) for Resonance, Humanity, potions, Natural Dimension, and combat identity details.
