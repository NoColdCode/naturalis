# CurseForge release notes — Naturalis 1.6.2

Paste into the **Changelog** field on your CurseForge file upload.

---

## Naturalis 1.6.2 — Mob profiles & mod compatibility

**Datapack-driven morph tuning plus bundled profiles for popular mob mods.**

### New

- **Mob profile datapacks** — override diet, mass, instincts, environment, vision, and quick-slot behaviour per entity via JSON (`mob_archetypes`, `mob_profiles`, `mob_namespace_defaults`). Reload with `/reload`.
- **10 new diet types** — piscivore, insectivore, necrovore, frugivore, fungivore, hematophage, scavenger, lithovore, florivore, nectarivore.
- **Bundled profiles** — Mowzie's Mobs, Ice and Fire, Born in Chaos included out of the box.

### Fixes

- **Forge 1.20.1** — crafting recipes and morph acquisition for modded mobs.
- **NeoForge 1.21.1** — dedicated server startup crash (client classes on server).
- **NeoForge 1.21.8** — module builds and runs again on current NeoForge APIs.

**Requires:** CraftedCore, Walkers, ReMorphed.
