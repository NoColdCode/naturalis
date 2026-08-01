#!/usr/bin/env python3
"""Generate complete Naturalis mob profiles for integration mods."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "common/src/main/resources/data/naturalis"
PROFILES = ROOT / "mob_profiles"
DEFAULTS = ROOT / "mob_namespace_defaults"


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=4) + "\n", encoding="utf-8")


def profile(ns: str, path: str, inherits: str, **kwargs) -> None:
    data = {"entity": f"{ns}:{path}", "inherits": inherits}
    data.update(kwargs)
    write(PROFILES / ns / f"{path}.json", data)


def hostile_profile(
    ns: str,
    path: str,
    arch: str,
    mass: float,
    diet: str,
    shader: str,
    *,
    mod_tag: str,
    boss: bool = False,
    flight: bool = False,
    aquatic: bool = False,
    nether: bool = False,
    ice: bool = False,
    static: bool = False,
    extra_tags: list[str] | None = None,
) -> None:
    mono = "undead" in shader or "void" in shader or "nether" in shader
    instinct: dict = {
        "wander": False if static else (not boss),
        "smell_strength": 2 if not boss else 3,
        "nyctalop_hostile": True,
    }
    if flight:
        instinct["flight_only"] = True
    if static:
        instinct["static"] = True
    if nether:
        instinct["fears"] = ["water"]
    env: dict = {}
    if nether:
        env["cold_vulnerable"] = True
        env["wet_vulnerable"] = True
    if ice:
        env["hot_vulnerable"] = True
    if aquatic or "aquatic" in arch or "abyssal" in shader:
        env["dry_vulnerable"] = True
    biome: dict = {}
    if nether:
        biome["volcanic"] = True
    if ice:
        biome["snow_adapted"] = True
    tags = [mod_tag] + (extra_tags or [])
    if boss:
        tags.append("boss")
    if static:
        tags.append("static")
    categories = ["hostile", "high_damage"] if boss else (["hostile", "aquatic"] if aquatic else ["hostile", "ground"])
    if flight and "aerial" not in categories:
        categories = ["aerial"] + categories
    profile(
        ns,
        path,
        arch,
        mass=mass,
        diet=diet,
        instinct=instinct,
        vision={
            "shader": f"naturalis:{shader}",
            "chromatic_mode": "mono" if mono else "dichrome",
            "motion_trail": 0.65 if boss else 0.5,
        },
        environment=env,
        biome_suitability=biome,
        quick_slot={
            "categories": categories,
            "primary": "high_damage" if boss else ("aerial" if flight else "hostile"),
        },
        tags=tags,
    )


def passive_profile(
    ns: str,
    path: str,
    arch: str,
    mass: float,
    diet: str,
    shader: str,
    *,
    mod_tag: str,
    flight: bool = False,
    aquatic: bool = False,
    walk_speed: float | None = None,
    extra_tags: list[str] | None = None,
) -> None:
    instinct: dict = {"wander": True, "smell_strength": 1}
    if flight:
        instinct["flight_only"] = True
    if walk_speed is not None:
        instinct["walk_speed"] = walk_speed
    env: dict = {"dry_vulnerable": True} if aquatic else {}
    profile(
        ns,
        path,
        arch,
        mass=mass,
        diet=diet,
        instinct=instinct,
        vision={"shader": f"naturalis:{shader}", "chromatic_mode": "quad" if flight else "dichrome"},
        environment=env,
        quick_slot={
            "categories": (["aerial"] if flight else (["aquatic"] if aquatic else ["ground"])),
            "primary": "aerial" if flight else ("aquatic" if aquatic else "ground"),
        },
        tags=[mod_tag] + (extra_tags or []),
    )


def main() -> None:
    # ---------- NATURALIST ----------
    ns = "naturalist"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:ground_herbivore",
            "overrides": {
                "bass": {"inherits": "naturalis:aquatic_predator", "diet": "piscivore", "mass": 0.8},
                "catfish": {"inherits": "naturalis:aquatic_predator", "diet": "piscivore", "mass": 1.2},
                "butterfly": {"inherits": "naturalis:insect_arthropod", "diet": "nectarivore", "mass": 0.2},
                "dragonfly": {"inherits": "naturalis:insect_arthropod", "diet": "insectivore", "mass": 0.25},
                "firefly": {"inherits": "naturalis:insect_arthropod", "diet": "nectarivore", "mass": 0.15},
                "caterpillar": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 0.2},
                "snail": {
                    "inherits": "naturalis:insect_arthropod",
                    "diet": "herbivore",
                    "mass": 0.35,
                    "instinct": {"walk_speed": 0.18, "wander": True, "smell_strength": 1},
                },
            },
        },
    )
    profile(
        ns, "alligator", "naturalis:aquatic_predator", mass=4.5, diet="carnivore",
        instinct={"wander": True, "smell_strength": 2, "hunt_prey": ["minecraft:chicken", "minecraft:rabbit", "naturalist:deer"]},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome", "motion_trail": 0.5},
        quick_slot={"categories": ["aquatic", "hostile"], "primary": "hostile"}, tags=["reptile", "predator"],
    )
    profile(
        ns, "bear", "naturalis:large_predator", mass=6.5, diet="omnivore",
        instinct={"wander": True, "smell_strength": 3, "nyctalop_hostile": True, "hunt_prey": ["minecraft:salmon", "naturalist:deer", "minecraft:rabbit"]},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome", "canine_predator": True, "motion_trail": 0.55},
        environment={"hot_vulnerable": True}, biome_suitability={"snow_adapted": True, "cave_adapted": True},
        quick_slot={"categories": ["ground", "hostile"], "primary": "hostile"}, tags=["predator", "mammal"],
    )
    profile(ns, "boar", "naturalis:ground_herbivore", mass=3.2, diet="omnivore",
            instinct={"wander": True, "smell_strength": 2, "fears": ["wolves"]},
            vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome"}, tags=["mammal"])
    profile(
        ns, "lion", "naturalis:large_predator", mass=5.8, diet="carnivore",
        instinct={"wander": True, "smell_strength": 3, "hunt_prey": ["naturalist:zebra", "naturalist:giraffe", "minecraft:cow", "naturalist:deer"]},
        environment={"cold_vulnerable": True},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome", "canine_predator": True, "motion_trail": 0.65},
        quick_slot={"categories": ["ground", "hostile"], "primary": "hostile"}, tags=["predator", "feline"],
    )
    profile(
        ns, "hippo", "naturalis:aquatic_predator", mass=8.5, diet="herbivore",
        instinct={"wander": True, "smell_strength": 1},
        environment={"cold_vulnerable": True, "dry_vulnerable": True},
        vision={"shader": "naturalis:aquatic_vision", "chromatic_mode": "dichrome"},
        quick_slot={"categories": ["aquatic", "hostile", "ground"], "primary": "hostile"}, tags=["mammal", "territorial"],
    )
    profile(
        ns, "rhino", "naturalis:large_predator", mass=9.0, diet="herbivore",
        instinct={"wander": True, "smell_strength": 1}, environment={"cold_vulnerable": True},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome"},
        quick_slot={"categories": ["ground", "hostile", "high_damage"], "primary": "hostile"}, tags=["mammal", "charge"],
    )
    profile(
        ns, "elephant", "naturalis:ground_herbivore", mass=12.0, diet="herbivore",
        instinct={"wander": True, "smell_strength": 2}, environment={"cold_vulnerable": True},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome", "motion_trail": 0.35},
        quick_slot={"categories": ["ground", "high_damage"], "primary": "ground"}, tags=["mammal", "megafauna"],
    )
    profile(
        ns, "coral_snake", "naturalis:small_predator", mass=0.7, diet="carnivore",
        instinct={"wander": True, "smell_strength": 2, "hunt_prey": ["minecraft:chicken", "minecraft:rabbit", "naturalist:lizard"]},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"},
        environment={"cold_vulnerable": True}, tags=["reptile", "venom"],
    )
    profile(
        ns, "rattlesnake", "naturalis:small_predator", mass=0.9, diet="carnivore",
        instinct={"wander": True, "smell_strength": 2, "hunt_prey": ["minecraft:rabbit", "minecraft:chicken"]},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"},
        environment={"cold_vulnerable": True, "wet_vulnerable": True}, tags=["reptile", "venom"],
    )
    profile(
        ns, "snake", "naturalis:small_predator", mass=0.8, diet="carnivore",
        instinct={"wander": True, "smell_strength": 2, "hunt_prey": ["minecraft:chicken", "minecraft:rabbit"]},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"}, tags=["reptile"],
    )
    profile(
        ns, "vulture", "naturalis:flying_predator", mass=1.6, diet="scavenger",
        instinct={"flight_only": True, "smell_strength": 3, "wander": True},
        vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad", "motion_trail": 0.7},
        environment={"cold_vulnerable": True},
        quick_slot={"categories": ["aerial"], "primary": "aerial"},
        tags=["bird", "scavenger"],
    )
    profile(
        ns, "deer", "naturalis:ground_herbivore", mass=2.8, diet="herbivore",
        instinct={"wander": True, "smell_strength": 2, "fears": ["wolves", "cats"]},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome"}, tags=["mammal", "prey"],
    )
    profile(ns, "giraffe", "naturalis:ground_herbivore", mass=7.5, diet="herbivore",
            instinct={"wander": True, "smell_strength": 1}, environment={"cold_vulnerable": True},
            vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome"}, tags=["mammal"])
    profile(ns, "zebra", "naturalis:ground_herbivore", mass=3.4, diet="herbivore",
            instinct={"wander": True, "smell_strength": 2, "fears": ["wolves"]},
            environment={"cold_vulnerable": True}, tags=["mammal"])
    profile(ns, "ostrich", "naturalis:ground_herbivore", mass=3.0, diet="omnivore",
            instinct={"wander": True, "smell_strength": 1},
            vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad"}, tags=["bird"])
    profile(ns, "duck", "naturalis:frugivore_bird", mass=0.7, diet="omnivore",
            instinct={"flight_only": True, "wander": True, "smell_strength": 1},
            vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad"},
            quick_slot={"categories": ["aerial", "aquatic"], "primary": "aerial"},
            tags=["bird", "waterfowl"])
    profile(ns, "tortoise", "naturalis:ground_herbivore", mass=2.2, diet="herbivore",
            instinct={"wander": True, "smell_strength": 1, "walk_speed": 0.32},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"},
            environment={"cold_vulnerable": True}, tags=["reptile"])
    profile(ns, "snail", "naturalis:insect_arthropod", mass=0.35, diet="herbivore",
            instinct={"wander": True, "smell_strength": 1, "walk_speed": 0.18},
            vision={"shader": "naturalis:insect_vision", "chromatic_mode": "dichrome"},
            tags=["insect", "slow"])
    profile(ns, "caterpillar", "naturalis:insect_arthropod", mass=0.2, diet="herbivore",
            instinct={"wander": True, "smell_strength": 1, "walk_speed": 0.28},
            vision={"shader": "naturalis:insect_vision", "chromatic_mode": "dichrome"},
            tags=["insect", "slow"])
    profile(ns, "lizard", "naturalis:insect_arthropod", mass=0.45, diet="insectivore",
            instinct={"wander": True, "smell_strength": 1},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"}, tags=["reptile"])
    for bird, mass in [("bluejay", 0.35), ("canary", 0.25), ("cardinal", 0.35), ("finch", 0.25), ("robin", 0.35), ("sparrow", 0.3)]:
        profile(
            ns, bird, "naturalis:frugivore_bird", mass=mass,
            diet="insectivore" if bird in ("sparrow", "bluejay") else "frugivore",
            instinct={"flight_only": True, "wander": True, "smell_strength": 1},
            vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad"},
            quick_slot={"categories": ["aerial"], "primary": "aerial"},
            tags=["bird", "ambient"],
        )
    for insect, mass, diet in [
        ("butterfly", 0.2, "nectarivore"),
        ("dragonfly", 0.25, "insectivore"),
        ("firefly", 0.15, "nectarivore"),
    ]:
        passive_profile(ns, insect, "naturalis:insect_arthropod", mass, diet, "insect_vision",
                        mod_tag="naturalist", flight=True, extra_tags=["insect", "ambient"])

    # ---------- AQUAMIRAE ----------
    ns = "aquamirae"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:aquatic_predator",
            "overrides": {
                "golden_moth": {"inherits": "naturalis:floral_fey", "mass": 0.2},
                "spinefish": {"inherits": "naturalis:aquatic_predator", "diet": "piscivore", "mass": 0.6},
            },
        },
    )
    profile(
        ns, "anglerfish", "naturalis:aquatic_predator", mass=2.4, diet="piscivore",
        instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True, "hunt_prey": ["minecraft:cod", "minecraft:salmon"]},
        vision={"shader": "naturalis:abyssal_vision", "chromatic_mode": "mono", "spectral_profile": 0.6},
        environment={"dry_vulnerable": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["aquatic", "hostile"], "primary": "hostile"}, tags=["abyss", "venom"],
    )
    profile(
        ns, "maw", "naturalis:aquatic_predator", mass=4.0, diet="carnivore",
        instinct={"wander": True, "smell_strength": 3, "nyctalop_hostile": True},
        vision={"shader": "naturalis:abyssal_vision", "chromatic_mode": "mono", "motion_trail": 0.7},
        environment={"dry_vulnerable": True, "sunlight_sensitive": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["aquatic", "hostile"], "primary": "hostile"}, tags=["abyss", "hunter"],
    )
    profile(
        ns, "tortured_soul", "naturalis:undead", mass=2.4, diet="necrovore",
        instinct={"wander": True, "nyctalop_hostile": True, "smell_strength": 2},
        vision={"shader": "naturalis:undead_vision", "chromatic_mode": "mono"},
        environment={"sunlight_sensitive": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["ground", "hostile"], "primary": "hostile"}, tags=["undead", "illager"],
    )
    profile(
        ns, "maze_mother", "naturalis:boss_entity", mass=7.5, diet="carnivore",
        instinct={"wander": False, "smell_strength": 2, "nyctalop_hostile": True},
        vision={"shader": "naturalis:abyssal_vision", "chromatic_mode": "mono"},
        environment={"dry_vulnerable": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["aquatic", "hostile", "high_damage"], "primary": "high_damage"}, tags=["boss", "abyss"],
    )
    profile(
        ns, "eel", "naturalis:boss_entity", mass=8.0, diet="carnivore",
        instinct={"wander": False, "smell_strength": 3, "nyctalop_hostile": True},
        vision={"shader": "naturalis:abyssal_vision", "chromatic_mode": "mono", "spectral_profile": 0.5},
        environment={"dry_vulnerable": True}, biome_suitability={"snow_adapted": True, "cave_adapted": True},
        quick_slot={"categories": ["aquatic", "hostile", "high_damage"], "primary": "high_damage"}, tags=["boss", "abyss"],
    )
    profile(
        ns, "captain_cornelia", "naturalis:boss_entity", mass=6.5, diet="necrovore",
        instinct={"wander": False, "smell_strength": 2, "nyctalop_hostile": True},
        vision={"shader": "naturalis:undead_vision", "chromatic_mode": "mono", "motion_trail": 0.55},
        environment={"sunlight_sensitive": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["ground", "hostile", "high_damage"], "primary": "high_damage"}, tags=["boss", "undead"],
    )
    profile(
        ns, "abyssal_scyphoid", "naturalis:aquatic_predator", mass=1.8, diet="carnivore",
        instinct={"wander": True, "smell_strength": 1, "nyctalop_hostile": True},
        vision={"shader": "naturalis:abyssal_vision", "chromatic_mode": "mono"},
        environment={"dry_vulnerable": True}, tags=["abyss", "jellyfish"],
    )

    # ---------- FRIENDS AND FOES ----------
    ns = "friendsandfoes"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:ground_herbivore",
            "overrides": {
                "glare": {"inherits": "naturalis:floral_fey", "mass": 0.8},
                "moobloom": {"inherits": "naturalis:ground_herbivore", "diet": "florivore", "mass": 3.0},
                "crab": {"inherits": "naturalis:insect_arthropod", "diet": "omnivore", "mass": 0.7},
                "rascal": {"inherits": "naturalis:small_predator", "diet": "omnivore", "mass": 1.2},
            },
        },
    )
    profile(
        ns, "iceologer", "naturalis:boss_entity", mass=3.2, diet="omnivore",
        instinct={"wander": True, "smell_strength": 1},
        vision={"shader": "naturalis:tempest_vision", "chromatic_mode": "dichrome", "spectral_profile": 0.4},
        environment={"hot_vulnerable": True}, biome_suitability={"snow_adapted": True},
        quick_slot={"categories": ["ground", "hostile", "high_damage"], "primary": "hostile"}, tags=["illager", "ice"],
    )
    profile(
        ns, "illusioner", "naturalis:small_predator", mass=2.8, diet="omnivore",
        instinct={"wander": True, "smell_strength": 1},
        vision={"shader": "naturalis:arcane_vision", "chromatic_mode": "quad", "spectral_profile": 0.5},
        quick_slot={"categories": ["ground", "hostile"], "primary": "hostile"}, tags=["illager", "ranged"],
    )
    profile(
        ns, "wildfire", "naturalis:nether_native", mass=4.5, diet="carnivore",
        instinct={"flight_only": True, "smell_strength": 2, "nyctalop_hostile": True, "fears": ["water"]},
        vision={"shader": "naturalis:nether_vision", "chromatic_mode": "dichrome", "motion_trail": 0.6},
        environment={"cold_vulnerable": True, "wet_vulnerable": True}, biome_suitability={"volcanic": True},
        quick_slot={"categories": ["aerial", "nether", "hostile", "high_damage"], "primary": "hostile"}, tags=["nether", "blaze"],
    )
    profile(
        ns, "mauler", "naturalis:small_predator", mass=2.0, diet="carnivore",
        instinct={"wander": True, "smell_strength": 3, "nyctalop_hostile": True, "hunt_prey": ["minecraft:chicken", "minecraft:rabbit", "minecraft:pig"]},
        vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome", "canine_predator": True},
        biome_suitability={"cave_adapted": True},
        quick_slot={"categories": ["ground", "hostile"], "primary": "hostile"}, tags=["predator", "ambush"],
    )
    profile(
        ns, "copper_golem", "naturalis:golem_construct", mass=4.0, diet="lithovore",
        instinct={"wander": True, "smell_strength": 0},
        vision={"shader": "naturalis:ferrous_vision", "chromatic_mode": "mono"},
        environment={"wet_vulnerable": True}, tags=["golem", "construct"],
    )
    profile(
        ns, "tuff_golem", "naturalis:golem_construct", mass=5.5, diet="lithovore",
        instinct={"wander": False, "smell_strength": 0},
        vision={"shader": "naturalis:ferrous_vision", "chromatic_mode": "mono"},
        biome_suitability={"cave_adapted": True}, tags=["golem", "construct"],
    )

    # ---------- CATACLYSM ----------
    ns = "cataclysm"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:large_predator",
            "overrides": {
                "the_baby_leviathan": {"inherits": "naturalis:aquatic_predator", "mass": 3.5},
                "hippocamtus": {"inherits": "naturalis:aquatic_predator", "mass": 3.0},
                "cindaria": {"inherits": "naturalis:aquatic_predator", "mass": 2.8},
                "urchinkin": {"inherits": "naturalis:insect_arthropod", "mass": 0.8},
            },
        },
    )
    bosses = [
        ("netherite_monstrosity", 14.0, "naturalis:nether_native", "nether_vision", True, False),
        ("ender_guardian", 12.0, "naturalis:boss_entity", "void_vision", False, True),
        ("the_harbinger", 11.0, "naturalis:boss_entity", "nether_vision", True, False),
        ("ancient_remnant", 12.5, "naturalis:boss_entity", "mammal_vision", False, False),
        ("the_leviathan", 13.0, "naturalis:serpent_aquatic", "abyssal_vision", False, False),
        ("scylla", 11.5, "naturalis:boss_entity", "tempest_vision", False, False),
        ("maledictus", 10.5, "naturalis:ethereal_spirit", "void_vision", False, False),
        ("ignis", 12.0, "naturalis:nether_native", "nether_vision", True, False),
    ]
    for path, mass, arch, shader, volcanic, ender in bosses:
        profile(
            ns, path, arch, mass=mass, diet="carnivore",
            instinct={"wander": False, "smell_strength": 3, "nyctalop_hostile": True},
            vision={"shader": f"naturalis:{shader}", "chromatic_mode": "dichrome" if shader != "void_vision" else "mono", "motion_trail": 0.7, "spectral_profile": 0.45},
            biome_suitability={"volcanic": volcanic, "ender_adapted": ender, "cave_adapted": True},
            quick_slot={"categories": ["hostile", "high_damage"], "primary": "high_damage"},
            resonance={"archetype": "apex"}, tags=["boss", "cataclysm"],
        )

    elites = [
        ("ender_golem", 8.5, "naturalis:golem_construct", "void_vision"),
        ("the_prowler", 7.0, "naturalis:golem_construct", "ferrous_vision"),
        ("the_watcher", 4.0, "naturalis:golem_construct", "ferrous_vision"),
        ("kobolediator", 6.5, "naturalis:undead", "undead_vision"),
        ("wadjet", 6.0, "naturalis:serpent_aquatic", "reptile_vision"),
        ("amethyst_crab", 5.5, "naturalis:insect_arthropod", "crystalline_vision"),
        ("clawdian", 6.0, "naturalis:insect_arthropod", "aquatic_vision"),
        ("aptrgangr", 6.5, "naturalis:undead", "undead_vision"),
        ("ignited_revenant", 5.5, "naturalis:nether_native", "nether_vision"),
        ("ignited_berserker", 5.0, "naturalis:nether_native", "nether_vision"),
        ("coralssus", 8.0, "naturalis:golem_construct", "aquatic_vision"),
        ("nameless_sorcerer", 3.5, "naturalis:boss_entity", "arcane_vision"),
    ]
    for path, mass, arch, shader in elites:
        profile(
            ns, path, arch, mass=mass, diet="carnivore" if "undead" not in arch and "golem" not in arch else ("necrovore" if "undead" in arch else "lithovore"),
            instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True},
            vision={"shader": f"naturalis:{shader}", "chromatic_mode": "dichrome", "motion_trail": 0.55},
            quick_slot={"categories": ["hostile", "high_damage"], "primary": "hostile"}, tags=["elite", "cataclysm"],
        )

    hostiles = [
        ("koboleton", 2.4, "naturalis:undead", "undead_vision", "necrovore"),
        ("deepling", 2.6, "naturalis:aquatic_predator", "abyssal_vision", "piscivore"),
        ("deepling_angler", 2.8, "naturalis:aquatic_predator", "abyssal_vision", "piscivore"),
        ("deepling_brute", 4.2, "naturalis:aquatic_predator", "abyssal_vision", "carnivore"),
        ("deepling_priest", 2.8, "naturalis:aquatic_predator", "abyssal_vision", "carnivore"),
        ("deepling_warlock", 2.9, "naturalis:aquatic_predator", "void_vision", "carnivore"),
        ("coral_golem", 5.5, "naturalis:golem_construct", "aquatic_vision", "lithovore"),
        ("lionfish", 1.4, "naturalis:aquatic_predator", "aquatic_vision", "piscivore"),
        ("endermaptera", 2.0, "naturalis:insect_arthropod", "void_vision", "insectivore"),
        ("draugr", 2.8, "naturalis:undead", "undead_vision", "necrovore"),
        ("elite_draugr", 3.6, "naturalis:undead", "undead_vision", "necrovore"),
        ("royal_draugr", 4.4, "naturalis:undead", "undead_vision", "necrovore"),
        ("symbiocto", 3.2, "naturalis:aquatic_predator", "cephalopod_vision", "carnivore"),
        ("modern_remnant", 5.0, "naturalis:boss_entity", "mammal_vision", "carnivore"),
        ("netherite_ministrosity", 4.5, "naturalis:nether_native", "nether_vision", "carnivore"),
    ]
    for path, mass, arch, shader, diet in hostiles:
        profile(
            ns, path, arch, mass=mass, diet=diet,
            instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True},
            vision={"shader": f"naturalis:{shader}", "chromatic_mode": "dichrome" if "void" not in shader and "undead" not in shader else "mono"},
            environment={"dry_vulnerable": "aquatic" in arch or "abyssal" in shader or "cephalopod" in shader},
            quick_slot={"categories": ["hostile", "aquatic"] if "aquatic" in arch or "abyssal" in shader else ["hostile", "ground"], "primary": "hostile"},
            tags=["cataclysm"],
        )

    # ---------- ICE AND FIRE (enrich existing + CE extras) ----------
    ns = "iceandfire"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:large_predator",
            "overrides": {
                "dragon_skull": {"inherits": "naturalis:undead", "mass": 3.0},
                "dread_ghoul": {"inherits": "naturalis:undead", "mass": 2.6},
                "dread_knight": {"inherits": "naturalis:undead", "mass": 3.2},
                "dread_thrall": {"inherits": "naturalis:undead", "mass": 2.4},
                "dread_scuttler": {"inherits": "naturalis:insect_arthropod", "mass": 1.8},
                "dread_lich": {"inherits": "naturalis:undead", "mass": 3.5},
                "dread_horse": {"inherits": "naturalis:undead", "mass": 3.8},
                "hippocampus": {"inherits": "naturalis:aquatic_predator", "diet": "herbivore", "mass": 3.2},
            },
        },
    )
    # Enrich dragons
    profile(
        ns, "fire_dragon", "naturalis:draconic", mass=11.0, diet="carnivore",
        instinct={"flight_only": True, "smell_strength": 3, "fears": ["water"], "hunt_prey": ["minecraft:cow", "minecraft:sheep", "minecraft:villager"]},
        environment={"cold_vulnerable": True}, biome_suitability={"volcanic": True},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome", "motion_trail": 0.75},
        quick_slot={"categories": ["aerial", "hostile", "high_damage"], "primary": "high_damage"}, tags=["dragon", "boss", "fire"],
    )
    profile(
        ns, "ice_dragon", "naturalis:draconic", mass=11.0, diet="carnivore",
        instinct={"flight_only": True, "smell_strength": 3, "hunt_prey": ["minecraft:cow", "minecraft:sheep", "minecraft:villager"]},
        environment={"hot_vulnerable": True}, biome_suitability={"snow_adapted": True},
        vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome", "motion_trail": 0.75},
        quick_slot={"categories": ["aerial", "hostile", "high_damage"], "primary": "high_damage"}, tags=["dragon", "boss", "ice"],
    )
    profile(
        ns, "lightning_dragon", "naturalis:draconic", mass=11.0, diet="carnivore",
        instinct={"flight_only": True, "smell_strength": 3, "hunt_prey": ["minecraft:cow", "minecraft:sheep", "minecraft:villager"]},
        vision={"shader": "naturalis:tempest_vision", "chromatic_mode": "dichrome", "motion_trail": 0.8, "spectral_profile": 0.4},
        biome_suitability={"ender_adapted": True},
        quick_slot={"categories": ["aerial", "hostile", "high_damage"], "primary": "high_damage"}, tags=["dragon", "boss", "lightning"],
    )
    profile(
        ns, "black_frost_dragon", "naturalis:draconic", mass=12.5, diet="carnivore",
        instinct={"flight_only": True, "smell_strength": 3, "nyctalop_hostile": True},
        environment={"hot_vulnerable": True}, biome_suitability={"snow_adapted": True},
        vision={"shader": "naturalis:void_vision", "chromatic_mode": "mono", "motion_trail": 0.8, "spectral_profile": 0.6},
        quick_slot={"categories": ["aerial", "hostile", "high_damage"], "primary": "high_damage"}, tags=["dragon", "boss", "frost"],
    )
    profile(ns, "amphithere", "naturalis:draconic", mass=6.0, diet="carnivore",
            instinct={"flight_only": True, "smell_strength": 2},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"},
            quick_slot={"categories": ["aerial", "hostile"], "primary": "aerial"}, tags=["dragon"])
    profile(ns, "hippogryph", "naturalis:flying_predator", mass=3.6, diet="carnivore",
            instinct={"flight_only": True, "smell_strength": 2, "hunt_prey": ["minecraft:rabbit", "minecraft:chicken"]},
            vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad"}, tags=["flyer", "mount"])
    profile(ns, "cockatrice", "naturalis:flying_predator", mass=2.2, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome", "spectral_profile": 0.35}, tags=["gaze"])
    profile(ns, "cyclops", "naturalis:boss_entity", mass=8.5, diet="carnivore",
            instinct={"wander": True, "smell_strength": 1},
            vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "mono"},
            quick_slot={"categories": ["ground", "hostile", "high_damage"], "primary": "high_damage"}, tags=["giant", "boss"])
    profile(ns, "gorgon", "naturalis:boss_entity", mass=5.5, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2},
            vision={"shader": "naturalis:arcane_vision", "chromatic_mode": "dichrome", "spectral_profile": 0.5},
            environment={"sunlight_sensitive": False}, tags=["boss", "gaze"])
    profile(ns, "hydra", "naturalis:boss_entity", mass=10.0, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"},
            environment={"cold_vulnerable": True}, tags=["boss", "venom"])
    profile(ns, "deathworm", "naturalis:serpent_aquatic", mass=7.0, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2},
            environment={"dry_vulnerable": False, "wet_vulnerable": False},
            vision={"shader": "naturalis:reptile_vision", "chromatic_mode": "dichrome"}, tags=["burrower"])
    profile(ns, "sea_serpent", "naturalis:serpent_aquatic", mass=8.0, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2},
            vision={"shader": "naturalis:aquatic_vision", "chromatic_mode": "dichrome"},
            environment={"dry_vulnerable": True}, tags=["aquatic", "serpent"])
    profile(ns, "siren", "naturalis:serpent_aquatic", mass=3.0, diet="hematophage",
            instinct={"wander": True, "smell_strength": 2},
            vision={"shader": "naturalis:aquatic_vision", "chromatic_mode": "dichrome", "spectral_profile": 0.35},
            environment={"dry_vulnerable": True}, tags=["aquatic", "charm"])
    profile(ns, "stymphalian_bird", "naturalis:flying_predator", mass=1.5, diet="carnivore",
            instinct={"flight_only": True, "smell_strength": 2},
            vision={"shader": "naturalis:avian_vision", "chromatic_mode": "quad"}, tags=["bird", "ranged"])
    profile(ns, "troll", "naturalis:large_predator", mass=6.5, diet="carnivore",
            instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True},
            environment={"sunlight_sensitive": True}, biome_suitability={"cave_adapted": True},
            vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome"}, tags=["cave"])
    profile(ns, "pixie", "naturalis:floral_fey", mass=0.4, diet="florivore",
            instinct={"flight_only": True, "wander": True},
            vision={"shader": "naturalis:fae_vision", "chromatic_mode": "quad", "kaleido_strength": 0.6}, tags=["fey"])
    profile(ns, "ghost", "naturalis:ethereal_spirit", mass=1.2, diet="necrovore",
            instinct={"flight_only": True, "nyctalop_hostile": True},
            vision={"shader": "naturalis:void_vision", "chromatic_mode": "mono"}, tags=["undead", "spirit"])
    profile(ns, "dread_beast", "naturalis:undead", mass=4.0, diet="necrovore",
            instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True},
            vision={"shader": "naturalis:undead_vision", "chromatic_mode": "mono", "canine_predator": True}, tags=["dread"])
    profile(ns, "dread_queen", "naturalis:boss_entity", mass=7.0, diet="necrovore",
            instinct={"wander": False, "smell_strength": 3, "nyctalop_hostile": True},
            vision={"shader": "naturalis:void_vision", "chromatic_mode": "mono", "spectral_profile": 0.7},
            quick_slot={"categories": ["hostile", "high_damage"], "primary": "high_damage"}, tags=["dread", "boss"],
    )

    # ---------- BORN IN CHAOS (expand beyond existing) ----------
    for ns in ("born_in_chaos_v1", "born_in_chaos"):
        write(
            DEFAULTS / f"{ns}.json",
            {
                "namespace": ns,
                "default_inherits": "naturalis:undead",
                "overrides": {
                    "maggot": {"inherits": "naturalis:insect_arthropod", "mass": 0.2},
                    "diamond_termite": {"inherits": "naturalis:insect_arthropod", "mass": 0.9},
                    "corpse_fish": {"inherits": "naturalis:aquatic_predator", "diet": "scavenger", "mass": 1.2},
                    "felsteed": {"inherits": "naturalis:ethereal_spirit", "mass": 3.5},
                    "lords_felsteed": {"inherits": "naturalis:ethereal_spirit", "mass": 4.0},
                },
            },
        )
        extras = [
            ("skeleton_demoman", "naturalis:undead", 2.4, "necrovore", "undead_vision"),
            ("supreme_bonescaller", "naturalis:boss_entity", 4.5, "necrovore", "arcane_vision"),
            ("barrel_zombie", "naturalis:scavenger_undead", 2.6, "scavenger", "undead_vision"),
            ("zombie_bruiser", "naturalis:scavenger_undead", 3.8, "scavenger", "undead_vision"),
            ("zombie_fisherman", "naturalis:scavenger_undead", 2.5, "scavenger", "undead_vision"),
            ("zombie_lumberjack", "naturalis:scavenger_undead", 2.8, "scavenger", "undead_vision"),
            ("fallen_chaos_knight", "naturalis:undead", 4.2, "necrovore", "undead_vision"),
            ("missionary_raider", "naturalis:boss_entity", 4.0, "carnivore", "mammal_vision"),
            ("lifestealer", "naturalis:large_predator", 4.5, "hematophage", "void_vision"),
            ("lifestealer_true_form", "naturalis:boss_entity", 6.0, "hematophage", "void_vision"),
            ("baby_spider", "naturalis:insect_arthropod", 0.6, "insectivore", "insect_vision"),
            ("swarmer", "naturalis:insect_arthropod", 0.8, "insectivore", "insect_vision"),
            ("bloody_gadfly", "naturalis:flying_predator", 0.7, "hematophage", "insect_vision"),
            ("corpse_fly", "naturalis:flying_predator", 0.6, "scavenger", "insect_vision"),
            ("dread_hound", "naturalis:large_predator", 3.2, "carnivore", "mammal_vision"),
            ("dire_hound_leader", "naturalis:large_predator", 4.2, "carnivore", "mammal_vision"),
            ("bone_imp", "naturalis:ethereal_spirit", 1.0, "necrovore", "void_vision"),
            ("seared_spirit", "naturalis:nether_native", 1.4, "necrovore", "nether_vision"),
            ("firelight", "naturalis:nether_native", 0.8, "necrovore", "nether_vision"),
            ("spiritof_chaos", "naturalis:ethereal_spirit", 2.0, "necrovore", "void_vision"),
            ("scarlet_persecutor", "naturalis:ethereal_spirit", 2.2, "hematophage", "void_vision"),
            ("phantom_creeper", "naturalis:ethereal_spirit", 2.0, "necrovore", "void_vision"),
            ("mrs_pumpkin", "naturalis:scavenger_undead", 2.4, "scavenger", "undead_vision"),
            ("senor_pumpkin", "naturalis:scavenger_undead", 2.4, "scavenger", "undead_vision"),
            ("pumpkin_dunce", "naturalis:scavenger_undead", 2.0, "scavenger", "undead_vision"),
            ("pumpkin_bruiser", "naturalis:scavenger_undead", 3.5, "scavenger", "undead_vision"),
            ("pumpkinhead", "naturalis:scavenger_undead", 2.8, "scavenger", "undead_vision"),
            ("sir_pumpkinhead", "naturalis:boss_entity", 5.0, "scavenger", "undead_vision"),
            ("sir_the_headless", "naturalis:boss_entity", 5.0, "scavenger", "undead_vision"),
            ("lord_the_headless", "naturalis:boss_entity", 6.5, "scavenger", "undead_vision"),
            ("krampus", "naturalis:boss_entity", 6.0, "carnivore", "mammal_vision"),
            ("krampus_henchman", "naturalis:large_predator", 3.5, "carnivore", "mammal_vision"),
            ("bonescaller", "naturalis:undead", 2.6, "necrovore", "arcane_vision"),
            ("decaying_zombie", "naturalis:scavenger_undead", 2.3, "scavenger", "undead_vision"),
            ("decrepit_skeleton", "naturalis:undead", 1.9, "necrovore", "undead_vision"),
            ("mr_pumpkin", "naturalis:scavenger_undead", 2.2, "scavenger", "undead_vision"),
            ("pumpkin_spirit", "naturalis:ethereal_spirit", 1.5, "necrovore", "void_vision"),
            ("restless_spirit", "naturalis:ethereal_spirit", 1.4, "necrovore", "void_vision"),
            ("spirit_guide", "naturalis:ethereal_spirit", 1.6, "necrovore", "void_vision"),
            ("zombie_clown", "naturalis:scavenger_undead", 2.4, "scavenger", "undead_vision"),
        ]
        for path, arch, mass, diet, shader in extras:
            profile(
                ns, path, arch, mass=mass, diet=diet,
                instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True, "flight_only": "flying" in arch or "ethereal" in arch or path.endswith("_fly") or path.endswith("gadfly") or path == "bone_imp" or path == "firelight"},
                vision={"shader": f"naturalis:{shader}", "chromatic_mode": "mono" if "undead" in shader or "void" in shader else "dichrome", "motion_trail": 0.5},
                environment={"sunlight_sensitive": True} if "undead" in arch or "ethereal" in arch or "scavenger" in arch else {},
                quick_slot={"categories": ["hostile", "high_damage"] if "boss" in arch else ["hostile", "ground"], "primary": "hostile"},
                tags=["born_in_chaos"],
            )
        # Enrich existing key profiles
        profile(ns, "nightmare_stalker", "naturalis:large_predator", mass=4.8, diet="carnivore",
                instinct={"wander": True, "smell_strength": 3, "nyctalop_hostile": True},
                vision={"shader": "naturalis:mammal_vision", "chromatic_mode": "dichrome", "canine_predator": True, "motion_trail": 0.7},
                environment={"sunlight_sensitive": True}, quick_slot={"categories": ["hostile", "ground"], "primary": "hostile"}, tags=["born_in_chaos", "stalker"])
        profile(ns, "lord_pumpkinhead", "naturalis:boss_entity", mass=6.5, diet="scavenger",
                instinct={"wander": False, "smell_strength": 2, "nyctalop_hostile": True},
                vision={"shader": "naturalis:undead_vision", "chromatic_mode": "mono", "spectral_profile": 0.4},
                quick_slot={"categories": ["hostile", "high_damage"], "primary": "high_damage"}, tags=["born_in_chaos", "boss"])
        profile(ns, "mother_spider", "naturalis:insect_arthropod", mass=4.0, diet="carnivore",
                instinct={"wander": True, "smell_strength": 2, "nyctalop_hostile": True},
                vision={"shader": "naturalis:insect_vision", "chromatic_mode": "quad", "kaleido_strength": 0.4},
                quick_slot={"categories": ["hostile", "high_damage"], "primary": "high_damage"}, tags=["born_in_chaos", "spider"])
        profile(ns, "glutton_fish", "naturalis:aquatic_predator", mass=2.2, diet="carnivore",
                instinct={"wander": True, "smell_strength": 2},
                vision={"shader": "naturalis:aquatic_vision", "chromatic_mode": "dichrome"},
                environment={"dry_vulnerable": True}, tags=["born_in_chaos", "aquatic"])
        profile(ns, "infernal_spirit", "naturalis:nether_native", mass=1.6, diet="necrovore",
                instinct={"flight_only": True, "nyctalop_hostile": True, "smell_strength": 2, "fears": ["water"]},
                vision={"shader": "naturalis:nether_vision", "chromatic_mode": "dichrome"}, tags=["born_in_chaos", "spirit"])

    # ---------- CRITTERS AND COMPANIONS ----------
    ns = "crittersandcompanions"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:ground_herbivore",
            "overrides": {
                "dragonfly": {"inherits": "naturalis:insect_arthropod", "mass": 0.25},
                "dumbo_octopus": {"inherits": "naturalis:aquatic_predator", "diet": "piscivore", "mass": 1.0},
                "jumping_spider": {"inherits": "naturalis:insect_arthropod", "diet": "insectivore", "mass": 0.5},
                "koi_fish": {"inherits": "naturalis:aquatic_predator", "diet": "herbivore", "mass": 0.7},
                "ladybug": {"inherits": "naturalis:insect_arthropod", "mass": 0.15},
                "leaf_insect": {"inherits": "naturalis:insect_arthropod", "mass": 0.2},
                "sea_bunny": {"inherits": "naturalis:aquatic_predator", "diet": "herbivore", "mass": 0.4},
                "shima_enaga": {"inherits": "naturalis:frugivore_bird", "mass": 0.3},
                "snail": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 0.35},
                "stag_beetle": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 0.55},
                "stick_bug": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 0.25},
                "weevil": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 0.2},
            },
        },
    )
    passive_profile(ns, "ferret", "naturalis:small_predator", 0.9, "carnivore", "mammal_vision",
                    mod_tag="crittersandcompanions", extra_tags=["mammal", "pet"])
    passive_profile(ns, "otter", "naturalis:aquatic_predator", 1.4, "piscivore", "aquatic_vision",
                    mod_tag="crittersandcompanions", aquatic=True, extra_tags=["mammal", "pet"])
    passive_profile(ns, "red_panda", "naturalis:ground_herbivore", 2.1, "frugivore", "mammal_vision",
                    mod_tag="crittersandcompanions", extra_tags=["mammal", "arboreal"])
    passive_profile(ns, "roly_poly", "naturalis:insect_arthropod", 0.4, "herbivore", "insect_vision",
                    mod_tag="crittersandcompanions", walk_speed=0.38, extra_tags=["arthropod"])
    passive_profile(ns, "dragonfly", "naturalis:insect_arthropod", 0.25, "insectivore", "insect_vision",
                    mod_tag="crittersandcompanions", flight=True, extra_tags=["insect"])
    passive_profile(ns, "dumbo_octopus", "naturalis:aquatic_predator", 1.0, "piscivore", "cephalopod_vision",
                    mod_tag="crittersandcompanions", aquatic=True, extra_tags=["cephalopod"])
    passive_profile(ns, "jumping_spider", "naturalis:insect_arthropod", 0.5, "insectivore", "insect_vision",
                    mod_tag="crittersandcompanions", extra_tags=["spider"])
    passive_profile(ns, "koi_fish", "naturalis:aquatic_predator", 0.7, "herbivore", "aquatic_vision",
                    mod_tag="crittersandcompanions", aquatic=True, extra_tags=["fish"])
    for bug, mass in [("ladybug", 0.15), ("leaf_insect", 0.2), ("stag_beetle", 0.55),
                      ("stick_bug", 0.25), ("weevil", 0.2)]:
        passive_profile(ns, bug, "naturalis:insect_arthropod", mass, "herbivore", "insect_vision",
                        mod_tag="crittersandcompanions", extra_tags=["insect"])
    passive_profile(ns, "snail", "naturalis:insect_arthropod", 0.35, "herbivore", "insect_vision",
                    mod_tag="crittersandcompanions", walk_speed=0.18, extra_tags=["insect", "slow"])
    passive_profile(ns, "sea_bunny", "naturalis:aquatic_predator", 0.4, "herbivore", "aquatic_vision",
                    mod_tag="crittersandcompanions", aquatic=True, walk_speed=0.22, extra_tags=["mollusk"])
    passive_profile(ns, "shima_enaga", "naturalis:frugivore_bird", 0.3, "insectivore", "avian_vision",
                    mod_tag="crittersandcompanions", flight=True, extra_tags=["bird"])

    # ---------- TWILIGHT FOREST ----------
    ns = "twilightforest"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:large_predator",
            "overrides": {
                "bighorn_sheep": {"inherits": "naturalis:ground_herbivore", "diet": "herbivore", "mass": 2.8},
                "boar": {"inherits": "naturalis:ground_herbivore", "diet": "omnivore", "mass": 3.0},
                "deer": {"inherits": "naturalis:ground_herbivore", "diet": "herbivore", "mass": 2.6},
                "dwarf_rabbit": {"inherits": "naturalis:ground_herbivore", "diet": "herbivore", "mass": 0.5},
                "penguin": {"inherits": "naturalis:ground_herbivore", "diet": "piscivore", "mass": 1.2},
                "quest_ram": {"inherits": "naturalis:ground_herbivore", "diet": "herbivore", "mass": 4.0},
                "raven": {"inherits": "naturalis:frugivore_bird", "mass": 0.5},
                "squirrel": {"inherits": "naturalis:ground_herbivore", "diet": "frugivore", "mass": 0.4},
                "tiny_bird": {"inherits": "naturalis:frugivore_bird", "mass": 0.25},
            },
        },
    )
    # Passives
    for path, arch, mass, diet, shader, kwargs in [
        ("bighorn_sheep", "naturalis:ground_herbivore", 2.8, "herbivore", "mammal_vision", {"extra_tags": ["mammal"]}),
        ("boar", "naturalis:ground_herbivore", 3.0, "omnivore", "mammal_vision", {"extra_tags": ["mammal"]}),
        ("deer", "naturalis:ground_herbivore", 2.6, "herbivore", "mammal_vision", {"extra_tags": ["mammal", "prey"]}),
        ("dwarf_rabbit", "naturalis:ground_herbivore", 0.5, "herbivore", "mammal_vision", {"extra_tags": ["mammal"]}),
        ("penguin", "naturalis:ground_herbivore", 1.2, "piscivore", "avian_vision", {"extra_tags": ["bird", "ice"]}),
        ("quest_ram", "naturalis:ground_herbivore", 4.0, "herbivore", "mammal_vision", {"extra_tags": ["mammal", "quest"]}),
        ("raven", "naturalis:frugivore_bird", 0.5, "omnivore", "avian_vision", {"flight": True, "extra_tags": ["bird"]}),
        ("squirrel", "naturalis:ground_herbivore", 0.4, "frugivore", "mammal_vision", {"extra_tags": ["mammal"]}),
        ("tiny_bird", "naturalis:frugivore_bird", 0.25, "insectivore", "avian_vision", {"flight": True, "extra_tags": ["bird"]}),
    ]:
        passive_profile(ns, path, arch, mass, diet, shader, mod_tag="twilightforest", **kwargs)
    # Bosses
    for path, mass, arch, shader, kwargs in [
        ("naga", 10.0, "naturalis:serpent_aquatic", "reptile_vision", {}),
        ("lich", 7.0, "naturalis:undead", "undead_vision", {}),
        ("minoshroom", 8.5, "naturalis:boss_entity", "mammal_vision", {}),
        ("hydra", 14.0, "naturalis:boss_entity", "reptile_vision", {"nether": True}),
        ("knight_phantom", 6.0, "naturalis:ethereal_spirit", "void_vision", {"flight": True}),
        ("ur_ghast", 13.0, "naturalis:boss_entity", "nether_vision", {"flight": True, "nether": True}),
        ("alpha_yeti", 11.0, "naturalis:boss_entity", "mammal_vision", {"ice": True}),
        ("snow_queen", 8.0, "naturalis:boss_entity", "tempest_vision", {"ice": True, "flight": True}),
        ("plateau_boss", 12.0, "naturalis:boss_entity", "void_vision", {}),
    ]:
        hostile_profile(ns, path, arch, mass, "carnivore", shader, mod_tag="twilightforest", boss=True, **kwargs)
    # Hostiles
    for path, arch, mass, diet, shader, kwargs in [
        ("adherent", "naturalis:ethereal_spirit", 2.5, "necrovore", "void_vision", {"flight": True}),
        ("armored_giant", "naturalis:large_predator", 9.0, "carnivore", "mammal_vision", {}),
        ("block_and_chain_goblin", "naturalis:small_predator", 2.2, "omnivore", "mammal_vision", {}),
        ("carminite_broodling", "naturalis:insect_arthropod", 0.8, "carnivore", "insect_vision", {}),
        ("carminite_ghastguard", "naturalis:flying_predator", 5.5, "carnivore", "nether_vision", {"flight": True, "nether": True}),
        ("carminite_ghastling", "naturalis:flying_predator", 2.0, "carnivore", "nether_vision", {"flight": True, "nether": True}),
        ("carminite_golem", "naturalis:golem_construct", 5.0, "lithovore", "ferrous_vision", {}),
        ("death_tome", "naturalis:ethereal_spirit", 1.5, "necrovore", "arcane_vision", {"flight": True}),
        ("fire_beetle", "naturalis:insect_arthropod", 1.8, "carnivore", "insect_vision", {"nether": True}),
        ("giant_miner", "naturalis:large_predator", 9.0, "omnivore", "mammal_vision", {}),
        ("harbinger_cube", "naturalis:golem_construct", 4.5, "lithovore", "void_vision", {"nether": True}),
        ("hedge_spider", "naturalis:insect_arthropod", 1.6, "carnivore", "insect_vision", {}),
        ("helmet_crab", "naturalis:insect_arthropod", 1.4, "carnivore", "insect_vision", {}),
        ("hostile_wolf", "naturalis:large_predator", 2.4, "carnivore", "mammal_vision", {}),
        ("ice_crystal", "naturalis:ethereal_spirit", 1.8, "necrovore", "tempest_vision", {"ice": True, "flight": True}),
        ("king_spider", "naturalis:insect_arthropod", 3.5, "carnivore", "insect_vision", {}),
        ("kobold", "naturalis:small_predator", 1.5, "omnivore", "mammal_vision", {}),
        ("lich_minion", "naturalis:undead", 2.2, "necrovore", "undead_vision", {}),
        ("lower_goblin_knight", "naturalis:small_predator", 2.0, "omnivore", "mammal_vision", {}),
        ("loyal_zombie", "naturalis:undead", 2.4, "necrovore", "undead_vision", {}),
        ("maze_slime", "naturalis:insect_arthropod", 1.2, "omnivore", "insect_vision", {}),
        ("minotaur", "naturalis:large_predator", 4.5, "carnivore", "mammal_vision", {}),
        ("mist_wolf", "naturalis:large_predator", 3.5, "carnivore", "mammal_vision", {}),
        ("mosquito_swarm", "naturalis:insect_arthropod", 1.0, "hematophage", "insect_vision", {"flight": True}),
        ("pinch_beetle", "naturalis:insect_arthropod", 1.6, "carnivore", "insect_vision", {}),
        ("redcap", "naturalis:small_predator", 1.8, "omnivore", "mammal_vision", {}),
        ("redcap_sapper", "naturalis:small_predator", 1.8, "omnivore", "mammal_vision", {}),
        ("rising_zombie", "naturalis:undead", 2.4, "necrovore", "undead_vision", {}),
        ("roving_cube", "naturalis:golem_construct", 3.5, "lithovore", "void_vision", {}),
        ("skeleton_druid", "naturalis:undead", 2.6, "necrovore", "undead_vision", {}),
        ("slime_beetle", "naturalis:insect_arthropod", 1.4, "carnivore", "insect_vision", {}),
        ("snow_guardian", "naturalis:ethereal_spirit", 2.2, "necrovore", "tempest_vision", {"ice": True}),
        ("stable_ice_core", "naturalis:ethereal_spirit", 2.4, "necrovore", "tempest_vision", {"ice": True}),
        ("swarm_spider", "naturalis:insect_arthropod", 0.7, "carnivore", "insect_vision", {}),
        ("towerwood_borer", "naturalis:insect_arthropod", 0.5, "herbivore", "insect_vision", {}),
        ("troll", "naturalis:large_predator", 5.5, "carnivore", "mammal_vision", {}),
        ("unstable_ice_core", "naturalis:ethereal_spirit", 2.4, "necrovore", "tempest_vision", {"ice": True}),
        ("upper_goblin_knight", "naturalis:small_predator", 2.4, "omnivore", "mammal_vision", {}),
        ("winter_wolf", "naturalis:large_predator", 3.5, "carnivore", "mammal_vision", {"ice": True}),
        ("wraith", "naturalis:ethereal_spirit", 2.0, "necrovore", "void_vision", {"flight": True}),
        ("yeti", "naturalis:large_predator", 5.0, "carnivore", "mammal_vision", {"ice": True}),
    ]:
        hostile_profile(ns, path, arch, mass, diet, shader, mod_tag="twilightforest", **kwargs)

    # ---------- GOETY ----------
    ns = "goety"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:undead",
            "overrides": {
                "carrion_fly": {"inherits": "naturalis:insect_arthropod", "mass": 0.4},
                "carrion_maggot": {"inherits": "naturalis:insect_arthropod", "mass": 0.25},
                "twilight_goat": {"inherits": "naturalis:ground_herbivore", "diet": "herbivore", "mass": 2.8},
                "prisoner": {"inherits": "naturalis:ground_herbivore", "diet": "omnivore", "mass": 2.4},
                "poison_anemone": {"inherits": "naturalis:aquatic_predator", "mass": 1.2},
                "poison_quill_vine": {"inherits": "naturalis:insect_arthropod", "diet": "carnivore", "mass": 1.5},
                "quick_growing_kelp": {"inherits": "naturalis:aquatic_predator", "diet": "herbivore", "mass": 1.0},
                "quick_growing_vine": {"inherits": "naturalis:insect_arthropod", "diet": "herbivore", "mass": 1.0},
            },
        },
    )
    goety_bosses = [
        ("apostle", 10.0, "naturalis:boss_entity", "nether_vision", {"nether": True}),
        ("vizier", 8.5, "naturalis:boss_entity", "arcane_vision", {}),
        ("heresiarch", 9.0, "naturalis:boss_entity", "void_vision", {}),
        ("skull_lord", 7.5, "naturalis:undead", "undead_vision", {}),
        ("ender_keeper", 9.5, "naturalis:boss_entity", "void_vision", {}),
        ("malghast", 8.0, "naturalis:boss_entity", "nether_vision", {"flight": True, "nether": True}),
        ("grave_golem", 9.0, "naturalis:golem_construct", "ferrous_vision", {}),
        ("redstone_monstrosity", 12.0, "naturalis:golem_construct", "ferrous_vision", {}),
        ("hostile_redstone_monstrosity", 12.0, "naturalis:golem_construct", "ferrous_vision", {}),
        ("black_beast", 7.0, "naturalis:large_predator", "mammal_vision", {}),
        ("brood_mother", 6.5, "naturalis:insect_arthropod", "insect_vision", {}),
        ("endersent", 7.0, "naturalis:boss_entity", "void_vision", {}),
        ("obsidian_monolith", 8.0, "naturalis:golem_construct", "void_vision", {}),
        ("squall_golem", 7.5, "naturalis:golem_construct", "tempest_vision", {}),
        ("ice_golem", 6.5, "naturalis:golem_construct", "tempest_vision", {"ice": True}),
    ]
    for path, mass, arch, shader, kwargs in goety_bosses:
        hostile_profile(ns, path, arch, mass, "necrovore" if "undead" in arch or "ethereal" in arch else "carnivore",
                        shader, mod_tag="goety", boss=True, **kwargs)

    goety_hostiles = [
        # Illagers / casters
        ("conquillager", 3.0, "naturalis:small_predator", "mammal_vision", "omnivore", {}),
        ("crone", 2.8, "naturalis:small_predator", "arcane_vision", "omnivore", {}),
        ("cryologer", 3.0, "naturalis:small_predator", "tempest_vision", "omnivore", {"ice": True}),
        ("envioker", 3.2, "naturalis:small_predator", "arcane_vision", "omnivore", {}),
        ("heretic", 2.8, "naturalis:small_predator", "nether_vision", "omnivore", {"nether": True}),
        ("inquillager", 3.0, "naturalis:small_predator", "mammal_vision", "omnivore", {}),
        ("minister", 2.8, "naturalis:small_predator", "arcane_vision", "omnivore", {}),
        ("neollager", 2.6, "naturalis:small_predator", "mammal_vision", "omnivore", {}),
        ("preacher", 2.8, "naturalis:small_predator", "arcane_vision", "omnivore", {}),
        ("sorcerer", 2.8, "naturalis:small_predator", "arcane_vision", "omnivore", {}),
        ("storm_caster", 3.0, "naturalis:small_predator", "tempest_vision", "omnivore", {}),
        ("tormentor", 3.2, "naturalis:small_predator", "void_vision", "omnivore", {}),
        ("warlock", 2.8, "naturalis:small_predator", "nether_vision", "omnivore", {"nether": True}),
        ("maverick", 2.6, "naturalis:small_predator", "mammal_vision", "omnivore", {}),
        ("piker", 2.6, "naturalis:small_predator", "mammal_vision", "omnivore", {}),
        ("crusher", 3.5, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        ("trampler", 4.0, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        ("ravaged", 5.5, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        # Undead / spirits
        ("wraith", 2.0, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("border_wraith", 2.2, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("muck_wraith", 2.2, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("haunt", 1.8, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("haunted_armor", 3.5, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("haunted_skull", 1.2, "naturalis:undead", "undead_vision", "necrovore", {"flight": True}),
        ("reaper", 2.8, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("wight", 2.6, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("necromancer", 3.2, "naturalis:undead", "arcane_vision", "necrovore", {}),
        ("cairn_necromancer", 3.4, "naturalis:undead", "arcane_vision", "necrovore", {}),
        ("mossy_necromancer", 3.2, "naturalis:undead", "arcane_vision", "necrovore", {}),
        ("wither_necromancer", 3.8, "naturalis:undead", "void_vision", "necrovore", {}),
        ("frayed", 2.4, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("rattled", 2.2, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("reprobate", 2.4, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("crypt_slime", 1.8, "naturalis:undead", "undead_vision", "necrovore", {}),
        # Beasts / spiders
        ("black_wolf", 2.8, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        ("hostile_black_wolf", 2.8, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        ("skeleton_wolf", 2.4, "naturalis:undead", "undead_vision", "necrovore", {}),
        ("winter_wolf", 3.2, "naturalis:large_predator", "mammal_vision", "carnivore", {"ice": True}),
        ("hellhound", 3.0, "naturalis:nether_native", "nether_vision", "carnivore", {"nether": True}),
        ("stormhound", 3.0, "naturalis:large_predator", "tempest_vision", "carnivore", {}),
        ("bone_spider", 2.2, "naturalis:insect_arthropod", "insect_vision", "carnivore", {}),
        ("icy_spider", 2.2, "naturalis:insect_arthropod", "insect_vision", "carnivore", {"ice": True}),
        ("web_spider", 2.0, "naturalis:insect_arthropod", "insect_vision", "carnivore", {}),
        ("gnasher", 2.4, "naturalis:small_predator", "mammal_vision", "carnivore", {}),
        ("snapper", 2.6, "naturalis:aquatic_predator", "aquatic_vision", "carnivore", {"aquatic": True}),
        ("ripper", 2.8, "naturalis:small_predator", "mammal_vision", "carnivore", {}),
        ("leapleaf", 3.5, "naturalis:large_predator", "mammal_vision", "carnivore", {}),
        ("inferno", 3.5, "naturalis:nether_native", "nether_vision", "carnivore", {"nether": True}),
        ("wartling", 1.0, "naturalis:nether_native", "nether_vision", "carnivore", {"nether": True}),
        ("watchling", 2.0, "naturalis:ethereal_spirit", "void_vision", "necrovore", {}),
        ("snareling", 2.2, "naturalis:ethereal_spirit", "void_vision", "necrovore", {}),
        ("blastling", 2.0, "naturalis:ethereal_spirit", "void_vision", "necrovore", {}),
        ("irk", 1.2, "naturalis:ethereal_spirit", "void_vision", "necrovore", {"flight": True}),
        ("sprite", 0.8, "naturalis:floral_fey", "fae_vision", "florivore", {"flight": True}),
        ("carrion_fly", 0.4, "naturalis:insect_arthropod", "insect_vision", "scavenger", {"flight": True}),
        ("carrion_maggot", 0.25, "naturalis:insect_arthropod", "insect_vision", "scavenger", {}),
        ("whisperer", 2.8, "naturalis:small_predator", "arcane_vision", "carnivore", {}),
        ("wavewhisperer", 3.0, "naturalis:aquatic_predator", "aquatic_vision", "carnivore", {"aquatic": True}),
        # Constructs
        ("redstone_golem", 7.0, "naturalis:golem_construct", "ferrous_vision", "lithovore", {}),
        ("hostile_redstone_golem", 7.0, "naturalis:golem_construct", "ferrous_vision", "lithovore", {}),
        ("redstone_cube", 2.0, "naturalis:golem_construct", "ferrous_vision", "lithovore", {}),
        ("redstone_ministrosity", 4.5, "naturalis:golem_construct", "ferrous_vision", "lithovore", {}),
        ("stone_ministrosity", 4.0, "naturalis:golem_construct", "ferrous_vision", "lithovore", {}),
        ("mini_ghast", 1.8, "naturalis:flying_predator", "nether_vision", "carnivore", {"flight": True, "nether": True}),
        ("blackguard_servant", 3.5, "naturalis:undead", "undead_vision", "necrovore", {}),
    ]
    for path, mass, arch, shader, diet, kwargs in goety_hostiles:
        hostile_profile(ns, path, arch, mass, diet, shader, mod_tag="goety", **kwargs)

    # Servant variants — inherit undead/caster defaults via mass-tuned profiles
    goety_servants = [
        "bear_servant", "blaze_servant", "blastling_servant", "bone_spider_servant", "border_wraith_servant",
        "bound_cryologer", "bound_evoker", "bound_geomancer", "bound_iceologer", "bound_storm_caster",
        "bound_wind_caller", "brood_mother_servant", "cairn_necromancer_servant", "cave_spider_servant",
        "crusher_servant", "cryologer_servant", "crypt_slime_servant", "drowned_necromancer_servant",
        "drowned_servant", "evoker_servant", "frayed_servant", "frozen_zombie_servant", "geomancer_servant",
        "ghast_servant", "guardian_servant", "heretic_servant", "hoglin_servant", "husk_servant",
        "iceologer_servant", "icy_spider_servant", "irk_servant", "jungle_zombie_servant",
        "magma_cube_servant", "maverick_servant", "mossy_necromancer_servant", "mossy_skeleton_servant",
        "mountaineer_servant", "muck_wraith_servant", "necromancer_servant", "phantom_servant",
        "piker_servant", "pillager_servant", "polar_bear_servant", "rattled_servant", "ravager_servant",
        "reaper_servant", "reprobate_servant", "ripper_servant", "signaler_servant",
        "skeleton_pillager_servant", "skeleton_servant", "slime_servant", "snareling_servant",
        "spider_servant", "storm_caster_servant", "stray_servant", "sunken_skeleton_servant",
        "trampler_servant", "tropical_slime_servant", "vanguard_servant", "vex_servant",
        "vindicator_chef_servant", "vindicator_servant", "warlock_servant", "watchling_servant",
        "web_spider_servant", "wildfire_servant", "wind_caller_servant", "witch_servant",
        "wither_necromancer_servant", "wither_skeleton_servant", "wraith_servant",
        "zombie_ravager_servant", "zombie_servant", "zombie_villager_servant", "zombie_vindicator_servant",
        "zpiglin_brute_servant", "zpiglin_servant",
    ]
    for path in goety_servants:
        arch = "naturalis:undead"
        diet = "necrovore"
        shader = "undead_vision"
        mass = 2.4
        kwargs: dict = {}
        if any(x in path for x in ("ghast", "vex", "phantom", "wraith", "reaper", "blaze", "wildfire")):
            arch = "naturalis:ethereal_spirit"
            shader = "void_vision"
            kwargs["flight"] = True
        if any(x in path for x in ("bear", "hoglin", "ravager", "polar")):
            arch = "naturalis:large_predator"
            diet = "carnivore"
            shader = "mammal_vision"
            mass = 4.0
        if "spider" in path:
            arch = "naturalis:insect_arthropod"
            diet = "carnivore"
            shader = "insect_vision"
            mass = 1.8
        if any(x in path for x in ("drowned", "guardian", "sunken", "tropical")):
            kwargs["aquatic"] = True
            shader = "aquatic_vision" if "guardian" in path or "tropical" in path else "undead_vision"
        if any(x in path for x in ("cryologer", "iceologer", "frozen", "icy", "stray")):
            kwargs["ice"] = True
            shader = "tempest_vision" if "ologer" in path or "caster" in path else shader
        if any(x in path for x in ("blaze", "zpiglin", "warlock", "heretic", "magma", "wildfire", "hoglin")):
            kwargs["nether"] = True
            shader = "nether_vision"
            arch = "naturalis:nether_native" if "blaze" in path or "wildfire" in path else arch
        if any(x in path for x in ("evoker", "geomancer", "necromancer", "witch", "wind_caller", "storm")):
            shader = "arcane_vision"
            mass = 2.8
        hostile_profile(ns, path, arch, mass, diet, shader, mod_tag="goety", extra_tags=["servant"], **kwargs)

    passive_profile(ns, "twilight_goat", "naturalis:ground_herbivore", 2.8, "herbivore", "mammal_vision",
                    mod_tag="goety", extra_tags=["mammal"])
    passive_profile(ns, "prisoner", "naturalis:ground_herbivore", 2.4, "omnivore", "mammal_vision",
                    mod_tag="goety")
    for plant, mass, aquatic in [("poison_anemone", 1.2, True), ("poison_quill_vine", 1.5, False),
                                  ("quick_growing_kelp", 1.0, True), ("quick_growing_vine", 1.0, False)]:
        hostile_profile(ns, plant, "naturalis:aquatic_predator" if aquatic else "naturalis:insect_arthropod",
                        mass, "carnivore", "aquatic_vision" if aquatic else "insect_vision",
                        mod_tag="goety", aquatic=aquatic, extra_tags=["plant"])

    # ---------- THE AETHER ----------
    ns = "aether"
    write(
        DEFAULTS / f"{ns}.json",
        {
            "namespace": ns,
            "default_inherits": "naturalis:ground_herbivore",
            "overrides": {
                "aechor_plant": {"inherits": "naturalis:insect_arthropod", "diet": "carnivore", "mass": 1.5},
                "cockatrice": {"inherits": "naturalis:flying_predator", "diet": "carnivore", "mass": 2.4},
                "evil_whirlwind": {"inherits": "naturalis:ethereal_spirit", "mass": 2.0},
                "fire_minion": {"inherits": "naturalis:nether_native", "mass": 2.5},
                "mimic": {"inherits": "naturalis:small_predator", "diet": "omnivore", "mass": 2.2},
                "sentry": {"inherits": "naturalis:golem_construct", "diet": "lithovore", "mass": 2.0},
                "slider": {"inherits": "naturalis:boss_entity", "mass": 10.0},
                "sun_spirit": {"inherits": "naturalis:boss_entity", "mass": 11.0},
                "valkyrie": {"inherits": "naturalis:flying_predator", "mass": 3.5},
                "valkyrie_queen": {"inherits": "naturalis:boss_entity", "mass": 7.0},
                "whirlwind": {"inherits": "naturalis:ethereal_spirit", "mass": 1.8},
                "zephyr": {"inherits": "naturalis:flying_predator", "mass": 4.5},
                "blue_swet": {"inherits": "naturalis:insect_arthropod", "diet": "omnivore", "mass": 1.2},
                "golden_swet": {"inherits": "naturalis:insect_arthropod", "diet": "omnivore", "mass": 1.2},
            },
        },
    )
    passive_profile(ns, "aerbunny", "naturalis:ground_herbivore", 0.6, "herbivore", "mammal_vision",
                    mod_tag="aether", extra_tags=["mammal", "pet"])
    passive_profile(ns, "aerwhale", "naturalis:flying_predator", 8.0, "herbivore", "mammal_vision",
                    mod_tag="aether", flight=True, extra_tags=["mammal", "sky"])
    passive_profile(ns, "flying_cow", "naturalis:ground_herbivore", 3.2, "herbivore", "mammal_vision",
                    mod_tag="aether", flight=True, extra_tags=["mammal"])
    passive_profile(ns, "moa", "naturalis:flying_predator", 3.0, "omnivore", "avian_vision",
                    mod_tag="aether", flight=True, extra_tags=["bird", "mount"])
    passive_profile(ns, "phyg", "naturalis:ground_herbivore", 2.8, "omnivore", "mammal_vision",
                    mod_tag="aether", flight=True, extra_tags=["mammal"])
    passive_profile(ns, "sheepuff", "naturalis:ground_herbivore", 2.6, "herbivore", "mammal_vision",
                    mod_tag="aether", extra_tags=["mammal"])
    for path, arch, mass, diet, shader, kwargs in [
        ("aechor_plant", "naturalis:insect_arthropod", 1.5, "carnivore", "insect_vision", {"static": True}),
        ("blue_swet", "naturalis:insect_arthropod", 1.2, "omnivore", "insect_vision", {}),
        ("golden_swet", "naturalis:insect_arthropod", 1.2, "omnivore", "insect_vision", {}),
        ("cockatrice", "naturalis:flying_predator", 2.4, "carnivore", "reptile_vision", {"flight": True}),
        ("evil_whirlwind", "naturalis:ethereal_spirit", 2.0, "necrovore", "tempest_vision", {"flight": True}),
        ("fire_minion", "naturalis:nether_native", 2.5, "carnivore", "nether_vision", {"nether": True}),
        ("mimic", "naturalis:small_predator", 2.2, "omnivore", "mammal_vision", {}),
        ("sentry", "naturalis:golem_construct", 2.0, "lithovore", "ferrous_vision", {}),
        ("whirlwind", "naturalis:ethereal_spirit", 1.8, "necrovore", "tempest_vision", {"flight": True}),
        ("zephyr", "naturalis:flying_predator", 4.5, "carnivore", "tempest_vision", {"flight": True}),
        ("valkyrie", "naturalis:flying_predator", 3.5, "omnivore", "avian_vision", {"flight": True}),
    ]:
        hostile_profile(ns, path, arch, mass, diet, shader, mod_tag="aether", **kwargs)
    for path, mass, arch, shader, kwargs in [
        ("slider", 10.0, "naturalis:boss_entity", "ferrous_vision", {}),
        ("sun_spirit", 11.0, "naturalis:boss_entity", "nether_vision", {"nether": True, "flight": True}),
        ("valkyrie_queen", 7.0, "naturalis:boss_entity", "avian_vision", {"flight": True}),
    ]:
        hostile_profile(ns, path, arch, mass, "carnivore", shader, mod_tag="aether", boss=True, **kwargs)

    total = sum(1 for _ in PROFILES.rglob("*.json"))
    print(f"Generated profiles. Total mob_profiles JSON files: {total}")


if __name__ == "__main__":
    main()
