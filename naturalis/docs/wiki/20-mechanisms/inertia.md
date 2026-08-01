# Inertia

Inertia is the body-weight movement model for **every** morph, including integrated mods.

Naturalis reads `mass` from mob profiles (or archetype / heuristic fallbacks), then applies speed, gravity, jump height, knockback, and fall damage.

## Mass sources (priority)

1. Profile / archetype `mass` (datapack)
2. Built-in path table (vanilla + Alex's Mobs seeds)
3. Integration heuristics (namespace + path keywords: leviathan, snail, elephant, …)
4. Entity hitbox volume estimate
5. Neutral fallback `2.2`

## Feel

| Mass | Speed | Jump | Gravity | Knockback |
|------|-------|------|---------|-----------|
| Light (~0.3–1) | Faster | Higher | Floatier | Weak resist |
| Neutral (~2.2–3) | Near player | Near player | Near player | Mild |
| Heavy (~6–13) | Much slower | Lower squat hop | Pulls down harder | Strong resist |

Jump height is applied on the jump event (motion scale), so it works even when the player has no `JUMP_STRENGTH` attribute. Gravity uses the `GRAVITY` attribute on 1.21+ and a synthetic airborne pull on Forge 1.20 / when the attribute is missing.

Species gait (`instinct.walk_speed`) stacks on top of mass speed (snails crawl; see walk-speed notes in the mob profiles doc).

## Practical examples

- `naturalist:elephant` (mass 12) — slow, heavy landings, short hops
- `naturalist:finch` / firefly — light and springy
- `cataclysm:the_leviathan` (mass 13) — very heavy inertia
- `aether:slider` (mass 10) — grounded boss feel
