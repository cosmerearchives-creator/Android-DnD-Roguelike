"""Check enemy rigged glTF assets and first-forest catalog without Blender."""
import json, struct
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/"godot"/"assets"
GOBLINS=["goblin_warrior","emerald_blade_warden_rigged","goblin_alt_2_rigged","goblin_alt_3_rigged","goblin_alt_4_rigged"]
def glb(stem):
    data=(ASSETS/(stem+".glb")).read_bytes()
    assert data[:4]==b"glTF",(stem,"invalid glTF header")
    assert struct.unpack_from("<I",data,8)[0]==len(data)
    size,typ=struct.unpack_from("<I4s",data,12)
    assert typ==b"JSON"
    return json.loads(data[20:20+size].decode("utf-8"))
for name in GOBLINS:
    doc=glb(name)
    tracks={str(a.get("name","")).lower() for a in doc.get("animations",[])}
    joints=max((len(s.get("joints",[])) for s in doc.get("skins",[])),default=0)
    assert {"idle","walk","attack"}.issubset(tracks),(name,tracks)
    assert joints>=15,(name,joints)
    print("ENEMY_OK",name,joints,sorted(tracks))
beetle=glb("fire_beetle")
assert any("walk" in a.get("name","").lower() for a in beetle.get("animations",[]))
assert beetle.get("skins"),"Beetle animation has no skin"
print("BEETLE_OK",len(beetle.get("skins",[])))
catalog=(ROOT/"godot"/"goblin_variants.gd").read_text()
for name in GOBLINS: assert name+".glb" in catalog
spawner=(ROOT/"godot"/"enemy_spawner.gd").read_text()
assert "MAX_ENEMIES := 8" in spawner
assert "Fire Beetle" in spawner and "defeat_nearest" in spawner
best=(ROOT/"godot"/"bestiary.gd").read_text()
assert '_button(item[0],_select_creature.bind(item[1]))' in best
print("ENEMY_CATALOG_OK")
