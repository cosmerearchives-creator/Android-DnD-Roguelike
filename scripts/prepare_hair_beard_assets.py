"""Make lightweight, centered modular hair/beard GLBs from supplied assets.
Run from Blender on the local development machine; no source GLBs overwritten.
"""
import bpy, json
from mathutils import Vector
from pathlib import Path
ROOT=Path(r"G:\ChatGPT Projects\Forests of Hell")
IN=ROOT/"Beastiary Imports"
OUT=ROOT/"ForestsOfHellSource"/"godot"/"assets"/"appearance"
OUT.mkdir(parents=True,exist_ok=True)
SOURCE={
"hair_wavy":"hair wavy.glb",
"hair_short":"hair short 1.glb",
"hair_braid":"hair braid.glb",
"beard_full":"beard 2.glb",
}
report={}
for key,name in SOURCE.items():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(IN/name))
    meshes=[o for o in bpy.context.scene.objects if o.type=="MESH"]
    primary=max(meshes,key=lambda o:len(o.data.vertices))
    for obj in list(bpy.context.scene.objects):
        if obj!=primary:bpy.data.objects.remove(obj,do_unlink=True)
    bpy.ops.object.select_all(action='DESELECT')
    primary.select_set(True)
    bpy.context.view_layer.objects.active=primary
    # Retain appearance and UVs; preserve the origin at geometry center for positioning.
    bpy.ops.object.transform_apply(location=False,rotation=True,scale=True)
    bounds=[primary.matrix_world@Vector(c) for c in primary.bound_box]
    lo=Vector(tuple(min(x[i] for x in bounds) for i in range(3)))
    hi=Vector(tuple(max(x[i] for x in bounds) for i in range(3)))
    center=(lo+hi)/2
    for vertex in primary.data.vertices:vertex.co-=center
    # Avoid top-heavy character creators: reduce triangle count but keep UVs/materials.
    before=len(primary.data.polygons)
    dec=primary.modifiers.new("Preview/mobile LOD","DECIMATE")
    dec.ratio=min(1.0,16000/max(1,before))
    bpy.ops.object.modifier_apply(modifier=dec.name)
    after=len(primary.data.polygons)
    dimensions=hi-lo
    primary.name=key
    bpy.ops.export_scene.gltf(filepath=str(OUT/(key+".glb")),export_format='GLB',use_selection=True,export_animations=False)
    report[key]={"source":name,"source_triangles":before,"triangles":after,"dimensions":list(dimensions),"source_center":list(center)}
    print("APPEARANCE_ASSET",key,report[key],flush=True)
(OUT/"dimensions.json").write_text(json.dumps(report,indent=2),encoding="utf8")
