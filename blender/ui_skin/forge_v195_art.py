from PIL import Image,ImageDraw,ImageFilter,ImageFont
from pathlib import Path
import math,random
out=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\ui_skin")
out.mkdir(exist_ok=True)
R=random.Random(712712)
# Replace naive stretched wood-grain with carved black leather that maintains
# a coherent scale across all crops. Every pixel is opaque except independent icon overlays.
SIZE=512
def leather(dark=1):
 im=Image.new("RGBA",(SIZE,SIZE),(25,18,13,255));p=im.load()
 rng=random.Random(761)
 for y in range(SIZE):
  for x in range(SIZE):
   noise=rng.randint(-5,5)
   a=math.sin(x*.044+y*.012)*1.9+math.sin(y*.068-x*.031)*1.6
   base=noise+a
   p[x,y]=(max(6,int((31+base)*dark)),max(5,int((23+base*.75)*dark)),max(4,int((18+base*.65)*dark)),255)
 pen=ImageDraw.Draw(im,'RGBA')
 # Small irregular leather scales/tooled floral tooling, avoiding horizontal wood streaks.
 for row in range(3,510,20):
  for x in range(6+(10 if row//20%2 else 0),510,23):
   q=random.Random(x*10003+row*317)
   xx=x+q.randrange(-3,4);yy=row+q.randrange(-3,4)
   pen.arc((xx-9,yy-7,xx+9,yy+8),8,168,fill=(124,88,47,27),width=1)
   pen.arc((xx-10,yy-5,xx+10,yy+10),191,347,fill=(6,3,2,40),width=1)
 return im

def point(x,y):return(int(x),int(y))
def filigree(d,cx,cy,sx=1,sy=1,scale=1,bright=False):
 def tr(x,y):return (int(cx+x*sx*scale),int(cy+y*sy*scale))
 gold=(243,191,112,255) if bright else (196,145,87,255)
 lo=(80,43,24,255)
 # elaborate forged copper curling vines, a nested acanthus-style scroll.
 paths=[
  [(0,57),(2,23),(5,13),(13,5),(24,3),(54,2)],
  [(12,49),(13,28),(18,20),(27,16),(37,18),(40,25),(37,30),(30,29),(29,25)],
  [(20,47),(25,35),(37,33),(45,26),(47,17),(52,14),(59,17)],
  [(35,13),(45,11),(48,7),(45,3)],
  [(7,52),(15,50),(20,57)],
  [(26,20),(29,13),(37,10)]
 ]
 for path in paths:
  xy=[tr(*p) for p in path]
  d.line(xy,fill=lo,width=max(2,int(5*scale)),joint='curve')
  d.line(xy,fill=gold,width=max(1,int(2*scale)),joint='curve')
 for xx,yy,rr in [(13,13,5),(29,29,3),(42,19,3)]:
  x,y=tr(xx,yy);r=max(2,int(rr*scale))
  d.ellipse((x-r,y-r,x+r,y+r),fill=(32,16,8,255),outline=(240,183,98,255),width=2)
 for xx,yy in [(19,41),(36,33)]:
  xy=[tr(xx,yy-6),tr(xx+5,yy),tr(xx,yy+6),tr(xx-5,yy)]
  d.polygon(xy,fill=(110,57,31,255))
  d.line(xy+[xy[0]],fill=gold,width=1)

def panel_art(active=False):
 im=leather()
 d=ImageDraw.Draw(im,'RGBA')
 # triple-layer embossed frame, highlights on upper/left, tarnished bronze inside
 d.rectangle((2,2,509,509),outline=(9,5,3,255),width=13)
 d.rectangle((8,8,503,503),outline=(78,44,26,255),width=8)
 d.rectangle((14,14,497,497),outline=(201,154,92,255),width=5)
 d.rectangle((21,21,490,490),outline=(41,20,10,255),width=7)
 d.rectangle((29,29,482,482),outline=(128,85,43,255),width=2)
 d.line((48,14,464,14),fill=(245,197,120,220),width=3)
 d.line((14,48,14,463),fill=(231,177,101,215),width=2)
 d.line((47,498,464,498),fill=(60,33,20,255),width=3)
 d.line((498,47,498,464),fill=(65,35,16,255),width=3)
 for sx in [1,-1]:
  for sy in [1,-1]:
   cx=3 if sx==1 else 509;cy=3 if sy==1 else 509
   filigree(d,cx,cy,sx,sy,scale=1.1,bright=active)
 # midpoint forged brackets and decorative bronze rivets
 for p in [(256,12),(256,500),(12,256),(500,256)]:
  x,y=p
  d.ellipse((x-4,y-4,x+4,y+4),fill=(42,20,10,255),outline=(242,179,92,255),width=2)
 return im

def slot_art(active=False):
 im=leather(.93)
 d=ImageDraw.Draw(im,"RGBA")
 d.rounded_rectangle((5,5,506,506),radius=35,fill=(9,6,5,80),outline=(13,8,6,255),width=22)
 d.rounded_rectangle((15,15,495,495),radius=31,outline=(132,87,43,255),width=12)
 d.rounded_rectangle((22,22,489,489),radius=29,outline=(224,174,102,255),width=5)
 d.rounded_rectangle((38,38,473,473),radius=24,outline=(54,33,20,255),width=12)
 d.rounded_rectangle((45,45,466,466),radius=20,outline=(115,73,41,255),width=3)
 for sx in [1,-1]:
  for sy in [1,-1]:
   x=21 if sx==1 else 491;y=21 if sy==1 else 491
   d.ellipse((x-8,y-8,x+8,y+8),fill=(31,13,8,255),outline=(245,179,94,255),width=3)
   x=57 if sx==1 else 455;y=57 if sy==1 else 455
   d.line([(x-8,y),(x,y-8),(x+8,y),(x,y+8),(x-8,y)],fill=(209,153,83,255),width=3)
 if active:
  d.rounded_rectangle((52,52,459,459),radius=16,outline=(255,166,33,255),width=13)
  d.rounded_rectangle((66,66,445,445),radius=15,outline=(255,205,94,200),width=4)
 return im

def button_art(active=False,tab=False):
 im=leather(1.08 if active else .95)
 d=ImageDraw.Draw(im,'RGBA')
 d.polygon([(16,18),(496,18),(510,54),(510,458),(491,496),(22,496),(2,459),(2,56)],fill=(18,10,7,255))
 d.line([(22,14),(488,14),(504,32),(504,480),(488,500),(22,500),(8,481),(8,35),(22,14)],fill=(35,20,12,255),width=19)
 d.line([(22,22),(488,22),(494,38),(494,473),(480,490),(32,490),(18,470),(18,43),(22,22)],fill=(214,152,81,255),width=7,joint='curve')
 d.line([(39,34),(465,34)],fill=(248,200,118,255),width=3)
 d.line([(40,479),(465,479)],fill=(55,32,18,255),width=5)
 d.line([(33,59),(33,449)],fill=(198,137,70,255),width=2)
 for cx in [28,482]:
  for cy in [35,479]:
   d.ellipse((cx-9,cy-9,cx+9,cy+9),fill=(25,13,9,255),outline=(226,168,89,255),width=3)
 if active:
  d.line([(44,465),(468,465)],fill=(255,180,47,255),width=13)
  d.line([(47,458),(462,458)],fill=(255,226,112,255),width=3)
  if tab: d.rectangle((46,52,468,446),outline=(246,160,38,215),width=4)
 return im

for name in ['panel','slot','button','tab']:
 for active in [False,True]:
  im=panel_art(active) if name=='panel' else slot_art(active) if name=='slot' else button_art(active,name=='tab')
  target=out/(name+('_active' if active else '')+'.png')
  im.save(target,optimize=False)
  print('FORGED',target.name,target.stat().st_size)
panel_art().save(out/'header.png',optimize=False)

# Photo-realistic-style stone stage, intentionally textured; does not replace player GLB.
pw,ph=650,1060
back=Image.new('RGBA',(pw,ph))
pix=back.load();rng=random.Random(7326)
for y in range(ph):
 for x in range(pw):
  radial=((x-pw*.5)/(pw*.73))**2+((y-ph*.48)/(ph*.82))**2
  n=rng.randrange(-8,9)
  glow=33*math.exp(-radial*2.5)
  grey=int(34+glow-12*radial+n)
  pix[x,y]=(max(10,grey),max(10,grey-3),max(10,grey-5),255)
d=ImageDraw.Draw(back,'RGBA')
for row in range(6):
 yy=806+row*43
 for col in range(12):
  xx=col*68-(36 if row%2 else 0)+rng.randrange(-8,8)
  d.polygon([(xx,yy),(xx+60,yy+rng.randrange(-5,5)),(xx+74,yy+35),(xx+4,yy+33)],fill=(86,81,70,70),outline=(12,12,10,100),width=2)
# multi-tier stone dais (perspective ellipses)
for bounds,fill,edge in [
 ((82,928,578,1029),(53,48,43,255),(22,18,13,255)),
 ((67,894,586,1002),(74,70,61,255),(32,27,23,255)),
 ((88,873,559,956),(97,89,77,255),(39,35,31,255)),
 ((104,856,543,925),(121,109,93,255),(56,47,37,255))]:
 d.ellipse(bounds,fill=fill,outline=edge,width=7)
# cobblestone top, naturally irregular joints
for i in range(90):
 ang=rng.uniform(0,math.tau);r=rng.random()**.5
 x=325+206*r*math.cos(ang);y=889+29*r*math.sin(ang)
 pts=[(x+11*math.cos(ang+k*math.tau/6),y+5*math.sin(ang+k*math.tau/6)) for k in range(6)]
 d.line(pts+[pts[0]],fill=(50,45,38,150),width=1)
back.save(out/'portrait_backdrop.png',optimize=False)
print('STONE_STAGE',back.size)
