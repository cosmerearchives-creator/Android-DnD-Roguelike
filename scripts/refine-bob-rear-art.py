#!/usr/bin/env python3
"""Precisely retouch only Tawny Bob's rear shoulder gold piping.

Runs on an isolated branch. Starts from the verified main-branch atlas.
No source sprite registration, shape, opacity, or other artwork is changed.
"""
from pathlib import Path
import hashlib
import numpy as np
from scipy.ndimage import distance_transform_edt
import cv2
from PIL import Image, ImageDraw, ImageChops

original_path=Path("/tmp/bob-rear-original.webp")
asset=Path("app/src/main/assets/elf_roster/bob/2.webp")
original=Image.open(original_path).convert("RGBA")
assert original.size==(800,400)
arr=np.array(original)
alpha=arr[:,:,3]
# Tight, image-specific polygons tracing the two unneeded vertical garment
# piping strips; confirmed from 5x diagnostic crop of original rear arms.
polygons=(
 ((368,10),(380,10),(388,22),(390,43),(388,59),(381,75),(371,79),(366,62),(368,35)),
 ((445,9),(460,11),(463,23),(459,45),(459,64),(449,78),(439,70),(438,48),(440,22))
)
region=Image.new("L",(800,400),0); d=ImageDraw.Draw(region)
for points in polygons:d.polygon(points,fill=255)
region=np.array(region)>0
# Keep all alpha values, including antialiasing, exactly as authored.
mask=region&(alpha>=80)
# Prevent transparent black around the silhouette from bleeding into skin.
valid=alpha>=200
nearest=distance_transform_edt(~valid,return_distances=False,return_indices=True)
prepared=arr[:,:,:3][tuple(nearest)].astype(np.uint8)
# Only masked upper-arm pixels get the new RGB; surrounding shading stays exact.
inpaint=cv2.inpaint(prepared,mask.astype(np.uint8)*255,5,cv2.INPAINT_TELEA)
result=arr.copy()
result[mask,:3]=inpaint[mask,:3]
assert np.array_equal(result[:,:,3],arr[:,:,3])
actual=np.any(result!=arr,axis=2)
assert np.any(actual) and np.all(~actual|region)
# Shoulder joints, elbows, and wrists are never repainted or moved.
for x,y in ((358,41),(471,41),(342,127),(491,127),(336,202),(498,202)):
 assert np.array_equal(result[y,x],arr[y,x]),("registration changed",x,y)
out=Image.fromarray(result,"RGBA")
tmp=Path("build/bob-rear-refined");tmp.mkdir(parents=True,exist_ok=True)
dest=tmp/"2.webp"
out.save(dest,format="WEBP",lossless=True,quality=100,method=6,exact=True)
decoded=Image.open(dest).convert("RGBA")
assert decoded.tobytes()==out.tobytes()
review=Path(".art-review")
review.mkdir(exist_ok=True)
for name,x0 in (("arm0_refined",288),("arm1_refined",416)):
 b=original.crop((x0,0,x0+128,145)).resize((640,725))
 a=decoded.crop((x0,0,x0+128,145)).resize((640,725))
 combined=Image.new("RGBA",(1280,725),(0,0,0,0))
 combined.paste(b,(0,0));combined.paste(a,(640,0))
 combined.save(review/(name+"_before_after.png"))
changed=int(actual.sum())
assert 200<changed<3500,changed
print("RETOUCH V2 CHANGED PIXELS:",changed)
print("MASK POLYGONS:",polygons)
print("OUTPUT SHA256:",hashlib.sha256(dest.read_bytes()).hexdigest())
