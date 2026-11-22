"""Release gate: actual Blender output must be present and contain opaque pixels."""
from pathlib import Path
from PIL import Image
base=Path("app/src/main/assets/blender_world")
for name,cell,cols,rows in (
    ("petite_idle.png",(168,192),4,8),
    ("petite_walk.png",(168,192),8,8),
    ("petite_punch.png",(168,192),5,8),
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
