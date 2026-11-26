import bpy, pathlib
root = pathlib.Path(__file__).resolve().parent
for variant in ("classic_study","petite_pale","classic_male","petite_male","dagger_goblin"):
    import sys
    sys.argv = ["blender","--","--variant",variant,"--output",str(root/"assets")]
    source = (root.parent/"blender/create_character.py").read_text()
    exec(compile(source.split("# Row zero faces")[0],"create_character.py","exec"),{"__name__":"__main__"})
    bpy.ops.object.select_all(action="DESELECT")
    for obj in bpy.context.scene.objects:
        if obj.type in {"MESH","EMPTY"}: obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(root/"assets"/(variant+".glb")),export_format="GLB",use_selection=True)

# Reuse the original world geometry and palette, without baking sprites.
source = (root.parent/"blender/create_world.py").read_text()
sys.argv = ["blender", "--", "--output", str(root/"assets")]
ns = {"__name__": "__main__"}
exec(compile(source.split("scene=bpy.context.scene")[0], "create_world.py", "exec"), ns)
for name, make in [("pine", lambda: ns["tree"](True)), ("broadleaf", lambda: ns["tree"](False)), ("boulder", ns["boulder"])]:
    ns["clear"]()
    make()
    bpy.ops.object.select_all(action="DESELECT")
    for obj in bpy.context.scene.objects:
        if obj.type == "MESH": obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(root/"assets"/(name+".glb")), export_format="GLB", use_selection=True)
