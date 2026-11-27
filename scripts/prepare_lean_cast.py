"""Build compact rigged character GLBs for Forests of Hell.

Run through Blender:
  blender -b -t 2 --python-exit-code 1 --python scripts/prepare_lean_cast.py -- \
    --human "original_female.glb" --goblin "original_goblin.glb" --output godot/assets

Original source files are only read; the source archive is not modified.
"""
import argparse
import math
import pathlib
import sys
import bpy


def load_and_write(path, destination, *, tris_target, max_texture, animations):
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes, bpy.data.armatures, bpy.data.images, bpy.data.materials, bpy.data.actions):
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)
    bpy.ops.import_scene.gltf(filepath=str(path))
    meshes = [o for o in bpy.context.scene.objects if o.type == "MESH"]
    skeletons = [o for o in bpy.context.scene.objects if o.type == "ARMATURE"]
    if len(skeletons) != 1 or not meshes:
        raise RuntimeError(f"Expected one rig and at least one mesh, saw {len(skeletons)} armatures and {len(meshes)} meshes")
    original_triangles = sum(len(o.data.loop_triangles) for o in meshes)
    print("SOURCE",path,"meshes",len(meshes),"rig_bones",len(skeletons[0].data.bones),"triangles",original_triangles,flush=True)
    if original_triangles > tris_target:
        ratio = tris_target / original_triangles
        for mesh in meshes:
            if mesh.data.shape_keys:
                print("SKIP decimation: shape keys",mesh.name,flush=True)
                continue
            if len(mesh.data.polygons) < 300:
                continue
            modifier = mesh.modifiers.new("MobileTriangleBudget", "DECIMATE")
            modifier.ratio = ratio
            bpy.context.view_layer.objects.active = mesh
            for other in bpy.context.selected_objects:
                other.select_set(False)
            mesh.select_set(True)
            # Put decimation before armature deformation, keeping the skin modifier intact.
            arm = next((m for m in mesh.modifiers if m.type == "ARMATURE"), None)
            if arm:
                while list(mesh.modifiers).index(modifier) > list(mesh.modifiers).index(arm):
                    bpy.ops.object.modifier_move_up(modifier=modifier.name)
            bpy.ops.object.modifier_apply(modifier=modifier.name)
            mesh.select_set(False)
    # Restrict the mobile texture footprint while preserving original PBR assignments.
    for image in list(bpy.data.images):
        if image.source != "FILE" or not image.has_data:
            continue
        w, h = image.size[:]
        if not w or not h:
            continue
        factor = min(1.0, max_texture / float(max(w, h)))
        if factor < 1:
            image.scale(max(1, round(w*factor)), max(1, round(h*factor)))
            print("TEXTURE",image.name,f"{w}x{h}",f"{image.size[0]}x{image.size[1]}",flush=True)
    for o in bpy.context.selected_objects:
        o.select_set(False)
    for o in bpy.context.scene.objects:
        if o.type in {"MESH", "ARMATURE", "EMPTY"}:
            o.select_set(True)
    bpy.context.view_layer.objects.active = skeletons[0]
    destination.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(destination), export_format="GLB", use_selection=True,
        export_animations=animations, export_skins=True,
        export_normals=True, export_texcoords=True,
        export_materials="EXPORT", export_image_format="AUTO",
        export_apply=False,
    )
    print("OUTPUT",destination,"bytes",destination.stat().st_size,
          "triangles",sum(len(o.data.loop_triangles) for o in meshes),flush=True)


def main():
    argv = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--human",required=True,type=pathlib.Path)
    parser.add_argument("--goblin",required=True,type=pathlib.Path)
    parser.add_argument("--output",required=True,type=pathlib.Path)
    args = parser.parse_args(argv)
    load_and_write(args.human,args.output/"human_adventurer.glb",
                   tris_target=85000,max_texture=2048,animations=False)
    load_and_write(args.goblin,args.output/"goblin_warrior.glb",
                   tris_target=30000,max_texture=2048,animations=True)


if __name__ == "__main__":
    main()
