"""Export environment geometry only.

All character models are separately imported GLBs. Do not regenerate legacy
pose-per-frame characters; all humanoids use the shared skeleton animation library.
"""
import bpy
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parent
source = (root.parent / "blender/create_world.py").read_text()
sys.argv = ["blender", "--", "--output", str(root / "assets")]
ns = {"__name__": "__main__"}
exec(compile(source.split("scene=bpy.context.scene")[0], "create_world.py", "exec"), ns)
for name, make in [
    ("pine", lambda: ns["tree"](True)),
    ("broadleaf", lambda: ns["tree"](False)),
    ("boulder", ns["boulder"]),
]:
    ns["clear"]()
    make()
    bpy.ops.object.select_all(action="DESELECT")
    for obj in bpy.context.scene.objects:
        if obj.type == "MESH":
            obj.select_set(True)
    bpy.ops.export_scene.gltf(
        filepath=str(root / "assets" / (name + ".glb")),
        export_format="GLB",
        use_selection=True,
    )
