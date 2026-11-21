#!/usr/bin/env python3
"""Produce a reversible, narrowly masked candidate for Bob rear shoulder trim."""
from pathlib import Path
from PIL import Image, ImageDraw
import hashlib

source = Path("app/src/main/assets/elf_roster/bob/2.webp")
out = Path("build/bob-rear-trim")
out.mkdir(parents=True, exist_ok=True)
original = Image.open(source).convert("RGBA")
assert original.size == (800, 400)
edited = original.copy()
# Gold piping follows the inner-facing contour of the rear shoulder caps.
# Restrict edits to narrow shoulder-edge strips, never the torso or arm joints.
regions = [(373, 10, 388, 89), (444, 10, 460, 89)]
changed = []
for x0, y0, x1, y1 in regions:
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b, a = original.getpixel((x, y))
            if a < 130:
                continue
            # Select dark gold/brown seam pixels, not light skin.
            if not (r > g * 1.10 and g > b * 1.10 and 45 <= r <= 175 and 28 <= g <= 125):
                continue
            # Skin-shaded replacement from nearby inward pixel in same arm cell.
            cell_start = 288 if x < 416 else 416
            candidates = []
            for dx in (-9, -7, -5, 5, 7, 9):
                sx = x + dx
                if not (cell_start + 2 <= sx < cell_start + 126):
                    continue
                sr, sg, sb, sa = original.getpixel((sx, y))
                if sa > 200 and sr >= 135 and sg >= 85 and sb >= 55 and sr > sg > sb:
                    candidates.append((abs(dx), (sr, sg, sb, a)))
            if candidates:
                edited.putpixel((x, y), min(candidates, key=lambda v: v[0])[1])
                changed.append((x, y))
assert changed, "No pixels matched conservative trim mask; inspect original before adjusting"
assert all(any(x0 <= x < x1 and y0 <= y < y1 for x0,y0,x1,y1 in regions) for x,y in changed)
assert all(original.getpixel((x,y))[3] == edited.getpixel((x,y))[3] for x,y in changed)
output = out / "2.webp"
edited.save(output, "WEBP", lossless=True, exact=True, method=6)
assert Image.open(output).convert("RGBA").tobytes() == edited.tobytes()
original.save(out / "before.png")
edited.save(out / "after.png")
# Zoomed side-by-side preview, highlighting the exact two edit strips.
before = original.crop((288, 0, 544, 145)).resize((768, 435))
after = edited.crop((288, 0, 544, 145)).resize((768, 435))
preview = Image.new("RGBA", (1536, 435), (30,30,30,255))
preview.paste(before, (0,0)); preview.paste(after, (768,0))
preview.save(out / "before-after.png")
(out / "report.txt").write_text(
    f"Changed pixels: {len(changed)}\\n"
    f"Original Git blob can be checked independently\\n"
    f"Candidate SHA256: {hashlib.sha256(output.read_bytes()).hexdigest()}\\n"
    f"Regions: {regions}\\n"
)
print((out / "report.txt").read_text())
