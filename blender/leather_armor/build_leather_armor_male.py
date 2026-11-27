"""Rebuild the original leather armor design fitted to the existing male rig (rig unchanged)."""
from pathlib import Path
import math
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree
from mathutils.kdtree import KDTree

source, output = map(Path, sys.argv[sys.argv.index("--") + 1:])
output.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(source))
body = next(obj for obj in bpy.context.scene.objects if obj.type == "MESH")
rig = next(obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE")
# The imported male comes with a Running action active. Clear its pose for fitting
# but preserve the original action datablock for later deformation previews.
source_running = bpy.data.actions.get("Running")
if source_running:
    source_running.use_fake_user = True
rig.animation_data_clear()
for pose_bone in rig.pose.bones:
    pose_bone.location = (0, 0, 0)
    pose_bone.rotation_euler = (0, 0, 0)
    pose_bone.rotation_quaternion = (1, 0, 0, 0)
    pose_bone.scale = (1, 1, 1)
bpy.context.scene.frame_set(0)
bpy.context.view_layer.update()
verts = [body.matrix_world @ v.co for v in body.data.vertices]
surface = BVHTree.FromPolygons(verts, [tuple(p.vertices) for p in body.data.polygons], all_triangles=True)
nearest = KDTree(len(verts))
for index, point in enumerate(verts):
    nearest.insert(point, index)
nearest.balance()

def material(name, rgba, roughness, metallic=0.0):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = rgba
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = rgba
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = roughness
    return mat

leather = material("Dark leather", (.048, .025, .015, 1), .57)
panels = material("Raised leather panels", (.086, .047, .026, 1), .48)
trim = material("Edge binding", (.15, .081, .044, 1), .55)
metal = material("Aged brass", (.29, .165, .071, 1), .36, .72)
materials = (leather, panels, trim, metal)
pieces = []

def fitted(center, radial, search, padding):
    start = center + radial * search
    point, _, _, _ = surface.ray_cast(start, -radial, search + .015)
    if point is None:
        point, _, _, _ = surface.find_nearest(start)
    return point + radial * padding

def horizontal(center, angle, radius, padding):
    radial = Vector((math.sin(angle), -math.cos(angle), 0))
    return fitted(Vector(center), radial, radius, padding)

def sheet(name, rows, closed, thickness=.003, palette=(0,)):
    width = len(rows[0])
    edge_count = width if closed else width - 1
    points = [point for row in rows for point in row]
    faces = [(r*width+i, r*width+(i+1)%width, (r+1)*width+(i+1)%width, (r+1)*width+i)
             for r in range(len(rows)-1) for i in range(edge_count)]
    data = bpy.data.meshes.new(name)
    data.from_pydata(points, [], faces)
    data.update()
    for mat in materials:
        data.materials.append(mat)
    for face in data.polygons:
        face.material_index = palette[face.index // edge_count % len(palette)]
        face.use_smooth = True
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    bpy.context.view_layer.objects.active = obj
    solid = obj.modifiers.new("Leather thickness", "SOLIDIFY")
    solid.thickness = thickness
    solid.offset = 0
    solid.material_offset_rim = 2
    bpy.ops.object.modifier_apply(modifier=solid.name)
    bm = bmesh.new()
    bm.from_mesh(data)
    bmesh.ops.recalc_face_normals(bm, faces=list(bm.faces))
    bm.to_mesh(data)
    bm.free()
    pieces.append(obj)
    return obj

angles = [math.tau*i/64 for i in range(64)]
def vest_edge(angle, upper):
    front = max(0, math.cos(angle))
    if upper:
        cup = abs(math.sin(2.0 * angle)) * front
        return 1.324 + .019*cup - .010*front
    return 1.012 - .019*front**6

def vest_row(fraction, padding=.013):
    return [horizontal((0, .017, (1-fraction)*vest_edge(a, False)+fraction*vest_edge(a, True)),
                       a, .217, padding) for a in angles]

sheet("Laced leather cuirass", [vest_row(i/20) for i in range(21)], True,
      palette=(0,))
for tag, lower, upper in (("Neckline binding", .965, 1.0), ("Waist binding", 0.0, .043)):
    sheet(tag, [vest_row(lower, .017), vest_row(upper, .017)], True, .002, (2,))

# A front and rear leg of each strap meets over the shoulder.
for side, sign in (("Left", 1), ("Right", -1)):
    rows = []
    for i in range(17):
        a = math.pi * i / 16
        x = sign * (.135 + .041*math.sin(a))
        radial = Vector((0, -math.cos(a), math.sin(a)))
        rows.append([fitted(Vector((x+dx, .005, 1.300)), radial, .207, .014)
                     for dx in (-.016, 0, .016)])
    sheet(side+" shoulder strap", rows, False, .004, (1,))

    def arm_row(t, padding):
        prefix = "mixamorig:" + side + "ForeArm"
        start = rig.matrix_world @ rig.data.bones[prefix].head_local
        end = rig.matrix_world @ rig.data.bones["mixamorig:" + side + "Hand"].head_local
        direction = (end-start).normalized()
        a_axis = direction.cross(Vector((0, 1, 0))).normalized()
        b_axis = direction.cross(a_axis).normalized()
        center = start.lerp(end, t)
        section = [p-center for p in verts if abs((p-center).dot(direction)) < .009
                   and (p-center).length_squared < .103**2 and sign*p.x > .246]
        assert section, prefix
        row = []
        for angle in angles:
            radial = math.cos(angle)*a_axis + math.sin(angle)*b_axis
            radius = max(.031, max(p.dot(radial) for p in section))
            row.append(center + radial*(radius+padding))
        return row


    sheet(side+" bracer", [arm_row(.08 + i*.80/14, .018) for i in range(15)], True,
          palette=(1,))
    for number, start in enumerate((.085, .77), 1):
        sheet(f"{side} bracer band {number}", [arm_row(start, .024), arm_row(start+.065, .024)],
              True, .004, (2,))

    def boot_row(z, padding):
        section = [p for p in verts if sign*p.x > .105 and abs(p.z-max(.018,z)) < .018]
        assert section, side
        limits = [(min(p[k] for p in section), max(p[k] for p in section)) for k in range(2)]
        cx, cy = [(lo+hi)/2 for lo,hi in limits]
        rx, ry = [(hi-lo)*.60+padding for lo,hi in limits]
        return [Vector((cx+rx*math.sin(a), cy-ry*math.cos(a), z)) for a in angles]
    boot_levels = [.006,.019,.038,.063,.092,.122,.155,.19,.23,.27,.31,.35,.39,.43,.475,.514]
    sheet(side+" tall leather boot", [boot_row(z, .011) for z in boot_levels], True,
          palette=(0,))
    for number, z in enumerate((.095,.464),1):
        sheet(f"{side} boot strap {number}", [boot_row(z, .017), boot_row(z+.018, .017)],
              True, .004, (2,))

    def shorts_row(z):
        # The male resting mesh includes arms near hip height. Ignore them to
        # avoid the original female shorts routine producing oversized rings.
        center_x = sign*(.102 if z > .86 else .120)
        section = [p for p in verts if .022 < sign*p.x < .217
                   and abs(p.y) < .133 and abs(p.z-z) < .012
                   and abs(p.x-center_x) < .116]
        assert len(section) > 6, (side,z,len(section))
        limits = [(min(p[k] for p in section), max(p[k] for p in section)) for k in range(2)]
        cx,cy = [(lo+hi)/2 for lo,hi in limits]
        rx = min(.113,max(.064,(limits[0][1]-limits[0][0])*.55+.009))
        ry = min(.124,max(.078,(limits[1][1]-limits[1][0])*.55+.009))
        return [Vector((cx+rx*math.sin(a),cy-ry*math.cos(a),z)) for a in angles]

    sheet(side+" leather shorts", [shorts_row(.777 + i*.251/12) for i in range(13)], True,
          .003, (0,))

    def tasset_row(t, extra):
        angles_side = [sign*(-.03 + 3.04*i/24) for i in range(25)]
        result = []
        for a in angles_side:
            lower = .795 - .035*abs(math.sin(a)) + .06*abs(math.cos(a))
            z = 1.012*(1-t) + lower*t
            center = (sign*.107, .027, z)
            result.append(horizontal(center, a, .165, .018+extra*t))
        return result
    for segment,(start,end) in enumerate(((0,.41),(.31,.74),(.63,1)),1):
        sheet(f"{side} layered hip panel {segment}",
              [tasset_row(start+(end-start)*i/8,.012*segment) for i in range(9)], False,
              .004, (1,0,0,0,0,0,0,2))

# A fitted central leather guard covers the gap between the independent leg pieces.
# This is armor geometry only, bound to the existing male skin.
for name, direction in (("Front", -1), ("Rear", 1)):
    levels=((1.025,.077,.129),(.980,.082,.132),(.925,.080,.136),
            (.870,.071,.132),(.818,.054,.118),(.782,.045,.108))
    rows=[]
    for z,width,depth in levels:
        row=[]
        for i in range(9):
            x=width*(i-4)/4 + .003
            y=direction*depth
            row.append(Vector((x,y,z)))
        rows.append(row)
    sheet(name+" leather hip guard", rows, False, .004, (1,))

def stud(name, position, radius, mat):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=radius, location=position)
    obj = bpy.context.object
    obj.name = name
    obj.data.materials.append(mat)
    pieces.append(obj)

for i in range(9):
    z = 1.031 + i*.028
    for sign in (-1,1):
        x = sign*(.014+.006*i/8)
        point = fitted(Vector((x,0,z)), Vector((0,-1,0)), .25, .022)
        stud("Lacing eyelet", point, .0055, metal)
    left = fitted(Vector((-.019,0,z)), Vector((0,-1,0)), .25, .024)
    right = fitted(Vector((.019,0,z+.011)), Vector((0,-1,0)), .25, .024)
    curve = [[point+Vector((0,0,dz)) for dz in (-.002,.002)] for point in (left,right)]
    sheet("Leather cross lace", curve, False, .002, (2,))
stud("Waist clasp", fitted(Vector((0,0,1.018)), Vector((0,-1,0)), .25, .024), .019, metal)

# Bind each attachment to the existing character skin by sampling nearby body weights.
valid = {bone.name for bone in rig.data.bones}
for obj in pieces:
    groups = {}
    matrix = obj.matrix_world.copy()
    for vertex in obj.data.vertices:
        samples = nearest.find_n(matrix @ vertex.co, 5)
        weights = {}
        for _, index, distance in samples:
            factor = 1 / max(distance, .002) ** 2
            for item in body.data.vertices[index].groups:
                name = body.vertex_groups[item.group].name
                if name in valid:
                    weights[name] = weights.get(name, 0.0) + item.weight*factor
        top = sorted(weights.items(), key=lambda item: -item[1])[:4]
        total = sum(weight for _, weight in top)
        assert total > 0, obj.name
        for name, weight in top:
            if name not in groups:
                groups[name] = obj.vertex_groups.new(name=name)
            groups[name].add([vertex.index], weight/total, "REPLACE")
    obj.parent = rig
    obj.matrix_world = matrix
    armature = obj.modifiers.new("Male Human existing skin", "ARMATURE")
    armature.object = rig

bpy.ops.object.select_all(action="DESELECT")
for obj in pieces:
    obj.select_set(True)
bpy.context.view_layer.objects.active = pieces[0]
bpy.context.preferences.filepaths.save_version = 0
bpy.ops.wm.save_as_mainfile(filepath=str(output/"Leather Armor - Male Human.blend"))
print("EDITABLE ARMOR PIECES", len(pieces))
# Keep the editable pieces in the blend and batch the game export by material.
bpy.ops.object.join()
armor = bpy.context.object
armor.name = "Leather Armor"
rig.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(output/"leather_armor_male.glb"), export_format="GLB",
                         use_selection=True, export_animations=False)
scene = bpy.context.scene
scene.world = bpy.data.worlds.new("Leather armor male review")
scene.world.color = (.06,.06,.06)
scene.render.engine = "BLENDER_WORKBENCH"
scene.display.shading.light = "STUDIO"
scene.display.shading.color_type = "MATERIAL"
scene.display.shading.background_type = "WORLD"
scene.display.shading.show_cavity = True
scene.render.resolution_x,scene.render.resolution_y = 520,690
scene.render.resolution_percentage = 100
camera=bpy.data.objects.new("Review Camera",bpy.data.cameras.new("Review Camera"))
scene.collection.objects.link(camera)
scene.camera=camera
camera.data.type="ORTHO"
camera.data.ortho_scale=2.07
target=Vector((0,0,.95))
for name,offset in (("Front",(.22,-3,.06)),("Rear",(.32,3,.1)),("Gameplay",(1,-1.5,2.6))):
    camera.location=target+Vector(offset)
    camera.rotation_euler=(target-camera.location).to_track_quat("-Z","Y").to_euler()
    scene.render.filepath=str(output/(name+".png"))
    bpy.ops.render.render(write_still=True)


# Use the original male's existing animation. We never edit bones, bone maps, hierarchy or animation.
if bpy.data.actions.get("Running"):
    action=bpy.data.actions["Running"]
    rig.animation_data_create()
    try:
        rig.animation_data.action=action
        print("EXISTING RUNNING ACTION", tuple(action.frame_range))
    except Exception as exc:
        print("RUNNING ACTION ASSIGN ERROR",repr(exc))
    for label, phase in (("Running early", .20),("Running late", .65)):
        first,last=action.frame_range
        scene.frame_set(round(first+phase*(last-first)))
        scene.render.filepath=str(output/(label+".png"))
        bpy.ops.render.render(write_still=True)
print("MALE FIT READY",str(output))
