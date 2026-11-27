from pathlib import Path
from PIL import Image,ImageDraw
root=Path(r"G:/ChatGPT Projects/3d Converter/ForestsOfHellSource/godot")
def render(active):
    w,h=256,128
    im=Image.new("RGBA",(w,h),(0,0,0,0))
    dr=ImageDraw.Draw(im)
    def poly(inset,cut):
        return [(inset+cut,inset),(w-inset-cut-1,inset),(w-inset-1,inset+cut),(w-inset-1,h-inset-cut-1),(w-inset-cut-1,h-inset-1),(inset+cut,h-inset-1),(inset,h-inset-cut-1),(inset,inset+cut)]
    dr.polygon(poly(1,22),fill=(40,78,79,255) if active else (9,43,48,255))
    dr.line(poly(1,22)+[poly(1,22)[0]],fill=(250,216,157,255) if active else (207,181,118,255),width=3)
    dr.line(poly(8,19)+[poly(8,19)[0]],fill=(218,186,122,255) if active else (137,120,78,255),width=2)
    im.save(root/("inventory_frame_active.png" if active else "inventory_frame.png"),optimize=True)
render(False);render(True)
print("BEVELED_FRAMES_READY")
