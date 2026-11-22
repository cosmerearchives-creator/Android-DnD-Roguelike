"""Bake matching isometric terrain and scenery from actual Blender geometry.
The camera and palette are shared across every tile and prop.
"""
import bpy, math, os, sys
from mathutils import Vector

args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
out=args[args.index("--output")+1] if "--output" in args else "build/blender-world"
os.makedirs(out,exist_ok=True)
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

def mat(name,rgb):
    m=bpy.data.materials.new(name)
    m.diffuse_color=(*rgb,1)
    m.use_nodes=True
    bs=m.node_tree.nodes.get("Principled BSDF")
    bs.inputs["Base Color"].default_value=(*rgb,1)
    bs.inputs["Roughness"].default_value=.93
    return m
grass=mat("meadow grass",(.24,.39,.19))
grass2=mat("deep grass",(.18,.30,.16))
soil=mat("earth",(.31,.23,.15))
sand=mat("sand",(.59,.49,.31))
stone=mat("stone",(.39,.42,.40))
snow=mat("frost",(.78,.80,.76))
water=mat("river",(.13,.36,.44))
leaf=mat("pine foliage",(.10,.27,.17))
leaf2=mat("leaf canopy",(.23,.42,.22))
wood=mat("bark",(.27,.16,.09))
roof=mat("slate roof",(.23,.30,.37))
wall=mat("plaster",(.63,.57,.44))
gold=mat("amber trim",(.64,.42,.17))

def cube(name,loc,scale,material,bevel=0):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc)
    o=bpy.context.object;o.name=name;o.dimensions=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(material)
    if bevel:
        mod=o.modifiers.new("soft edges","BEVEL");mod.width=bevel;mod.segments=2
        o.modifiers.new("weighted normals","WEIGHTED_NORMAL")
    return o
def cone(name,loc,r1,r2,depth,material,verts=12):
    bpy.ops.mesh.primitive_cone_add(vertices=verts,radius1=r1,radius2=r2,depth=depth,location=loc)
    o=bpy.context.object;o.name=name;o.data.materials.append(material)
    return o
def sphere(name,loc,scale,material):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=12,ring_count=8,location=loc)
    o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(material)
    return o
def clear():
    for o in list(bpy.data.objects):
        if o.type=="MESH":bpy.data.objects.remove(o,do_unlink=True)
def ground(material):
    cube("ground block",(0,0,-.09),(1.01,1.01,.18),soil)
    cube("ground surface",(0,0,.006),(1.012,1.012,.03),material)
def details(seed,material):
    for i in range(5):
        x=(((i*37+seed*11)%83)/83-.5)*.78
        y=(((i*29+seed*17)%79)/79-.5)*.78
        sphere("ground detail",(x,y,.038),(.035,.025,.018),material)
def tree(pine=False):
    cone("trunk",(0,0,.56),.095,.065,1.1,wood)
    if pine:
        for z,r in ((.85,.45),(1.13,.36),(1.4,.26)):
            cone("pine needles",(0,0,z),r,0,.75,leaf)
    else:
        for x,y,z,r in ((0,0,1.35,.50),(-.25,0,1.22,.32),(.24,.04,1.24,.34)):
            sphere("canopy",(x,y,z),(r,r*.8,r*.72),leaf2)
def boulder():
    sphere("granite",(0,0,.30),(.37,.31,.31),stone)
    sphere("moss",(-.13,-.07,.49),(.13,.12,.055),grass2)
def house():
    cube("stone foundation",(0,0,.12),(.8,.72,.24),stone)
    cube("cottage",(0,0,.59),(.75,.68,.72),wall)
    roof1=cube("roof left",(-.21,0,1.03),(.55,.82,.12),roof)
    roof1.rotation_euler[1]=-.50
    roof2=cube("roof right",(.21,0,1.03),(.55,.82,.12),roof)
    roof2.rotation_euler[1]=.50
    cube("door",(0,-.353,.45),(.20,.045,.42),wood)
    cube("window",(-.24,-.356,.70),(.13,.05,.14),gold)
def obelisk():
    cube("waystone",(0,0,.46),(.30,.29,.92),stone,bevel=.04)
    cone("spire",(0,0,1.02),.24,0,.34,stone,4)
    sphere("rune",(0,-.155,.65),(.065,.018,.12),gold)

scene=bpy.context.scene
scene.render.engine="BLENDER_EEVEE"
scene.render.resolution_x=256
scene.render.resolution_y=256
scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG"
scene.render.film_transparent=True
scene.view_settings.view_transform="Standard"
scene.world.color=(.25,.25,.25)
bpy.ops.object.light_add(type="AREA",location=(-3,-4,8))
bpy.context.object.data.energy=750
bpy.context.object.data.size=5
bpy.ops.object.camera_add(location=(5,-7,6))
cam=bpy.context.object
cam.rotation_euler=(Vector((0,0,.10))-cam.location).to_track_quat("-Z","Y").to_euler()
cam.data.type="ORTHO"
cam.data.ortho_scale=2.15
scene.camera=cam
def render(name,scale=2.15):
    cam.data.ortho_scale=scale
    scene.render.filepath=os.path.abspath(os.path.join(out,name))
    bpy.ops.render.render(write_still=True)

for n in range(16):
    clear()
    base=grass if n in (0,1,2,12,13,14) else grass2
    if n in (3,):base=sand
    if n in (7,8,15):base=stone
    if n==10:base=water
    if n in (4,5,6,9,11):base=soil
    ground(base)
    details(n,grass2 if n%2 else sand)
    if n in (7,15):details(n+9,snow)
    if n==11:
        cube("stone bridge",(0,0,.08),(.95,.34,.13),stone)
    render("terrain_%02d.png"%n)
for n in range(5):
    clear()
    if n==0:obelisk()
    if n==1:tree(True)
    if n==2:tree(False)
    if n==3:boulder()
    if n==4:house()
    render("prop_%02d.png"%n,2.7)
print("BLENDER_WORLD_PIPELINE_OK",out)
