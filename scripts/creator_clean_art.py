from pathlib import Path
from PIL import Image,ImageDraw
root=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource")
src=Image.open(root/"godot/creator_ui/source_elements.png").convert("RGBA")
out=root/"godot/creator_ui"
# Derive clean frame components from intact long forged-button art. Original atlas untouched.
# Source large blank horizontal bar is at x 348..743, y 265..348.
im=src.crop((350,270,740,353))
im.save(out/"creator_header_frame.png")
# Layout surface: sample only interior clean brown leather, avoiding gold posts/controls.
# Use direct full resolution crops without resampling.
leather=src.crop((876,799,946,869))
leather.save(out/"creator_leather_tile.png")
# Frame: actual independent empty octagonal art from x1090..1205 y 482..594.
button=src.crop((1092,484,1206,596))
button.save(out/"creator_button_source.png")
# Stone pedestal from lower equipment body, actual stage at x150..318 y 861..929.
pedestal=src.crop((148,876,316,943))
pedestal.save(out/"creator_stone_pedestal.png")
# Four real metal ornamental corner caps from the lower-right sheet
for name,box in {
 "corner_tl":(981,1127,1056,1214),
 "corner_tr":(1060,1127,1139,1214),
 "corner_bl":(982,1218,1059,1309),
 "corner_br":(1061,1218,1143,1309)
}.items():
 src.crop(box).save(out/("creator_"+name+".png"))
print("CROPS",[(x,(Image.open(out/x).size)) for x in ["creator_header_frame.png","creator_leather_tile.png","creator_button_source.png","creator_stone_pedestal.png"]])
