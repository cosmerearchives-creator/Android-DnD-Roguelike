from PIL import Image
from pathlib import Path
import hashlib
root=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource")
src=Path(r"G:\ChatGPT Projects\Forests of Hell\Inventory Review\Original Equipment UI Source.png")
dst=root/"godot"/"ui_skin"/"approved_equipment"
dst.mkdir(parents=True,exist_ok=True)
raw=src.read_bytes()
print("SOURCE_SHA256",hashlib.sha256(raw).hexdigest(),"BYTES",len(raw))
(root/"godot"/"ui_skin"/"approved_equipment_source.png").write_bytes(raw)
im=Image.open(src).convert("RGBA")
assert im.size==(1280,1229)
coords={
    "tab_header":(2,206,515,361),
    "character_panel":(2,344,515,1118),
    "stat_panel":(525,153,1247,370),
    "inventory_panel":(516,437,1248,790),
    "detail_panel":(516,777,1248,1115),
    "top_banner":(27,54,519,189),
    "slot_blank":(548,545,623,623),
}
for name,box in coords.items():
    patch=im.crop(box)
    patch.save(dst/(name+".png"),optimize=False)
    print(name, patch.size)
# The reference provides 14 slots. Preserve its seven original right-hand icons,
# add a compatible eighth slot from the same frame, and retain full source untouched.
character=im.crop(coords["character_panel"])
# A source-derived slot with a neutral background is used only for the accessory
# placeholder without fabricating an arbitrary fantasy equipment icon.
extra=im.crop((385,390,497,485))
extra.save(dst/"accessory_slot.png",optimize=False)
print("ASSET_IMPORT_OK")
