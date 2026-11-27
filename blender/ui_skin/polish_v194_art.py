"""Ornamental, softly aged bronze/leather UI skin, source assets editable.
Uses RGBA PNG at 256px so 9-slice corners retain exact shape.
No reference screenshot is baked into an interaction layer.
"""
from PIL import Image, ImageDraw, ImageFilter
from pathlib import Path
from random import Random
import math
out=Path(__file__).resolve().parents[2]/"godot"/"ui_skin"
out.mkdir(parents=True,exist_ok=True)
W=256
def leather(seed, light=1.0):
 rng=Random(seed); im=Image.new("RGBA",(W,W)); p=im.load()
 for y in range(W):
  for x in range(W):
   n=rng.randrange(-8,9)
   grain=int(3*math.sin(x*.12+y*.033)+4*math.sin(y*.065-x*.018))
   base=int(n+grain)
   p[x,y]=(int(max(5,(27+base)*light)),int(max(5,(18+base*.8)*light)),int(max(4,(13+base*.6)*light)),255)
 d=ImageDraw.Draw(im,"RGBA")
 for y in range(6,254,17):
  for x in range(4+(9 if y//17%2 else 0),254,21):
   j=rng.randint(-3,3)
   d.arc((x-11,y-9+j,x+11,y+9+j),190,345,fill=(110,73,41,16),width=1)
   d.arc((x-10,y-8+j,x+10,y+8+j),20,165,fill=(4,2,1,24),width=1)
 return im

def decorative_corner(pen,corner,active):
 # Actual ornamental bronze corner on dark leather rather than triangular bevel.
 xx,yy,sx,sy=corner
 def pt(dx,dy):return (int(xx+sx*dx),int(yy+sy*dy))
 hi=(248,187,100,255) if active else (198,143,74,255)
 shadow=(67,36,19,255)
 pen.line([pt(0,32),pt(0,8),pt(8,0),pt(32,0)],fill=shadow,width=8)
 pen.line([pt(0,32),pt(0,8),pt(8,0),pt(32,0)],fill=hi,width=3)
 pen.line([pt(9,26),pt(9,17),pt(14,11),pt(23,11)],fill=shadow,width=6)
 pen.line([pt(9,26),pt(9,17),pt(14,11),pt(23,11)],fill=hi,width=2)
 pen.line([pt(14,22),pt(19,19),pt(24,21)],fill=(232,176,93,255),width=2)
 cx,cy=pt(12,12)
 pen.ellipse((cx-4,cy-4,cx+4,cy+4),fill=(27,15,9,255),outline=(247,195,107,255),width=2)
 # Copper-set tiny diamond
 coords=[pt(24,2),pt(28,6),pt(24,10),pt(20,6)]
 pen.polygon(coords,fill=(133,72,30,255))
 pen.line(coords+[coords[0]],fill=hi,width=1)

def art(kind,active):
 base=leather(len(kind)*123+int(active)*31)
 d=ImageDraw.Draw(base,"RGBA")
 border=(225,172,93,255) if active else (164,112,61,255)
 pale=(255,208,116,255) if active else (201,148,78,255)
 dark=(28,12,8,255)
 # Instead of the thick double-bordered quadrilateral, draw finer embossed
 # bronze rails with inlaid black channels, directional glints, and small scrolls.
 if kind=='panel':
  d.rectangle((3,3,252,252),outline=(14,8,5,255),width=7)
  d.rectangle((7,7,248,248),outline=border,width=3)
  d.rectangle((13,13,242,242),outline=(51,28,16,255),width=4)
  d.rectangle((18,18,237,237),outline=(144,97,50,255),width=1)
  d.line([(35,9),(220,9)],fill=(250,206,124,220),width=1)
  d.line([(9,35),(9,220)],fill=(212,153,75,210),width=1)
  d.line([(35,247),(220,247)],fill=(72,39,22,255),width=2)
  d.line([(247,35),(247,220)],fill=(68,34,16,255),width=2)
  for q in [(4,4,1,1),(251,4,-1,1),(4,251,1,-1),(251,251,-1,-1)]:
   decorative_corner(d,q,active)
 elif kind=='slot':
  d.rounded_rectangle((4,4,251,251),radius=8,outline=(11,7,5,255),width=8)
  d.rounded_rectangle((8,8,247,247),radius=7,outline=pale,width=3)
  d.rounded_rectangle((14,14,241,241),radius=5,outline=(51,31,19,255),width=3)
  d.rounded_rectangle((19,19,236,236),radius=4,outline=(126,78,41,255),width=1)
  for xx in [14,242]:
   for yy in [14,242]:
    d.ellipse((xx-3,yy-3,xx+3,yy+3),fill=(16,8,4,255),outline=(206,153,87,255),width=1)
  if active:
   d.rounded_rectangle((20,20,235,235),radius=5,outline=(255,178,35,255),width=4)
 elif kind in ('button','tab'):
  vert=5
  d.rounded_rectangle((4,vert,251,255-vert),radius=10,fill=(24,15,10,120),outline=(12,7,5,255),width=7)
  d.rounded_rectangle((8,vert+4,247,251-vert),radius=8,outline=pale,width=3)
  d.rounded_rectangle((14,vert+9,241,246-vert),radius=6,outline=(96,60,32,255),width=2)
  d.line([(22,vert+9),(235,vert+9)],fill=(253,210,131,210),width=1)
  d.line([(22,245-vert),(235,245-vert)],fill=(43,23,12,255),width=2)
  for xx in [17,239]:
   for yy in [vert+16,240-vert]:
    d.ellipse((xx-4,yy-4,xx+4,yy+4),fill=(42,21,11,255),outline=(234,175,98,255),width=2)
  if active:
   d.rounded_rectangle((21,vert+16,234,239-vert),radius=4,outline=(255,186,50,220),width=3)
 elif kind=='header':
  d.rectangle((4,19,252,240),outline=(16,10,8,255),width=7)
  d.rectangle((7,22,249,237),outline=border,width=3)
  d.rectangle((16,31,240,229),outline=(104,71,36,255),width=1)
 return base

for k in ['panel','slot','button','tab','header']:
 for active in ([False,True] if k!='header' else [False]):
  f=out/(k+('_active' if active else '')+'.png')
  art(k,active).save(f,optimize=False)
  print(f.name,f.stat().st_size)

# Character portrait stage with subtle charcoal haze and broken paving stones.
rng=Random(771)
im=Image.new("RGBA",(380,620))
pix=im.load()
for y in range(620):
 for x in range(380):
  r=math.hypot((x-190)/270,(y-260)/460)
  n=rng.randint(-9,9)
  haze=26*math.exp(-((x-192)**2/(245**2)+(y-245)**2/(355**2)))
  v=int(28+haze-15*r+n)
  pix[x,y]=(max(9,v+7),max(10,v+5),max(9,v+1),255)
d=ImageDraw.Draw(im,'RGBA')
for row in range(4):
 yy=478+row*46
 for col in range(7):
  offset=28 if row%2 else -5
  x=col*63+offset+rng.randint(-5,5)
  d.line([(x,yy),(x+49,yy+rng.randint(-5,5)),(x+60,yy+32)],fill=(10,8,6,140),width=2)
  d.line([(x+1,yy+1),(x+48,yy+2)],fill=(120,103,84,95),width=1)
d.ellipse((55,530,324,586),fill=(55,48,39,255),outline=(129,99,65,255),width=4)
d.ellipse((82,540,297,575),outline=(190,145,83,255),width=2)
im.save(out/'portrait_backdrop.png')
print('portrait backdrop written')
