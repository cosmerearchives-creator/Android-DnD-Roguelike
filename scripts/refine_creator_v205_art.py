from pathlib import Path
from PIL import Image,ImageDraw
root=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource")
atlas=Image.open(root/"godot/creator_ui/source_elements.png").convert("RGBA")
ui=root/"godot/creator_ui"
# Crop the complete standalone, transparent stone oval, not the old cutout
# which contained a stray vertical bar from an adjacent atlas cell.
stone=atlas.crop((1714,1558,1887,1645))
stone.save(ui/"creator_stone_oval.png")
# Source-derived compact rectangular bronze frame. All gold corner details
# are native-resolution pixel crops; none of the original sheet is downscaled.
leather=Image.open(ui/"creator_leather_tile.png").convert("RGBA")
original=Image.open(ui/"creator_button_clean.png").convert("RGBA")
w,h=420,100
frame=Image.new("RGBA",(w,h),(0,0,0,0))
for y in range(0,h,leather.height):
 for x in range(0,w,leather.width):
  frame.alpha_composite(leather,(x,y))
draw=ImageDraw.Draw(frame)
draw.rounded_rectangle((1,1,w-2,h-2),radius=10,outline=(218,151,68,255),width=3)
draw.rounded_rectangle((5,5,w-6,h-6),radius=7,outline=(91,52,26,255),width=3)
draw.line((17,8,w-18,8),fill=(253,204,116,210),width=1)
draw.line((17,h-9,w-18,h-9),fill=(167,112,51,210),width=1)
# Preserve a small native-sized crest at each corner.
corners=[((2,2),(0,0,20,20)),((w-22,2),(400,0,420,20)),
         ((2,h-22),(0,80,20,100)),((w-22,h-22),(400,80,420,100))]
for pos,crop in corners:
 part=original.crop(crop)
 frame.alpha_composite(part,pos)
frame.save(ui/"creator_button_compact.png")
# Source-derived tiny square for control arrows, distinct from navigation.
small=Image.new("RGBA",(130,110),(0,0,0,0))
for y in range(0,110,leather.height):
 for x in range(0,130,leather.width):
  small.alpha_composite(leather,(x,y))
d=ImageDraw.Draw(small)
d.rounded_rectangle((2,2,127,107),radius=11,outline=(224,162,77,255),width=4)
d.rounded_rectangle((9,9,120,100),radius=8,outline=(106,69,29,255),width=2)
small.save(ui/"creator_arrow_compact.png")
print("CREATOR_ART_REFINED",stone.size,frame.size,small.size)

# Active compact button: warm selected glow, no larger decorative corners.
from PIL import ImageFilter
glow=Image.new("RGBA",frame.size,(0,0,0,0))
g=ImageDraw.Draw(glow)
g.rounded_rectangle((5,4,frame.width-6,frame.height-5),radius=11,outline=(255,114,14,240),width=8)
highlight=Image.alpha_composite(frame,glow.filter(ImageFilter.GaussianBlur(7)))
highlight=Image.alpha_composite(highlight,glow)
hd=ImageDraw.Draw(highlight)
hd.rounded_rectangle((5,4,frame.width-6,frame.height-5),radius=10,outline=(255,206,110,255),width=3)
highlight.save(ui/"creator_button_compact_active.png")
# Compact handle drawn at its native size, not resized from user-source art.
diamond=Image.new("RGBA",(38,44))
dd=ImageDraw.Draw(diamond)
dd.polygon([(19,1),(36,22),(19,43),(2,22)],fill="#b86612",outline="#ffe19a",width=2)
dd.polygon([(19,5),(31,22),(19,39),(7,22)],fill="#f7b92f",outline="#df8d15",width=2)
dd.polygon([(19,5),(19,39),(7,22)],fill="#ffdd69")
diamond.save(ui/"creator_slider_diamond_compact.png")
print("CREATOR_V205_ART_REPRODUCIBLE")
