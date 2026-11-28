from pathlib import Path
from PIL import Image,ImageDraw,ImageFilter
root=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\creator_ui")
# Build crisp nine-slice style boxes at 160x160 out of original source art, rather than stretching adjacent decorations.
bar=Image.open(root/"creator_header_frame.png").convert("RGBA")
leather=Image.open(root/"creator_leather_tile.png").convert("RGBA")
def nine(src,left,right,top,bottom,w,h):
 out=Image.new("RGBA",(w,h))
 def paste(im,box,dest):
  crop=im.crop(box);dw=dest[2]-dest[0];dh=dest[3]-dest[1]
  if dw>0 and dh>0:out.paste(crop.resize((dw,dh),Image.Resampling.BICUBIC),dest[:2],crop.resize((dw,dh),Image.Resampling.BICUBIC))
 sw,sh=src.size
 cols=[(0,left,0,left),(left,sw-right,left,w-right),(sw-right,sw,w-right,w)]
 rows=[(0,top,0,top),(top,sh-bottom,top,h-bottom),(sh-bottom,sh,h-bottom,h)]
 for sx0,sx1,dx0,dx1 in cols:
  for sy0,sy1,dy0,dy1 in rows:
   paste(src,(sx0,sy0,sx1,sy1),(dx0,dy0,dx1,dy1))
 return out
# Keep the source-based carved-corner artwork centered and undistorted at native output size.
frame=nine(bar,54,54,32,32,420,100)
frame.save(root/"creator_button_clean.png")
# Reuse the same boundary but extend the real leather texture: original corners remain 54px each.
panel=Image.new("RGBA",(500,340),(0,0,0,0))
leather_big=Image.new("RGBA",(500,340))
for y in range(0,340,70):
 for x in range(0,500,70):leather_big.paste(leather,(x,y))
panel.alpha_composite(leather_big)
# Source-based warm bronze frame with bevel; source corners remain unscaled.
d=ImageDraw.Draw(panel)
for inset,color,width in ((1,(185,115,48,255),3),(7,(70,37,15,255),3),(12,(234,169,75,210),2)):
 d.rounded_rectangle((inset,inset,499-inset,339-inset),radius=15,outline=color,width=width)
for corner,original in [
 ((0,0),bar.crop((0,0,56,40))),((444,0),bar.crop((334,0,390,40))),
 ((0,300),bar.crop((0,43,56,83))),((444,300),bar.crop((334,43,390,83)))]:
 panel.alpha_composite(original,corner)
panel.save(root/"creator_panel_clean.png")
# Derive accurate arrow/diamond slider assets without introducing unrelated glyphs.
slider=Image.new("RGBA",(480,32))
draw=ImageDraw.Draw(slider)
draw.line((18,17,462,17),fill="#3a1605",width=10)
draw.line((18,15,462,15),fill="#e29a3a",width=5)
draw.line((18,12,462,12),fill="#ffda7c",width=2)
slider.save(root/"creator_slider_track.png")
diamond=Image.new("RGBA",(50,58))
draw=ImageDraw.Draw(diamond)
draw.polygon([(25,2),(46,28),(25,55),(4,28)],fill="#b86612",outline="#ffe19a",width=3)
draw.polygon([(25,8),(40,28),(25,49),(11,28)],fill="#f7b92f",outline="#df8d15",width=2)
draw.polygon([(25,8),(25,49),(11,28)],fill="#ffdd69")
diamond.save(root/"creator_slider_diamond.png")
print("CLEAN_SPRITES_READY")
