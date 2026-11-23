"""Pack the weapon test bakes and approved inventory art into Android assets."""
import argparse
import json
import math
from pathlib import Path

from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument("renders", type=Path)
parser.add_argument("icons", type=Path)
parser.add_argument("destination", type=Path)
options = parser.parse_args()
out = options.destination
out.mkdir(parents=True, exist_ok=True)

width, height = 144, 164
styles = ("classic_study", "petite_pale", "classic_male", "petite_male", "dagger_goblin")
for style, variant in enumerate(styles):
    root = options.renders / variant
    motions = {"swing": 5, "thrust": 5, "heavy": 5, "bow": 5}
    if style == 4:
        motions = {"swing": 5, "bow": 5, "walk": 8, "idle": 1}
    for motion, count in motions.items():
        name = f"{style}_{motion}"
        atlas = Image.new("RGBA", (width * count, height * 8))
        for facing in range(8):
            for frame in range(count):
                with Image.open(root / f"{motion}_{facing}_{frame}.png") as sprite:
                    assert sprite.mode == "RGBA" and sprite.size == (width, height)
                    box = sprite.getchannel("A").getbbox()
                    assert box and min(box[0], box[1], width - box[2], height - box[3]) > 0
                    atlas.paste(sprite, (frame * width, facing * height))
        atlas.save(out / f"{name}.webp", lossless=True)
        if count == 5:
            anchors = json.loads((root / f"{motion}.json").read_text())
            assert len(anchors) == 40 and all(len(a) == 5 and 0 < a[0] < width and 0 < a[1] < height for a in anchors)
            (out / f"{name}.txt").write_text("".join(" ".join(map(str,a))+"\n" for a in anchors))

# Local grip coordinates in the approved inventory models, followed by their
# intended on-character size in the smaller trial frame.
weapons = {
    "longsword": (0,.34,66), "scimitar": (0,.31,61), "battleaxe": (0,.63,63), "warhammer": (0,.63,62),
    "dagger": (0,.29,38), "rapier": (0,.34,62), "spear": (0,1.0,88),
    "greatsword": (0,.46,83), "greataxe": (0,.62,87), "maul": (0,.71,83),
    "shortbow": (-.26,.77,61), "longbow": (-.27,1.0,82),
}
# Inventory factory camera and rotation, used to find each grip in the PNG.
yaw=math.atan2(1.16,6)
tilt=math.atan2(2.16-1.08,math.hypot(1.16,6))
for name,(x,z,size) in weapons.items():
    xx=math.cos(.47)*x+math.sin(.47)*(z-1.13)
    zz=-math.sin(.47)*x+math.cos(.47)*(z-1.13)+1.13
    px=128+xx*math.cos(yaw)*256/2.43
    py=128-(math.cos(tilt)*(zz-1.08)-math.sin(tilt)*math.sin(yaw)*xx)*256/2.43
    with Image.open(options.icons / f"{name}.png") as icon:
        box=icon.getchannel("A").getbbox()
        assert box
        icon.crop(box).save(out / f"{name}.webp", lossless=True)
    (out / f"{name}.txt").write_text(f"{px-box[0]:.2f} {py-box[1]:.2f} {size}\n")
