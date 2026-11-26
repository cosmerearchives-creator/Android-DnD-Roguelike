"""Synthesize a seamless meadow grass material from our original Wildlands atlas."""
from pathlib import Path
from PIL import Image, ImageEnhance, ImageDraw, ImageFilter, ImageOps
import random

root = Path(__file__).resolve().parent.parent
atlas = Image.open(root / "app/src/main/assets/wildlands_terrain.webp").convert("RGB")
randomizer = random.Random(81492)
size = 512
ground = Image.new("RGB", (size,size), (73,91,39))
for i in range(145):
    diameter = randomizer.randint(96,164)
    # Favor the grassy upper-left atlas, occasionally include exposed earth.
    sx = randomizer.randint(0, 190)
    sy = randomizer.randint(0, 24 if i % 7 else 75)
    sample = atlas.crop((sx,sy,sx+diameter,sy+diameter))
    if randomizer.random() < 0.5:
        sample = ImageOps.mirror(sample)
    if randomizer.random() < 0.5:
        sample = ImageOps.flip(sample)
    mask = Image.new("L", (diameter,diameter), 0)
    drawer = ImageDraw.Draw(mask)
    drawer.ellipse((12,12,diameter-12,diameter-12),fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(diameter * 0.07))
    cx = randomizer.randrange(size)
    cy = randomizer.randrange(size)
    ox,oy = cx-diameter//2,cy-diameter//2
    for dx in (-size,0,size):
        for dy in (-size,0,size):
            ground.paste(sample,(ox+dx,oy+dy),mask)
ground = ImageEnhance.Color(ground).enhance(0.84)
target = root / "godot/grass_detail.png"
ground.save(target, "PNG", optimize=True)
print("GRASS_TILE_CREATED", target, target.stat().st_size)
