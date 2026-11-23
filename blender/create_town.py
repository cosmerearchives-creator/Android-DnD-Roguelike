"""Generate the six Wildlands town facades using the Free Roam projection."""
import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector

out = Path(sys.argv[sys.argv.index("--") + 1])
out.mkdir(parents=True, exist_ok=True)

def material(name, color):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*color, 1)
    return m

plaster = material("Plaster", (.68,.54,.36))
timber = material("Dark oak", (.24,.123,.064))
stone = material("Granite", (.43,.43,.39))
amber = material("Amber window", (.98,.63,.18))
iron = material("Iron", (.20,.22,.21))
roof = [material("Roof "+str(i), color) for i,color in enumerate([
    (.20,.28,.34),(.25,.24,.21),(.48,.21,.13),(.28,.35,.22),(.30,.31,.42),(.44,.24,.16)])]

def cube(label, xyz, dimensions, mat):
    bpy.ops.mesh.primitive_cube_add(size=1, location=xyz)
    obj = bpy.context.object
    obj.name = label
    obj.dimensions = dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(mat)
    bevel = obj.modifiers.new("Soft edges", "BEVEL")
    bevel.width = .045
    bevel.segments = 2
    obj.modifiers.new("Normals", "WEIGHTED_NORMAL")
    return obj

def roof_panel(label, x, y, z, dims, tilt, mat):
    obj = cube(label, (x,y,z), dims,mat)
    obj.rotation_euler.x = tilt

def house(width,depth,variant):
    wall=1.7 if variant in (3,5) else 2.3
    cube("Foundation",(0,0,.17),(width,depth,.34),stone)
    cube("Main facade",(0,0,.29+wall/2),(width-.18,depth-.22,wall),plaster)
    for x in (-width*.46,width*.46):
        for yy in (-depth*.46,depth*.46):
            cube("Corner timber",(x,yy,.3+wall/2),(.17,.17,wall+.08),timber)
    for height in (.46,1.54,.24+wall):
        cube("Lintel",(0,-depth*.48,height),(width,.12,.12),timber)
        cube("Side timber",(width*.48,0,height),(.12,depth,.12),timber)
    cube("Front door",(0,-depth*.463,.88),(.74,.15,1.15),timber)
    cube("Door step",(0,-depth*.495,.12),(1,.36,.24),stone)
    for x in (-width*.28,width*.28):
        cube("Window frame",(x,-depth*.475,1.41),(.68,.13,.60),timber)
        cube("Window light",(x,-depth*.48-.08,1.41),(.51,.09,.43),amber)
        cube("Window bar",(x,-depth*.48-.135,1.41),(.06,.04,.47),timber)
    for y in (-depth*.27,depth*.27):
        cube("Side window",(width*.48,y,1.41),(.11,.49,.46),amber)
    rise=depth*.23
    peak=wall+.33+rise
    for side in (-1,1):
        roof_panel("Roof slope",0,side*depth*.25,peak-rise*.5,
            (width+.37,math.hypot(depth*.55,rise),.16),-side*math.atan2(rise,depth*.5),roof[variant])
    cube("Ridge trim",(0,0,peak+.07),(width+.42,.16,.13),timber)
    cube("Chimney",(width*.25,depth*.17,peak-.12),(.44,.49,1.37),stone)
    cube("Chimney cap",(width*.25,depth*.17,peak+.56),(.57,.60,.15),stone)
    if variant in (0,1,2):
        awning=roof[variant] if variant!=2 else material("Shop awning",(.70,.38,.12))
        roof_panel("Porch awning",0,-depth*.49,1.92,(width*.73,.95,.12),.19,awning)
        for x in (-width*.33,width*.33):
            cube("Porch support",(x,-depth*.495,.95),(.12,.12,1.8),timber)
    sign=material("Shop sign",[(.64,.35,.12),(.35,.34,.30),(.71,.47,.18),(.39,.55,.24),(.22,.36,.57),(.51,.31,.20)][variant])
    cube("Signpost",(width*.22,-depth*.49,1.80),(.55,.26,.085),timber)
    cube("Painted shop sign",(width*.22,-depth*.49-.13,1.65),(.40,.10,.27),sign)

scene=bpy.context.scene
bpy.ops.object.camera_add(location=(8,-8,7))
camera=bpy.context.object
scene.camera=camera
rotation=Vector((-1,1,-math.tan(math.asin(.51196))*math.sqrt(2))).to_track_quat("-Z","Y")
camera.rotation_euler=rotation.to_euler()
camera.data.type="ORTHO"
scene.render.engine="BLENDER_WORKBENCH"
scene.display.render_aa="16"
scene.display.shading.color_type="MATERIAL"
scene.display.shading.light="STUDIO"
scene.display.shading.show_cavity=True
scene.display.shading.cavity_type="WORLD"
scene.render.resolution_x=384
scene.render.resolution_y=384
scene.render.resolution_percentage=100
scene.render.film_transparent=True
scene.render.image_settings.file_format="PNG"
scene.render.image_settings.color_mode="RGBA"

for i,(w,d) in enumerate([(6,5),(5,4),(4,4),(4,3),(4,4),(4,3)]):
    for obj in list(scene.objects):
        if obj.type=="MESH": bpy.data.objects.remove(obj,do_unlink=True)
    house(w,d,i)
    # All files share a center ground pivot at (192, 264) and 36 pixels/tile.
    camera.data.ortho_scale=384/36
    camera.location=rotation @ Vector((0,2,20))
    scene.render.filepath=str((out/f"building_{i}.png").resolve())
    bpy.ops.render.render(write_still=True)
