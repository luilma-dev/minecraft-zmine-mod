"""Keep vanilla surface structures from overlapping the continuous city grid.

Underground structures, Nether and End structure tags are deliberately preserved.
Run from any directory with Python 3; no external dependencies are required.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
SURFACE_STRUCTURES = (
    "buried_treasure", "desert_pyramid", "igloo", "jungle_temple",
    "ocean_monument", "ocean_ruin_cold", "ocean_ruin_warm", "pillager_outpost",
    "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_mountain",
    "ruined_portal_ocean", "ruined_portal_standard", "ruined_portal_swamp",
    "shipwreck", "shipwreck_beached", "swamp_hut", "trail_ruins",
    "village_desert", "village_plains", "village_savanna", "village_snowy",
    "village_taiga", "woodland_mansion",
)

if __name__ == "__main__":
    folder = ROOT / "src/main/resources/data/minecraft/tags/worldgen/biome/has_structure"
    folder.mkdir(parents=True, exist_ok=True)
    for structure in SURFACE_STRUCTURES:
        (folder / f"{structure}.json").write_text(
            json.dumps({"replace": True, "values": []}, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(SURFACE_STRUCTURES)} city surface exclusions")
    feature_folder = ROOT / "src/main/resources/data/zmine/worldgen/configured_feature"
    feature_folder.mkdir(parents=True, exist_ok=True)
    for feature in ("military_base", "apartment_tower", "abandoned_hospital", "ruined_tower"):
        (feature_folder / f"{feature}.json").write_text(
            json.dumps({"type": f"zmine:{feature}", "config": {}}, indent=2) + "\n", encoding="utf-8")
