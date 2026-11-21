#!/usr/bin/env python3
"""Tawny Bob rear upper-arm garment trim: texture-clone skin retouch test."""
from pathlib import Path
import hashlib
import numpy as np
from scipy.ndimage import distance_transform_edt
import cv2
from PIL import Image,ImageDraw

original=Image.open("/tmp/bob-rear-original.webp").convert("RGBA")
assert original.size==(800,400)
arr=np.array(original)
alpha=arr[:,:,3]
skin=alpha>=195
nearest=distance_transform_edt(~skin,return_distances=False,return_indices=True)
# Use only original opaque skin texture as donor. Never source previous edits.
validRGB=arr[:,:,:3][tuple(nearest)]
# Surface texture donor taken 24 pixels TOWARD center of each shoulder, away
# from decorative piping, with gentle feather across patch boundaries.
regions=[
 ([(366,9),(379,9),(390,19),(390,44),(388,64),(378,80),(366,77),(363,57)],-23),
 ([(447,8),(461,11),(464,28),(461,52),(460,65),(450,81),(437,78),(435,47),(438,21)],24)
]
result=arr.copy()
total=0
for polygon,donorShift in regions:
 mimg=Image.new("L",(800,400),0)
 ImageDraw.Draw(mimg).polygon(polygon,fill=255)
 region=np.asarray(mimg)>0
 included=region&(alpha>=80)
 dist=cv2.distanceTransform(included.astype(np.uint8),cv2.DIST_L2,3)
 weight=np.clip(dist/3,0,1)*.95
 ys,xs=np.where(included)
 for y,x in zip(ys,xs):
  sx=max(0,min(799,x+donorShift))
  # nearest opaque-skin coordinates if donor is antialiased/transparent.
  if alpha[y,sx]<195:
    sy2,sx2=nearest[0,y,sx],nearest[1,y,sx]
  else:
    sy2,sx2=y,sx
  donor=validRGB[sy2,sx2]
  w=weight[y,x]
  result[y,x,:3]=np.clip(np.round((1-w)*arr[y,x,:3]+w*donor),0,255).astype(np.uint8)
  total+=1
assert np.array_equal(result[:,:,3],arr[:,:,3])
changed=np.any(result!=arr,axis=2)
assert np.any(changed)
for x,y in ((358,41),(471,41),(342,127),(491,127),(336,202),(498,202)):
 assert np.array_equal(result[y,x],arr[y,x]),(x,y)
out=Image.fromarray(result,"RGBA")
dir=Path("build/bob-rear-refined");dir.mkdir(parents=True,exist_ok=True)
dest=dir/"2.webp";out.save(dest,"WEBP",lossless=True,quality=100,method=6,exact=True)
assert Image.open(dest).convert("RGBA").tobytes()==out.tobytes()
prev=Path(".art-review");prev.mkdir(exist_ok=True)
for name,x0 in (("arm0_v3",288),("arm1_v3",416)):
 b=original.crop((x0,0,x0+128,145)).resize((640,725))
 a=out.crop((x0,0,x0+128,145)).resize((640,725))
 canvas=Image.new("RGBA",(1280,725))
 canvas.paste(b,(0,0));canvas.paste(a,(640,0))
 canvas.save(prev/(name+"_before_after.png"))
print("V3 pixels changed:",int(changed.sum()),"candidate sha256:",hashlib.sha256(dest.read_bytes()).hexdigest())
