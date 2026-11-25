"""Pose the actual in-game avatar atlases and their matching customization masks."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets"
WIDTH, HEIGHT, FACINGS = 168, 192, 8
# Project the forward lean along the same directional order as the avatar.
FORWARD_X = (-1, 0, 1, 1, 1, 0, -1, -1)


def pose_atlas(source, frames, walking, mask=False):
    atlas = Image.new(source.mode, (WIDTH * frames, HEIGHT * FACINGS))
    for facing in range(FACINGS):
        for frame in range(frames):
            x, y = frame * WIDTH, facing * HEIGHT
            cell = source.crop((x, y, x + WIDTH, y + HEIGHT))
            drop = 8 + (frame % 4 in (1, 2)) if walking else 9
            # Lower the upper body as a unit. Bend the silhouette around the
            # hips and knees, keeping the entire foot-contact band stationary.
            neutral = (0, 70, 100, 130, 151, HEIGHT)
            posed = (drop, 70 + drop, 100 + drop, 134, 151, HEIGHT)
            lean = (4, 4, 0, 2, 0, 0)
            offset = FORWARD_X[facing]
            mesh = []
            for i in range(len(neutral) - 1):
                y0, y1 = neutral[i:i + 2]
                sx0, sx1 = -offset * lean[i], -offset * lean[i + 1]
                mesh.append(((0, posed[i], WIDTH, posed[i + 1]),
                             (sx0, y0, sx1, y1, WIDTH + sx1, y1, WIDTH + sx0, y0)))
            # Use identical registration and filtering for beauty and masks.
            cell = cell.transform(cell.size, Image.Transform.MESH, mesh, Image.Resampling.BILINEAR)
            if mask:
                assert cell.getchannel("R").getbbox(), (facing, frame)
            else:
                assert cell.getchannel("A").getbbox(), (facing, frame)
            atlas.paste(cell, (x, y))
    return atlas


for motion, frames in (("idle", 1), ("walk", 8)):
    art_dir = ROOT / "blender_world"
    mask_dir = ROOT / "petite_modules"
    with Image.open(art_dir / f"petite_detail_{motion}.png") as art:
        pose_atlas(art.convert("RGBA"), frames, motion == "walk").save(art_dir / f"sneak_{motion}.png")
    with Image.open(mask_dir / f"{motion}.png") as mask:
        pose_atlas(mask.convert("RGB"), frames, motion == "walk", mask=True).save(mask_dir / f"sneak_{motion}.png")
