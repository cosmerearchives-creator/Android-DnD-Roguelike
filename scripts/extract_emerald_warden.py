"""Extract the left Warden from the supplied three-figure Meshy GLB."""
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector

source, output = map(Path, sys.argv[sys.argv.index("--") + 1:])
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(source))
mesh = next(obj for obj in bpy.context.scene.objects if obj.type == "MESH")
bpy.context.view_layer.objects.active = mesh
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
cut = -.30
assert all(
    max(mesh.data.vertices[i].co.x for i in face.vertices) <= cut
    or min(mesh.data.vertices[i].co.x for i in face.vertices) > cut
    for face in mesh.data.polygons
), "The extraction plane intersects a face"
bm = bmesh.new()
bm.from_mesh(mesh.data)
bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.x > cut], context="VERTS")
bm.to_mesh(mesh.data)
bm.free()
assert len(mesh.data.polygons) > 0
floor = min(v.co.z for v in mesh.data.vertices)
top = max(v.co.z for v in mesh.data.vertices)
# Center from the boots, keeping the weapon's asymmetric reach.
boots = [v.co for v in mesh.data.vertices if v.co.z < floor + .08 * (top - floor)]
center = Vector([(min(p[i] for p in boots) + max(p[i] for p in boots)) / 2 for i in range(2)] + [floor])
for vertex in mesh.data.vertices:
    vertex.co = (vertex.co - center) / (top - floor)
mesh.data.update()
mesh.name = "Emerald Blade Warden"
bpy.ops.export_scene.gltf(filepath=str(output), export_format="GLB",
                         use_selection=True, export_animations=False)
print("WARDEN TRIANGLES", len(mesh.data.polygons))

scene = bpy.context.scene
scene.render.engine = "BLENDER_WORKBENCH"
scene.display.shading.color_type = "TEXTURE"
scene.display.shading.light = "STUDIO"
scene.display.shading.background_type = "WORLD"
scene.world = bpy.data.worlds.new("Review World")
scene.world.color = (.065, .065, .065)
scene.render.resolution_x = 450
scene.render.resolution_y = 570
scene.render.resolution_percentage = 100
camera = bpy.data.objects.new("Camera", bpy.data.cameras.new("Camera"))
scene.collection.objects.link(camera)
scene.camera = camera
camera.data.type = "ORTHO"
camera.data.ortho_scale = 1.24
target = Vector((-.05, 0, .50))
for label, direction in (("Front", -1), ("Back", 1)):
    camera.location = target + Vector((0, direction * 3, 0))
    camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()
    scene.render.filepath = str(output.with_name("Warden " + label + ".png"))
    bpy.ops.render.render(write_still=True)
