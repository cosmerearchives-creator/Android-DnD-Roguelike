"""Blender smoke test: editable stylized High Elf model and four-direction idle bake.
Run: blender -b -t 2 --python blender/create_character.py -- --output build/blender-preview
No existing game artwork is overwritten.
"""
import bpy, math, sys, os
from mathutils import Vector

args = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
out = args[args.index("--output")+1] if "--output" in args else "build/blender-preview"
os.makedirs(out, exist_ok=True)
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

def material(name, color):
    m=bpy.data.materials.new(name); m.diffuse_color=(*color,1)
    m.use_nodes=True
    bs=m.node_tree.nodes.get("Principled BSDF")
    bs.inputs["Base Color"].default_value=(*color,1)
    bs.inputs["Roughness"].default_value=.84
    return m

skin=material("Warm elf skin",(.72,.46,.32))
hair=material("Coiled dark hair",(.13,.085,.075))
navy=material("Navy tunic",(.055,.10,.22))
gold=material("Gold trim",(.72,.45,.14))
leather=material("Brown leather",(.18,.10,.065))
eye=material("Eyes",(.08,.14,.17))

root=bpy.data.objects.new("CharacterRoot",None)
bpy.context.collection.objects.link(root)
def uv(name,loc,scale,mat):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=20,ring_count=12,location=loc)
    o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(mat)
    o.parent=root
    return o
def link(name,a,b,r,mat):
    mid=(Vector(a)+Vector(b))/2;d=Vector(b)-Vector(a)
    bpy.ops.mesh.primitive_cylinder_add(vertices=16,radius=r,depth=d.length,location=mid)
    o=bpy.context.object;o.name=name;o.rotation_euler=d.to_track_quat("Z","Y").to_euler()
    o.data.materials.append(mat);o.parent=root
    return o

uv("Torso",(0,0,1.42),(.32,.18,.43),navy)
uv("Hips",(0,0,1.04),(.29,.19,.18),navy)
uv("Neck",(0,0,1.84),(.105,.105,.15),skin)
uv("Head",(0,-.015,2.04),(.225,.20,.29),skin)
uv("Nose",(0,-.215,2.025),(.055,.065,.07),skin)
for side in (-1,1):
    x=side
    uv("Pointed ear "+str(x),(.25*x,0,2.05),(.17,.065,.07),skin)
    uv("Eye "+str(x),(.09*x,-.194,2.09),(.034,.019,.028),eye)
    link("Upper arm "+str(x),(.29*x,0,1.72),(.41*x,0,1.35),.105,skin)
    link("Sleeve "+str(x),(.29*x,0,1.72),(.34*x,0,1.55),.125,navy)
    link("Forearm "+str(x),(.41*x,0,1.35),(.40*x,-.035,1.05),.078,skin)
    uv("Hand "+str(x),(.40*x,-.035,1.015),(.083,.065,.11),skin)
    link("Thigh "+str(x),(.16*x,0,1.00),(.18*x,0,.56),.135,leather)
    link("Shin "+str(x),(.18*x,0,.56),(.18*x,-.01,.16),.095,skin)
    uv("Boot "+str(x),(.18*x,-.065,.14),(.125,.20,.15),leather)
    uv("Shoulder trim "+str(x),(.28*x,-.02,1.71),(.045,.17,.035),gold)
uv("Belt",(0,-.01,1.13),(.31,.195,.065),leather)
uv("Buckle",(0,-.205,1.13),(.07,.025,.055),gold)
for i in range(9):
    angle=2*math.pi*i/9
    uv("Hair curl %02d"%i,(.17*math.cos(angle),.12*math.sin(angle)+.02,2.27),(.105,.105,.12),hair)
uv("Hair crown",(0,.055,2.30),(.215,.19,.13),hair)

world=bpy.context.scene.world
world.color=(.32,.32,.32)
bpy.ops.object.light_add(type="AREA",location=(2,-4,6))
bpy.context.object.data.energy=600;bpy.context.object.data.shape="DISK";bpy.context.object.data.size=5
bpy.ops.object.camera_add(location=(3.5,-6,3.2))
camera=bpy.context.object
direction=Vector((0,0,1.15))-camera.location
camera.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
camera.data.type="ORTHO";camera.data.ortho_scale=2.85
scene=bpy.context.scene;scene.camera=camera
scene.render.engine="BLENDER_EEVEE_NEXT"
scene.render.resolution_x=168;scene.render.resolution_y=192;scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG";scene.render.film_transparent=True
scene.view_settings.view_transform="Standard"
# Save the editable source before rendering.
bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(os.path.join(out,"high_elf_prototype.blend")))
for facing in range(4):
    root.rotation_euler.z=facing*math.pi/2
    for frame in range(4):
        root.location.z=.012*math.sin(frame*math.pi/2)
        scene.render.filepath=os.path.abspath(os.path.join(out,"idle_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
print("BLENDER_CHARACTER_PIPELINE_OK",out)
