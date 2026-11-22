"""Blender smoke test: editable stylized High Elf model and four-direction idle bake.
Run: blender -b -t 2 --python blender/create_character.py -- --output build/blender-preview
No existing game artwork is overwritten.
"""
import bpy, math, sys, os
from mathutils import Vector

args = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
out = args[args.index("--output")+1] if "--output" in args else "build/blender-preview"
variant = args[args.index("--variant")+1] if "--variant" in args else "petite_pale"
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

skin=material("Pale elf skin",(.87,.73,.68))
hair=material("Pale silver-blonde hair",(.73,.70,.60))
navy=material("Deep blue tunic",(.075,.12,.23))
gold=material("Gold trim",(.72,.45,.14))
leather=material("Brown leather",(.18,.10,.065))
eye=material("Eyes",(.08,.14,.17))

root=bpy.data.objects.new("PetitePaleElfRoot",None)
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

uv("Waist",(0,0,1.33),(.205,.155,.25),navy)
uv("Ribcage",(0,0,1.55),(.29,.19,.28),navy)
uv("Bust left",(-.14,-.125,1.57),(.16,.13,.17),navy)
uv("Bust right",(.14,-.125,1.57),(.16,.13,.17),navy)
uv("Pelvis",(0,0,1.09),(.32,.21,.23),navy)
uv("Hip left",(-.18,0,1.07),(.18,.18,.18),navy)
uv("Hip right",(.18,0,1.07),(.18,.18,.18),navy)
uv("Neck",(0,0,1.84),(.105,.105,.15),skin)
uv("Head",(0,-.015,2.04),(.175,.158,.225),skin)
uv("Jaw",(0,-.08,1.92),(.124,.113,.095),skin)
uv("Nose",(0,-.167,2.015),(.027,.035,.045),skin)
for side in (-1,1):
    x=side
    uv("Pointed ear "+str(x),(.195*x,0,2.05),(.075,.045,.045),skin)
    uv("Eye "+str(x),(.075*x,-.163,2.085),(.016,.011,.014),eye)
    uv("Brow "+str(x),(.076*x,-.161,2.125),(.046,.012,.012),hair)
    link("Upper arm "+str(x),(.29*x,0,1.72),(.41*x,0,1.35),.105,skin)
    link("Sleeve "+str(x),(.29*x,0,1.72),(.34*x,0,1.55),.125,navy)
    link("Forearm "+str(x),(.41*x,0,1.35),(.40*x,-.035,1.05),.078,skin)
    uv("Hand "+str(x),(.40*x,-.035,1.015),(.083,.065,.11),skin)
    link("Thigh "+str(x),(.17*x,0,1.04),(.18*x,0,.57),.145,leather)
    uv("Upper leg contour "+str(x),(.17*x,0,.83),(.145,.14,.25),leather)
    link("Shin "+str(x),(.18*x,0,.56),(.18*x,-.01,.16),.105,skin)
    uv("Calf contour "+str(x),(.18*x,-.01,.37),(.108,.108,.21),skin)
    uv("Boot "+str(x),(.18*x,-.065,.14),(.125,.20,.15),leather)
    uv("Shoulder trim "+str(x),(.28*x,-.02,1.71),(.045,.17,.035),gold)
uv("Belt",(0,-.01,1.13),(.31,.195,.065),leather)
uv("Buckle",(0,-.205,1.13),(.07,.025,.055),gold)
for i in range(12):
    angle=2*math.pi*i/12
    uv("Hair strand %02d"%i,(.16*math.cos(angle),.115*math.sin(angle)+.025,2.245),(.072,.065,.12),hair)
uv("Hair crown",(0,.045,2.29),(.195,.18,.105),hair)
for side in (-1,1):
    uv("Side hair "+str(side),(.17*side,.045,1.99),(.07,.10,.29),hair)
uv("Back hair",(0,.12,1.96),(.18,.09,.34),hair)
# Smooth the intersecting primitive silhouette into a continuous surface.
# Keep materials grouped so clothing, skin and hair remain independently shaded.
for material_group in (navy, skin, leather):
    parts=[o for o in bpy.data.objects if o.type=="MESH" and o.data.materials and o.data.materials[0]==material_group]
    if len(parts)<2: continue
    bpy.ops.object.select_all(action="DESELECT")
    for o in parts:o.select_set(True)
    bpy.context.view_layer.objects.active=parts[0]
    bpy.ops.object.join()
    joined=bpy.context.object
    joined.name="Continuous "+material_group.name
    remesh=joined.modifiers.new("Blend overlapping anatomy","REMESH")
    remesh.mode="VOXEL";remesh.voxel_size=.032
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth=joined.modifiers.new("Soft organic silhouette","SMOOTH")
    smooth.factor=1.2;smooth.iterations=4
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    for polygon in joined.data.polygons:polygon.use_smooth=True

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
scene.render.engine="BLENDER_EEVEE"
scene.render.resolution_x=168;scene.render.resolution_y=192;scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG";scene.render.film_transparent=True
scene.view_settings.view_transform="Standard"
# Cache mesh vertices in root coordinates so the entire connected mesh can
# deform smoothly at hip, knee, ankle, shoulder and elbow while baking.
meshes=[]
for obj in bpy.data.objects:
    if obj.type!="MESH":continue
    to_root=obj.matrix_local.copy()
    from_root=to_root.inverted()
    points=[to_root @ v.co for v in obj.data.vertices]
    meshes.append((obj,points,from_root))

def pose(mode,phase):
    stride=math.sin(phase)
    counter=math.cos(phase)
    punch=max(0.0,math.sin(math.pi*phase))
    for obj,original,inverse in meshes:
        for vertex,base in zip(obj.data.vertices,original):
            p=base.copy()
            x,y,z=base.x,base.y,base.z
            if mode=="walk" and z<1.12 and abs(x)>.08:
                side=1 if x>0 else -1
                swing=math.sin(phase+(0 if side>0 else math.pi))
                lift=max(0.0,swing)
                stance=max(0.0,-swing)
                # The knee travels forward and upward during the swing phase.
                # The shin folds underneath it; the ankle then extends forward.
                if z>.55:
                    t=min(1.,max(0.,(1.10-z)/.53))
                    p.y-=.28*swing*t
                    p.z+=.065*lift*t-.022*stance*t
                else:
                    t=min(1.,max(0.,(.57-z)/.50))
                    p.y-=.28*swing+(.18*lift*t)
                    p.z+=.10*lift+(.17*lift*t)-.025*stance
            elif mode=="walk" and z>1.13 and z<1.85 and abs(x)>.27:
                side=1 if x>0 else -1
                p.y+=.17*math.sin(phase+(math.pi if side>0 else 0))
                p.z+=.018*math.cos(phase+(0 if side>0 else math.pi))
            elif mode=="punch" and x>.29 and .94<z<1.80:
                weight=min(1.,max(0.,(x-.29)/.13))
                p.y-=.63*punch*weight
                p.z+=.26*punch*weight
                p.x+=.055*punch*weight
            vertex.co=inverse @ p
        obj.data.update()

# The editable source contains the neutral continuous-mesh figure.
bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(os.path.join(out,"petite_pale_elf.blend")))
for facing in (0,1,2,3,4):
    root.rotation_euler.z=facing*math.pi/4
    for frame in range(1):
        pose("idle",0)
        root.location.z=.012*math.sin(frame*math.pi/2)
        scene.render.filepath=os.path.abspath(os.path.join(out,"idle_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
    for frame in range(8):
        phase=frame*math.tau/8
        pose("walk",phase)
        root.location.z=.025*abs(math.sin(phase))
        scene.render.filepath=os.path.abspath(os.path.join(out,"walk_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
    for frame in range(3):
        pose("punch",frame/4)
        root.location.z=0
        scene.render.filepath=os.path.abspath(os.path.join(out,"punch_f%d_%d.png"%(facing,frame)))
        bpy.ops.render.render(write_still=True)
print("BLENDER_CHARACTER_IDLE_WALK_KNEE_PUNCH_OK",out)
