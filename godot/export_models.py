import bpy, pathlib
root = pathlib.Path(__file__).resolve().parent
for variant in ("classic_study","dagger_goblin"):
    import sys
    sys.argv = ["blender","--","--variant",variant,"--output",str(root/"assets")]
    source = (root.parent/"blender/create_character.py").read_text()
    exec(compile(source.split("# Row zero faces")[0],"create_character.py","exec"),{"__name__":"__main__"})
    bpy.ops.object.select_all(action="DESELECT")
    for obj in bpy.context.scene.objects:
        if obj.type in {"MESH","EMPTY"}: obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(root/"assets"/(variant+".glb")),export_format="GLB",use_selection=True)
