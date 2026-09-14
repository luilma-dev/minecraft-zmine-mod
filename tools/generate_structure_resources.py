"""Rebuild ZMine's loot, block/item definitions and building feature resources.

Run from any directory: python tools/generate_structure_resources.py
Original Blockbench files in images/Boxes are never modified.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
NAMES = ["box1", "box2", "box3", "box3_1", "medkit", "medkit_military", "medkit_wall"]


def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def entry(name, weight=10, minimum=1, maximum=3, potion=None):
    result = {"type": "minecraft:item", "name": f"minecraft:{name}", "weight": weight}
    result["functions"] = [{"function": "minecraft:set_count", "count": {
        "type": "minecraft:uniform", "min": minimum, "max": maximum}}]
    if potion:
        result["functions"].append({"function": "minecraft:set_potion", "id": f"minecraft:{potion}"})
    return result


water = entry("potion", 12, 1, 2, "water")
tables = {
    "food": [entry("bread", 20), entry("apple", 18, 1, 4), entry("baked_potato", 16),
             entry("carrot", 12, 2, 5), entry("dried_kelp", 8, 2, 6), entry("cookie", 10, 2, 6),
             entry("rotten_flesh", 3, 1, 2), water, entry("beetroot_soup", 5, 1, 1)],
    "household": [entry("bread", 14), entry("apple", 10), entry("string", 14, 1, 5),
                  entry("paper", 15, 1, 6), entry("coal", 12), entry("torch", 12, 2, 8),
                  entry("leather", 8), entry("glass_bottle", 10), entry("book", 6, 1, 2),
                  entry("stone_axe", 3, 1, 1), water],
    "tools": [entry("iron_nugget", 18, 3, 12), entry("iron_ingot", 10),
              entry("copper_ingot", 15, 2, 6), entry("coal", 15, 2, 6), entry("stick", 12, 2, 8),
              entry("redstone", 10, 1, 5), entry("torch", 10, 3, 10), entry("flint", 8),
              entry("iron_pickaxe", 3, 1, 1), entry("iron_axe", 3, 1, 1), entry("bucket", 4, 1, 1)],
    "medical": [entry("paper", 14, 2, 6), entry("honey_bottle", 14, 1, 2),
                entry("potion", 22, 1, 1, "healing"), entry("potion", 7, 1, 1, "regeneration"),
                entry("splash_potion", 6, 1, 1, "healing"), water,
                entry("golden_carrot", 8, 1, 2), entry("golden_apple", 2, 1, 1)],
    "military": [entry("arrow", 25, 4, 16), entry("iron_ingot", 12), entry("bread", 16, 2, 4),
                 entry("torch", 14, 4, 10), entry("leather_boots", 5, 1, 1),
                 entry("iron_helmet", 3, 1, 1), entry("crossbow", 2, 1, 1),
                 entry("iron_sword", 3, 1, 1), entry("shield", 3, 1, 1), water],
}

for name, entries in tables.items():
    write(f"data/zmine/loot_table/chests/{name}.json", {
        "type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": 4, "max": 7},
            "entries": entries,
        }],
    })

for name in NAMES:
    write(f"assets/zmine/blockstates/{name}.json", {"variants": {
        f"facing={direction}": {"model": f"zmine:block/{name}", "y": angle}
        for direction, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
    }})
    write(f"assets/zmine/items/{name}.json", {"model": {
        "type": "minecraft:model", "model": f"zmine:item/{name}"
    }})
    write(f"data/zmine/loot_table/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"zmine:{name}"}
        ], "conditions": [{"condition": "minecraft:survives_explosion"}]}]
    })
    source_name = "medkit_miltary" if name == "medkit_military" else name
    model = json.loads((ROOT / f"images/Boxes/{source_name}.json").read_text(encoding="utf-8"))
    model["textures"] = {"0": f"zmine:block/{name}", "particle": f"zmine:block/{name}"}
    # Attach the thin medicine cabinet to the back of its block (north-facing front).
    if name == "medkit_wall":
        for element in model["elements"]:
            for key in ("from", "to"):
                element[key][1] += 3
                element[key][2] += 6.75
            if "rotation" in element:
                element["rotation"]["origin"][1] += 3
                element["rotation"]["origin"][2] += 6.75
    write(f"assets/zmine/models/block/{name}.json", model)

for lang, labels in {
    "pt_br": ["Caixa de suprimentos", "Caixote reforçado", "Caixa de mantimentos", "Caixa de pertences",
              "Maleta de primeiros socorros", "Kit médico militar", "Armário de primeiros socorros"],
    "en_us": ["Supply Crate", "Reinforced Crate", "Ration Box", "Personal Belongings",
              "First Aid Case", "Military Medical Kit", "First Aid Cabinet"],
}.items():
    write(f"assets/zmine/lang/{lang}.json", {f"block.zmine.{name}": label for name, label in zip(NAMES, labels)})

for name in ["abandoned_building", "abandoned_house", "abandoned_apartments", "abandoned_market", "abandoned_clinic", "abandoned_warehouse",
             "abandoned_diner", "abandoned_gas_station", "abandoned_garage", "abandoned_checkpoint", "survivor_camp", "abandoned_brick_house", "abandoned_townhouse", "abandoned_farmhouse", "apocalypse_district"]:
    write(f"data/zmine/worldgen/configured_feature/{name}.json", {"type": f"zmine:{name}", "config": {}})

write("data/zmine/worldgen/placed_feature/abandoned_building.json", {
    "feature": "zmine:abandoned_building", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 12},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
        {"type": "minecraft:biome"},
    ]
})

write("data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"zmine:{name}" for name in NAMES]})
write("data/zmine/worldgen/placed_feature/apocalypse_district.json", {
    "feature": "zmine:apocalypse_district", "placement": [
        {"type": "minecraft:count", "count": 1},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
        {"type": "minecraft:biome"},
    ]
})
print("Generated 7 containers, 5 loot pools, 10 buildings and the district/scenery feature.")
