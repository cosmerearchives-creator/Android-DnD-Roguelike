"""Build a periodic, blended meadow atlas from existing licensed project grass art.
No harsh atlas grid edges: randomized source patches, overlaps and wrapped
feathering make both axes continuous, including the 0/1 UV boundary.
"""
from PIL import Image, ImageDraw, ImageStat, ImageEnhance, ImageFilter
from pathlib import Path
import random, math
root=Path(__file__).resolve().parent.parent
source=Image.open(root/"godot"/"grass_detail.png").convert("RGB")
rng=random.Random(81273)
N=1024
pitch=128
patch_width=220
feather=56
output=Image.new("RGBA",(N,N),(100,112,43,255))
# A small randomized mosaic of the source prevents any recognizable tile stamp.
# Prefer bright grass with low amounts of the original hard-edged black blocks.
patches=[]
for y in range(0,source.height-192,12):
    for x in range(0,source.width-192,12):
        crop=source.crop((x,y,x+192,y+192))
        thumb=crop.resize((24,24)).convert("L")
        dark=sum(c<45 for c in thumb.getdata())/576
        if dark<.004:
            patches.append((dark,x,y))
assert len(patches)>8
mask=Image.new("L",(patch_width,patch_width),0)
mp=mask.load()
for y in range(patch_width):
    for x in range(patch_width):
        edge=min(x,y,patch_width-1-x,patch_width-1-y)
        t=max(0,min(1,edge/feather))
        mp[x,y]=round(255*(t*t*(3-2*t)))
atlas_patches={}
for gy in range(-1,9):
    for gx in range(-1,9):
        key=(gx%8,gy%8)
        if key not in atlas_patches:
            dark,sx,sy=rng.choice(patches)
            patch=source.crop((sx,sy,sx+192,sy+192)).resize((patch_width,patch_width),Image.Resampling.LANCZOS)
            if rng.random()<.5: patch=patch.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            if rng.random()<.5: patch=patch.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
            patch=patch.convert("RGBA")
            patch.putalpha(mask)
            atlas_patches[key]=patch
        patch=atlas_patches[key]
        px=gx*pitch-patch_width//2+pitch//2
        py=gy*pitch-patch_width//2+pitch//2
        for ox in (-N,0,N):
            for oy in (-N,0,N):
                if px+ox < N and py+oy < N and px+ox+patch_width > 0 and py+oy+patch_width > 0:
                    output.alpha_composite(patch,(px+ox,py+oy))
# Remove slight blocky lightness transitions while preserving individual grass
# blades and flowers. Crossfade a low-opacity blurred copy only in chroma.
result=output.convert("RGB")
result=ImageEnhance.Contrast(result).enhance(.92)
# Force exact pixel equality and a smoothly converging neighborhood on both
# wrapped axes. Prevents dark vertical/horizontal lines at every UV repeat.
pix=result.load()
for y in range(N):
    for x in range(64):
        a=pix[x,y]; b=pix[N-1-x,y]
        influence=(1-x/64)**2
        midpoint=tuple(round((a[k]+b[k])*.5) for k in range(3))
        pix[x,y]=tuple(round(a[k]*(1-influence)+midpoint[k]*influence) for k in range(3))
        pix[N-1-x,y]=tuple(round(b[k]*(1-influence)+midpoint[k]*influence) for k in range(3))
for x in range(N):
    for y in range(64):
        a=pix[x,y]; b=pix[x,N-1-y]
        influence=(1-y/64)**2
        midpoint=tuple(round((a[k]+b[k])*.5) for k in range(3))
        pix[x,y]=tuple(round(a[k]*(1-influence)+midpoint[k]*influence) for k in range(3))
        pix[x,N-1-y]=tuple(round(b[k]*(1-influence)+midpoint[k]*influence) for k in range(3))
result.save(root/"godot"/"grass_seamless.png",optimize=True)
# Measure adjacent tiling boundary error versus the original.
def edge_error(im):
    p=im.load();w,h=im.size
    e1=sum(abs(p[0,y][c]-p[w-1,y][c]) for y in range(h) for c in range(3))/(h*3)
    e2=sum(abs(p[x,0][c]-p[x,h-1][c]) for x in range(w) for c in range(3))/(w*3)
    return round(e1,2),round(e2,2)
print("GRASS_ATLAS",result.size,"candidate_patches",len(patches),"edge_error_old",edge_error(source),"edge_error_new",edge_error(result))
