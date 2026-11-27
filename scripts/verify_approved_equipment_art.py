#!/usr/bin/env python3
"""Lossless user-supplied Equipment UI source / derivative / import QA."""
from pathlib import Path
from PIL import Image, ImageChops
import hashlib
root=Path(__file__).resolve().parents[1]/"godot"/"ui_skin"
original=root/"approved_equipment_source.png"
dir=root/"approved_equipment"
expected_sha="cae4145663bf6d7e2d1f696091aebb5efde58ded01ca4ff91eb14b318c7957e4"
assert original.is_file()
assert hashlib.sha256(original.read_bytes()).hexdigest()==expected_sha, "UI original source was modified"
src=Image.open(original).convert("RGBA")
assert src.size==(1280,1229)
crops={
    "tab_header":(2,206,515,361),
    "stat_panel":(525,153,1247,370),
    "inventory_panel":(516,437,1248,790),
    "detail_panel":(516,777,1248,1115),
    "top_banner":(27,54,519,189),
    "slot_blank":(548,545,623,623),
}
for name,box in crops.items():
    file=dir/(name+".png")
    assert file.is_file(),file
    im=Image.open(file).convert("RGBA")
    assert im.size==(box[2]-box[0],box[3]-box[1])
    assert ImageChops.difference(src.crop(box),im).getbbox() is None, "Unapproved edits to original art "+name
    imp=Path(str(file)+".import")
    if imp.is_file():
        data=imp.read_text(encoding="utf-8")
        assert "compress/mode=0" in data, "Texture import is not lossless "+name
        assert "process/size_limit=0" in data, "Texture import unexpectedly downsized "+name
    print("APPROVED_UI_CROP_OK",name,im.size)
art=Image.open(dir/"character_panel.png").convert("RGBA")
assert art.size==(513,774)
reference=src.crop((2,344,515,1118))
assert ImageChops.difference(art.crop((0,0,370,774)), reference.crop((0,0,370,774))).getbbox() is None
assert Image.open(dir/"occupied_slot.png").size==(116,100)
assert Image.open(dir/"accessory_slot.png").size==(112,95)
print("APPROVED_EQUIPMENT_ASSET_OK original_sha256",expected_sha,"editable_15_slot_derivative=yes")
