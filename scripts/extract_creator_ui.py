from PIL import Image
from pathlib import Path
src=Image.open(r'C:\Users\thebl\Downloads\Untit121212led.png').convert('RGBA')
dst=Path(r'G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\creator_ui')
dst.mkdir(exist_ok=True)
# Pixel-exact full-resolution crops of user-supplied UI components.
areas={
 'character_panel':(20,250,322,650),
 'item_panel':(320,303,702,632),
 'selection_bar':(30,171,483,240),
 'large_selection_bar':(225,173,482,243),
 'ornate_header':(25,171,228,245),
 'frame_left':(26,173,88,244),
 'frame_right':(423,174,480,244),
 'slot':(721,310,810,407),
 'slot_large':(826,304,951,422),
 'panel_full':(1088,1114,1430,1755),
 'panel_frame':(789,753,984,862),
 'bar':(723,424,960,478),
 'icon_helm':(20,4,104,103),
 'icon_eyes':(101,12,176,102),
 'icon_swords':(166,4,258,103),
 'icon_shield':(246,2,328,105),
 'icon_bow':(317,9,409,105),
 'icon_gloves':(402,7,478,106),
 'icon_body':(646,2,740,110),
}
for name,box in areas.items():
 crop=src.crop(box)
 crop.save(dst/(name+'.png'))
 print(name,crop.size,crop.getbbox())
