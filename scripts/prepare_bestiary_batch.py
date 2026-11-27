"""Prepare the four new Forests of Hell bestiary candidates.
Preserves source files. Optimizes only game-ready derivatives for Android.
Blender 5.3: blender -b -t 2 --python scripts/prepare_bestiary_batch.py
"""
import bpy, os, pathlib, json, time, sys
from mathutils import Vector
ROOT=pathlib.Path(__file__).resolve().parent.parent
SOURCE=ROOT.parent / 'Beastiary Imports'
DEST=ROOT/'godot'/'assets'
CASES=[
 ('Goblin Alt 2.glb','goblin_alt_2.glb',26000,1024,False),
 ('Goblin Alt 3.glb','goblin_alt_3.glb',33000,1024,False),
 ('Goblin Alt 4.glb','goblin_alt_4.glb',40000,1024,False),
 ('Meshy_AI_Cybersecurity_Beetle_icon_as_a_geometric_20261127_animated_Walking.glb','fire_beetle.glb',35000,1024,True),
]
def run(name,output,target,texture_size,animated):
 start=time.time()
 bpy.ops.wm.read_factory_settings(use_empty=True)
 bpy.ops.import_scene.gltf(filepath=str(SOURCE/name))
 meshes=[ob for ob in bpy.data.objects if ob.type=='MESH']
 assert meshes,name
 before=sum(len(ob.data.polygons) for ob in meshes)
 print('CANDIDATE',name,'original triangles',before,flush=True)
 # Goblin source exports have their long dimension along Z, already roughly 2 units.
 # Rotate the entire imported scene for Godot Y-up instead of modifying the mesh.
 # Blender's glTF importer/exporter converts between Z-up and glTF Y-up.
 # Imported goblins are Z-up in Blender; standard glTF exporter handles transform.
 # The beetle's original skeletal data and animation must remain intact.
 if not animated:
  for ob in meshes:
   # Decimate only derivatives; preserve every embedded source file.
   if len(ob.data.polygons)>target:
    modifier=ob.modifiers.new("Android_Render_Budget","DECIMATE")
    modifier.ratio=max(.005, min(1.0,target/len(ob.data.polygons)))
    bpy.context.view_layer.objects.active=ob
    bpy.ops.object.select_all(action='DESELECT')
    ob.select_set(True)
    bpy.ops.object.modifier_apply(modifier=modifier.name)
    ob.select_set(False)
 else:
  for ob in meshes:
   if len(ob.data.polygons)>target:
    modifier=ob.modifiers.new("Android_Render_Budget","DECIMATE")
    modifier.ratio=max(.005, min(1.0,target/len(ob.data.polygons)))
    bpy.context.view_layer.objects.active=ob
    bpy.ops.object.select_all(action='DESELECT')
    ob.select_set(True)
    bpy.ops.object.modifier_apply(modifier=modifier.name)
    ob.select_set(False)
 for im in bpy.data.images:
  if not im.has_data: continue
  w,h=im.size
  if max(w,h)>texture_size:
   scale=texture_size/max(w,h)
   im.scale(max(1,round(w*scale)),max(1,round(h*scale)))
 # Goblin meshes are unrigged, don't fabricate animation data.
 # Preserve the beetle's armature, skin weights, and Blender action.
 for act in bpy.data.actions:
  if animated and act.frame_range[1]>act.frame_range[0]:
   act.name='Walk'
 outfile=DEST/output
 bpy.ops.export_scene.gltf(filepath=str(outfile),export_format='GLB',export_animations=animated,export_skins=animated,export_image_format='AUTO')
 after=sum(len(ob.data.polygons) for ob in meshes)
 print('CANDIDATE_OK',json.dumps({'name':name,'output':output,'triangles_before':before,'triangles_after':after,'size_bytes':outfile.stat().st_size,'images':[(im.name,list(im.size)) for im in bpy.data.images],'actions':[a.name for a in bpy.data.actions],'duration_s':round(time.time()-start,1)}),flush=True)
for args in CASES:
 try: run(*args)
 except Exception as e:
  print('CANDIDATE_FAILED',args[0],repr(e),flush=True)
  raise
