"""Experimental Blender inventory art, one transparent icon per real Equipment catalog ID."""
import bpy,sys,json
from pathlib import Path
from mathutils import Matrix, Vector
sys.path.insert(0,str(Path(__file__).resolve().parent))
from weapon_art import render_weapon
args=sys.argv[sys.argv.index("--")+1:]
def opt(k,d):return args[args.index(k)+1] if k in args else d
group=opt("--group","weapons")
catalog=json.loads(Path(opt("--catalog","build/inventory-factory/catalog.json")).read_text())
out=Path(opt("--output","build/inventory-factory/icons"));out.mkdir(parents=True,exist_ok=True)
def mat(name,rgb):
 m=bpy.data.materials.new(name);m.diffuse_color=(*rgb,1);m.use_nodes=True
 m.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value=(*rgb,1);return m
iron=mat("Iron",(.43,.48,.49));wood=mat("Wood",(.28,.14,.075));gold=mat("Brass",(.67,.48,.16))
leather=mat("Leather",(.32,.19,.11));cloth=mat("Cloth",(.18,.26,.34));glass=mat("Glass",(.16,.42,.47))
def cylinder(name,loc,r,depth,m,vertices=12):
 bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=r,depth=depth,location=loc)
 o=bpy.context.object;o.name=name;o.data.materials.append(m);return o
def sphere(name,loc,scale,m):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=12,ring_count=8,location=loc)
 o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(m);return o
def cube(name,loc,scale,m):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc)
 o=bpy.context.object;o.name=name;o.dimensions=scale;o.data.materials.append(m)
 bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);return o
bpy.ops.object.light_add(type="AREA",location=(3,-4,6));bpy.context.object.data.energy=650;bpy.context.object.data.size=4
bpy.ops.object.camera_add(location=(3,-5,4));camera=bpy.context.object
camera.rotation_euler=(Vector((0,0,.8))-camera.location).to_track_quat("-Z","Y").to_euler()
camera.data.type="ORTHO";camera.data.ortho_scale=3.6
scene=bpy.context.scene;scene.camera=camera
scene.render.resolution_x=256;scene.render.resolution_y=256;scene.render.resolution_percentage=100
scene.render.film_transparent=True;scene.render.image_settings.file_format="PNG"
if group=="weapons":
 scene.render.engine="BLENDER_WORKBENCH"
 scene.display.render_aa="16"
 scene.display.shading.light="STUDIO"
 scene.display.shading.color_type="MATERIAL"
 scene.display.shading.show_cavity=True
 scene.display.shading.cavity_type="WORLD"
 camera.location=(1.16,-6,2.16)
 camera.rotation_euler=(Vector((0,0,1.08))-camera.location).to_track_quat("-Z","Y").to_euler()
 camera.data.ortho_scale=2.43
else:
 scene.render.engine="BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in scene.render.bl_rna.properties["engine"].enum_items.keys() else "BLENDER_EEVEE"
for entry in catalog:
 if (entry["category"]=="weapon") != (group=="weapons"):continue
 for obj in list(bpy.data.objects):
  if obj.type in {"MESH","CURVE"}:bpy.data.objects.remove(obj,do_unlink=True)
 name=entry["name"].lower()
 if group=="weapons":
  render_weapon(entry["id"],iron,wood,gold,leather,cloth,cylinder,sphere)
  # Angle the icon in the image plane so points and handles have room.
  angle=.47
  rotation=Matrix.Rotation(angle,4,"Y")
  pivot=Vector((0,0,1.13))
  bpy.context.view_layer.update()
  for obj in list(bpy.data.objects):
   if obj.type in {"MESH","CURVE"}:
    matrix=obj.matrix_world.copy()
    matrix.translation-=pivot
    result=rotation @ matrix
    result.translation+=pivot
    obj.matrix_world=result
 elif entry["category"]=="armor":
  cube("torso",(0,0,.85),(.9,.5,1.15),iron if any(k in name for k in ("mail","plate","scale")) else leather)
  cube("collar",(0,0,1.5),(.56,.6,.16),gold)
 else:
  if any(k in name for k in ("book","map","documents","sketch","cards")):
   cube("paper",(0,0,.6),(.95,.22,1.25),cloth)
   cube("binding",(-.42,-.13,.6),(.13,.08,1.25),leather)
  elif any(k in name for k in ("bottle","oil","waterskin","flask")):
   sphere("container",(0,0,.65),(.43,.35,.52),glass)
   cylinder("neck",(0,0,1.23),.13,.3,leather)
  elif any(k in name for k in ("rope","thread","silk")):
   cylinder("coil",(0,0,.7),.48,.42,leather)
   cylinder("knot",(0,0,1),.15,.25,wood)
  elif any(k in name for k in ("bag","pouch","pack","chest","kit","supplies","tools","set")):
   cube("case",(0,0,.6),(1,.7,.85),leather)
   cube("latch",(0,-.36,.7),(.17,.06,.16),gold)
  else:
   cylinder("handle",(0,0,.65),.12,1.1,wood)
   sphere("head",(0,0,1.2),(.3,.25,.28),iron)
 scene.render.filepath=str((out/(entry["id"]+".png")).resolve())
 bpy.ops.render.render(write_still=True)
print("INVENTORY_ART_RENDERED",group)
