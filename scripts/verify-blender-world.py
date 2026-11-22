"""Release gate: actual Blender output must be present and contain opaque pixels."""
from pathlib import Path
import hashlib
from PIL import Image
base=Path("app/src/main/assets/blender_world")
source_hash=hashlib.sha256(Path("blender/create_character.py").read_bytes()).hexdigest()
manifest=base/"character_source.sha256"
assert manifest.exists(), "Blender bake has not finished: source digest missing"
assert manifest.read_text().strip()==source_hash, "Blender atlases are stale: wait for bake and asset commit before releasing"
for name,cell,cols,rows in (
    ("petite_idle.png",(168,192),4,8),
    ("petite_walk.png",(168,192),8,8),
    ("petite_punch.png",(168,192),5,8),
    ("classic_idle.png",(168,192),4,8),
    ("classic_walk.png",(168,192),8,8),
    ("classic_punch.png",(168,192),5,8),
    ("goblin_idle.png",(168,192),4,8),
    ("goblin_walk.png",(168,192),8,8),
    ("goblin_punch.png",(168,192),5,8),
    ("terrain.png",(256,256),4,4),
    ("props.png",(256,256),5,1),
):
    path=base/name
    assert path.exists(),f"Missing Blender-rendered game asset: {path}"
    with Image.open(path) as art:
        assert art.mode=="RGBA" and art.size==(cell[0]*cols,cell[1]*rows),(name,art.size)
        for y in range(rows):
            for x in range(cols):
                rect=(x*cell[0],y*cell[1],(x+1)*cell[0],(y+1)*cell[1])
                alpha=art.crop(rect).getchannel("A")
                assert alpha.getbbox(),f"Empty Blender asset {name} ({x},{y})"
        print("VERIFIED_BLENDER_GAME_ASSET",path,path.stat().st_size)
