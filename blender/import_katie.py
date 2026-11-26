"""Import and optimize the user-supplied Katie Blender scene for Forests of Hell.

Run with Blender 5.x:
 blender --background --python blender/import_katie.py -- "C:/path/Katie - Version 2.01.zip"
Optional: --inspect to inventory objects without exporting, --include Name1,Name2
"""
import argparse
import pathlib
import sys
import zipfile
import tempfile
import json

import bpy

def parse_args():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=pathlib.Path)
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1] / "godot" / "assets" / "katie.glb")
    parser.add_argument("--include", help="Comma-separated object names; include linked rig and children automatically")
    parser.add_argument("--inspect", action="store_true")
    parser.add_argument("--max-texture", type=int, default=1024)
    parser.add_argument("--max-triangles", type=int, default=65000)
    return parser.parse_args(args)

def open_source(path):
    if path.suffix.lower() == ".zip":
        with zipfile.ZipFile(path) as archive:
            matches = [v for v in archive.infolist() if v.filename.lower().endswith(".blend") and not v.is_dir()]
            if not matches:
                raise RuntimeError("The ZIP has no .blend model")
            matches.sort(key=lambda v: v.file_size, reverse=True)
            with tempfile.TemporaryDirectory() as temp:
                name = matches[0]
                out = pathlib.Path(temp) / pathlib.Path(name.filename).name
                with archive.open(name) as source, out.open("wb") as target:
                    import shutil
                    shutil.copyfileobj(source, target)
                bpy.ops.wm.open_mainfile(filepath=str(out), load_ui=False)
    elif path.suffix.lower() == ".blend":
        bpy.ops.wm.open_mainfile(filepath=str(path), load_ui=False)
    else:
        raise RuntimeError("Expected .zip or .blend")

def tri_count(obj):
    if obj.type != "MESH":
        return 0
    return sum(max(0, len(poly.vertices) - 2) for poly in obj.data.polygons)

def main():
    opts = parse_args()
    if not opts.source.is_file():
        raise FileNotFoundError(str(opts.source))
    open_source(opts.source)
    objects = list(bpy.context.scene.objects)
    inventory = [{"name": o.name, "type": o.type, "triangles": tri_count(o),
                  "parent": o.parent.name if o.parent else None,
                  "visible": not o.hide_get()} for o in objects]
    report = {"source": opts.source.name, "objects": inventory,
              "actions": [a.name for a in bpy.data.actions],
              "images": [{"name": i.name, "size": list(i.size)} for i in bpy.data.images]}
    print("KATIE_INSPECTION=" + json.dumps(report))
    if opts.inspect:
        return
    # Start with visible meshes. Use --include to restrict scene contents if source contains props.
    if opts.include:
        names = {name.strip() for name in opts.include.split(",") if name.strip()}
        chosen = [o for o in objects if o.name in names]
        if len(chosen) != len(names):
            raise ValueError("Unmatched --include names: " + str(names - {o.name for o in chosen}))
        added = set(chosen)
        for o in list(chosen):
            p = o.parent
            while p:
                added.add(p)
                p = p.parent
        chosen = list(added)
    else:
        chosen = [o for o in objects if o.type in {"MESH", "ARMATURE"} and not o.hide_get()]
        meshes = [o for o in chosen if o.type == "MESH"]
        if not meshes:
            raise RuntimeError("No visible character mesh; inspect the file first")
        # Include rigs needed by skinned objects.
        for m in meshes:
            rig = m.find_armature()
            if rig and rig not in chosen:
                chosen.append(rig)
    if not any(o.type == "MESH" for o in chosen):
        raise RuntimeError("Selection contains no mesh")
    bpy.ops.object.select_all(action="DESELECT")
    for o in chosen:
        o.hide_set(False)
        o.select_set(True)
    bpy.context.view_layer.objects.active = next(o for o in chosen if o.type == "MESH")
    before = sum(tri_count(o) for o in chosen)
    for o in chosen:
        if o.type != "MESH":
            continue
        count = tri_count(o)
        if count <= opts.max_triangles:
            continue
        # Keep original armature modifiers and vertex groups.
        mod = o.modifiers.new(name="Mobile triangle reduction", type="DECIMATE")
        mod.ratio = max(0.01, opts.max_triangles / count)
        bpy.context.view_layer.objects.active = o
        try:
            bpy.ops.object.modifier_apply(modifier=mod.name)
        except Exception as exc:
            print("WARNING: Could not decimate", o.name, str(exc))
            o.modifiers.remove(mod)
    # Resize large in-memory textures only. Missing external textures are reported.
    for img in bpy.data.images:
        if img.source not in {"FILE", "GENERATED"}:
            continue
        if not img.has_data:
            try:
                img.reload()
            except Exception:
                print("WARNING: Missing image", img.name, img.filepath)
                continue
        w, h = img.size
        if max(w, h) > opts.max_texture and min(w, h) > 0:
            factor = opts.max_texture / max(w, h)
            img.scale(max(1, round(w * factor)), max(1, round(h * factor)))
    out = opts.output.resolve()
    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(out), export_format="GLB", use_selection=True,
        export_animations=True, export_skins=True, export_morph=True,
        export_materials="EXPORT", export_image_format="AUTO",
    )
    print("KATIE_EXPORT=" + json.dumps({"path": str(out),
        "bytes": out.stat().st_size, "triangles_before": before,
        "triangles_after": sum(tri_count(o) for o in chosen)}))

if __name__ == "__main__":
    main()
