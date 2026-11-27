from PIL import Image,ImageDraw
from pathlib import Path
out=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\ui_skin")
size=512
base=Image.open(out/"panel.png").convert("RGBA")
def style(kind,active):
 im=base.crop((140,140,372,372)).resize((size,size))
 d=ImageDraw.Draw(im)
 # True four-way nine-slice texture: all border art lives within the outer 32 px.
 # The middle 448px is plain dark leather; corners never enter scalable center.
 if kind=="slot":
  corner=29; n=7
 else:
  corner=27;n=6
 gold=(242,185,95,255) if active else (177,127,67,255)
 d.rectangle((3,3,508,508),outline=(12,8,6,255),width=11)
 d.rectangle((9,9,502,502),outline=gold,width=n)
 d.rectangle((20,20,491,491),outline=(47,29,19,255),width=5)
 d.rectangle((25,25,486,486),outline=(113,76,40,255),width=2)
 d.line((34,11,478,11),fill=(245,199,112,255),width=2)
 for xx in [14,498]:
  for yy in [14,498]:
   d.ellipse((xx-5,yy-5,xx+5,yy+5),fill=(24,14,9,255),outline=(236,176,93,255),width=2)
 if active:
  d.rectangle((32,32,479,479),outline=(255,169,36,255),width=6)
 return im
for kind in ('slot','button','tab'):
 for active in (False,True):
  style(kind,active).save(out/(kind+('_active' if active else '')+'.png'))
print("NINE_SLICE_COMPACT_CONTROLS_RECTANGULAR")
