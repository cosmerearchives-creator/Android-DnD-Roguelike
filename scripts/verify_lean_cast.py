"""Fail the Android build if retired characters accidentally reappear."""
from pathlib import Path
import json
import struct

root = Path(__file__).resolve().parents[1]
assets = root/"godot"/"assets"
allowed = {
    "human_adventurer.glb", "goblin_warrior.glb", "fire_beetle.glb",
    "male_human.glb", "male_human_leather_armored.glb", "twig_blight.glb", "wolf_quadruped.glb",
    "humanoid_mixamo_animation_library.glb", "pine.glb", "broadleaf.glb",
    "boulder.glb",
}
existing = {file.name for file in assets.glob("*.glb")}
unexpected = sorted(existing - allowed)
missing = sorted(allowed - existing)
assert not unexpected, f"Retired GLB characters reappeared: {unexpected}"
assert not missing, f"Required GLB assets missing: {missing}"
assert not list(assets.glob("katie*.png")), "Old duplicate Katie textures still present"
assert not list(assets.glob("*.blend*")), "Old unshipped Blender source still in game assets"


def gltf_info(name):
    blob = (assets/name).read_bytes()
    magic, version, total = struct.unpack_from("<4sII", blob)
    assert magic == b"glTF" and version == 2 and total == len(blob), name
    json_len, kind = struct.unpack_from("<I4s", blob, 12)
    assert kind == b"JSON", name
    return json.loads(blob[20:20+json_len])


human = gltf_info("human_adventurer.glb")
goblin = gltf_info("goblin_warrior.glb")
shared = gltf_info("humanoid_mixamo_animation_library.glb")
beetle = gltf_info("fire_beetle.glb")
assert len(human.get("skins", [])) == 1 and len(human.get("animations", [])) == 20
assert len(goblin.get("skins", [])) == 1
assert "Axe_Spin_Attack" in [a["name"] for a in goblin.get("animations", [])]
assert len(shared.get("animations", [])) == 20
assert len(gltf_info("male_human.glb").get("skins", [])) == 1
assert len(gltf_info("twig_blight.glb").get("skins", [])) == 1
assert len(gltf_info("twig_blight.glb")["skins"][0]["joints"]) == 28
assert len(gltf_info("male_human.glb")["skins"][0]["joints"]) == 84
armored = gltf_info("male_human_leather_armored.glb")
assert len(armored.get("skins", [])) == 1 and len(armored["skins"][0]["joints"]) == 84
assert len(armored["meshes"]) == 2 and len(armored.get("animations", [])) == len(gltf_info("male_human.glb").get("animations", []))
assert {armored["nodes"][i]["name"] for i in armored["skins"][0]["joints"]} == {gltf_info("male_human.glb")["nodes"][i]["name"] for i in gltf_info("male_human.glb")["skins"][0]["joints"]}
assert "Shield_Push_Left" in [a["name"] for a in gltf_info("twig_blight.glb")["animations"]]
assert len(beetle.get("skins", [])) == 1
wolf = gltf_info("wolf_quadruped.glb")
assert len(wolf.get("skins", [])) == 1 and len(wolf["skins"][0]["joints"]) == 46
assert {a["name"] for a in wolf["animations"]} == {"dog_dog_over","dog_idle1","dog_run","leopard_skill_feipu"}
asset_total = sum(p.stat().st_size for p in assets.iterdir() if p.is_file())
import hashlib
assert hashlib.sha256((assets/"human_adventurer.glb").read_bytes()).hexdigest() == "119496a7536e41fca167b8f1ce6162883cc37bf8d4db775b4322eb59362aac68"
assert hashlib.sha256((assets/"goblin_warrior.glb").read_bytes()).hexdigest() == "29c21a1c5e1e3bdcd49e99e7cafaf63c466dbfec8b029570e8a13d2e06d2bdcc"
print(f"LEAN_ASSET_CHECK_OK files={len(existing)} assets_bytes={asset_total}",
      "human", (assets/"human_adventurer.glb").stat().st_size,
      "goblin", (assets/"goblin_warrior.glb").stat().st_size)
