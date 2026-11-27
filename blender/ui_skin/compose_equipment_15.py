from PIL import Image
from pathlib import Path
root=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\ui_skin\approved_equipment")
source=Image.open(root.parent/"approved_equipment_source.png").convert("RGBA")
body=source.crop((2,344,515,1118))
# Treat the right-side slot column as modular artwork from the same user source.
# Preserve the original full-size supplied file and all seven original slot crops.
right_boxes=[(386,391,502,491),(386,492,502,591),(386,591,502,692),
             (386,691,502,791),(386,790,502,891),(386,888,502,990),
             (386,989,502,1090)]
right_slots=[source.crop(b) for b in right_boxes]
blank=Image.open(root/"slot_blank.png").convert("RGBA")
# Cover only the right slot column, not the surrounding frame or center portrait.
# Reuse leather pixels from the original source, preserving the material palette.
fill=source.crop((209,415,319,1083)).resize((116,710),Image.Resampling.BICUBIC)
body.paste(fill,(382,35),fill)
for i in range(8):
    y=35+i*89
    slot=right_slots[i] if i<5 else right_slots[i-1] if i>=6 else blank
    # Each pre-existing slot is scaled solely to fit the eighth functional slot.
    slot=slot.resize((116,88),Image.Resampling.LANCZOS)
    if i==5:
        # Neutral accessory slot retains source bronze slot framing.
        frame=right_slots[4].resize((116,88),Image.Resampling.LANCZOS)
        # Replace the source ring icon with blank dark leather from an original empty item slot.
        interior=blank.resize((76,59),Image.Resampling.LANCZOS)
        frame.paste(interior,(20,18),interior)
        slot=frame
    body.paste(slot,(381,y),slot)
body.save(root/"character_panel.png")
# Occupied equipment slot: user-supplied frame with the central placeholder
# replaced by the sheet's original blank leather texture.
frame=right_slots[0].copy()
interior=blank.resize((79,64),Image.Resampling.LANCZOS)
frame.paste(interior,(18,19),interior)
frame.save(root/"occupied_slot.png")
print("RIGHT_SIDE_EIGHT_SLOT_LAYOUT_OK",body.size, (root/"occupied_slot.png").stat().st_size)
