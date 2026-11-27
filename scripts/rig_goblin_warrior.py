"""Goblin Warrior source-mesh rigging for Forests of Hell.
Run in Blender 5.x: blender -b --python scripts/rig_goblin_warrior.py
Source geometry/material are preserved; inferred region weights need visual review.
"""
import bpy
import math
import pathlib
import sys
from mathutils import Vector

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT.parent / "Beastiary Imports" / "goblin_warrior.glb"
if not SOURCE.is_file():
    SOURCE = ROOT.parent / "Beastiary Review" / "goblin_warrior.glb"
OUTPUT = ROOT / "godot" / "assets" / "goblin_warrior.glb"
if "--" in sys.argv:
    args = [arg for arg in sys.argv[sys.argv.index("--") + 1:] if arg != "--preview"]
    if args:
        SOURCE = pathlib.Path(args[0])
    if len(args) > 1:
        OUTPUT = pathlib.Path(args[1])

assert SOURCE.is_file(), "Missing Goblin Warrior source: " + str(SOURCE)
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
meshes = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
assert len(meshes) == 1, "Unexpected multi-mesh source: %s" % len(meshes)
mesh = meshes[0]
mesh.name = "GoblinWarrior"
bpy.context.view_layer.update()
points = [mesh.matrix_world @ v.co for v in mesh.data.vertices]
assert 0.95 <= max(p.z for p in points) <= 1.05, "Source proportions changed: re-audit rig positions"

# A compact humanoid armature, with hands holding sword and shield.
arm_data = bpy.data.armatures.new("GoblinWarriorSkeleton")
arm = bpy.data.objects.new("GoblinWarriorRig", arm_data)
bpy.context.collection.objects.link(arm)
bpy.context.view_layer.objects.active = arm
arm.select_set(True)
bpy.ops.object.mode_set(mode="EDIT")
bones = arm_data.edit_bones
def bone(name, start, end, parent=None):
    b = bones.new(name)
    b.head = start
    b.tail = end
    if parent:
        b.parent = bones[parent]
        b.use_connect = False
    return b

bone("Hips", (0,0,.37), (0,0,.48))
bone("Spine", (0,0,.48), (0,0,.75), "Hips")
bone("Head", (0,-.015,.75), (0,-.02,.98), "Spine")
for side, s in (("L",-1),("R",1)):
    bone(side+"UpperArm", (s*.145,0,.705), (s*.22,-.015,.52), "Spine")
    bone(side+"Forearm", (s*.22,-.015,.52), (s*.285,-.12,.42), side+"UpperArm")
    bone(side+"Hand", (s*.285,-.12,.42), (s*.30,-.14,.36), side+"Forearm")
    bone(side+"Thigh", (s*.105,0,.38), (s*.125,-.01,.22), "Hips")
    bone(side+"Shin", (s*.125,-.01,.22), (s*.145,-.02,.085), side+"Thigh")
    bone(side+"Foot", (s*.145,-.02,.085), (s*.145,-.11,.04), side+"Shin")
bpy.ops.object.mode_set(mode="OBJECT")

# Source includes separate disconnected prop components. Keep shields and blades
# rigid so they don't crumple with weighted body deformation.
parents = list(range(len(mesh.data.vertices)))
def find(i):
    while parents[i] != i:
        parents[i] = parents[parents[i]]
        i = parents[i]
    return i
for e in mesh.data.edges:
    a,b = e.vertices
    ra,rb = find(a), find(b)
    if ra != rb:
        parents[ra] = rb
parts = {}
for i, v in enumerate(mesh.data.vertices):
    parts.setdefault(find(i), []).append(i)
rigid = {}
for indices in parts.values():
    if len(indices) < 14:
        continue
    xs = [points[i].x for i in indices]
    ys = [points[i].y for i in indices]
    zs = [points[i].z for i in indices]
    mean_x = sum(xs)/len(xs)
    mean_y = sum(ys)/len(ys)
    mean_z = sum(zs)/len(zs)
    # Blade on negative X, projecting forward; shield on positive X.
    if mean_x < -.30 and mean_y < -.085 and .30 < mean_z < .76:
        target = "LHand"
    elif mean_x > .275 and .31 < mean_z < .73:
        target = "RHand"
    else:
        continue
    for i in indices:
        rigid[i] = target
print("Rigid prop vertices", len(rigid), "components",len(parts))

names = [b.name for b in arm_data.bones]
groups = {name: mesh.vertex_groups.new(name=name) for name in names}
def blend(a,b,t):
    t=max(0.0,min(1.0,t))
    return {a:1-t,b:t}
def weights(point, index):
    x,y,z = point
    prop = rigid.get(index)
    if prop == "RHand":
        return {prop: 1.0}
    if z < .37:
        side="L" if x < -.005 else "R"
        # Central cloth/coat belongs to the hips; legs remain independently movable.
        leg=max(0.0,min(1.0,(abs(x)-.024)/.075))
        if y < -.095 and z > .23:
            leg*=.25
        if z < .13:
            part=side+"Foot"
        elif z < .24:
            part=side+"Shin"
        else:
            part=side+"Thigh"
        return {"Hips":1-leg,part:leg}
    if z > .785:
        return blend("Spine","Head",(z-.775)/.095)
    # Blend across the shoulder, elbow and wrist. A height threshold at
    # either joint makes adjacent shells follow different rigid transforms.
    side = "L" if x < 0 else "R"
    def ramp(value, low, high):
        t = max(0.0, min(1.0, (value - low) / (high - low)))
        return t * t * (3.0 - 2.0 * t)

    reach = ramp(abs(x), .105, .205) * (1.0 - ramp(z, .735, .805))
    upper = ramp(z, .47, .59)
    hand = (1.0 - upper) * (1.0 - ramp(z, .395, .46))
    forearm = 1.0 - upper - hand
    hips = 1.0 - ramp(z, .39, .50)
    result = {
        "Hips": (1.0 - reach) * hips,
        "Spine": (1.0 - reach) * (1.0 - hips),
        side + "UpperArm": reach * upper,
        side + "Forearm": reach * forearm,
        side + "Hand": reach * hand,
    }

    # The sword component also reaches back into the upper arm. Keep its
    # forward blade rigid and fade to arm weights across the rear overlap.
    if prop == "LHand":
        rigid_share = 1.0 - ramp(y, -.12, -.045) * ramp(z, .48, .58)
        result = {name: weight * (1.0 - rigid_share) for name, weight in result.items()}
        result[prop] += rigid_share
    return result

# glTF splits vertices at UV and normal boundaries. Identical source
# positions must use the same weights or those seams can open in motion.
coincident = {}
for index, point in enumerate(points):
    key = tuple(round(component, 6) for component in point)
    coincident.setdefault(key, []).append(index)
for indices in coincident.values():
    combined = {}
    for index in indices:
        for name, weight in weights(points[index], index).items():
            combined[name] = combined.get(name, 0.0) + weight
    strongest = sorted(combined.items(), key=lambda item: -item[1])[:4]
    total = sum(weight for _, weight in strongest)
    for name, weight in strongest:
        if weight > 1e-6:
            groups[name].add(indices, weight / total, "REPLACE")
modifier = mesh.modifiers.new("Goblin Warrior Skin", "ARMATURE")
modifier.object = arm
mesh.parent = arm
mesh.matrix_parent_inverse = arm.matrix_world.inverted()

# Short, explicitly named clips suitable for the Godot AnimationPlayer.
pose = arm.pose.bones
for b in pose:
    b.rotation_mode="XYZ"
def animation(name, frames):
    action = bpy.data.actions.new(name=name)
    arm.animation_data_create()
    arm.animation_data.action=action
    for frame, rotations in frames:
        for b in pose:
            b.rotation_euler = (0,0,0)
            b.location = (0,0,0)
        for bone_name, degrees in rotations.items():
            pose[bone_name].rotation_euler = tuple(math.radians(v) for v in degrees)
        for b in pose:
            b.keyframe_insert(data_path="rotation_euler",frame=frame,group=b.name)
    action.frame_range=(frames[0][0],frames[-1][0])
    arm.animation_data.action=None
    track=arm.animation_data.nla_tracks.new()
    track.name=name
    strip=track.strips.new(name, 1, action)
    strip.action_frame_start=frames[0][0]
    strip.action_frame_end=frames[-1][0]
    strip.repeat=1.0
    track.mute=True
    print("ANIMATION",name,frames[0][0],frames[-1][0], "tracks",len(track.strips))
animation("Idle",[(1,{}),(13,{"Spine":(1.3,0,0),"Head":(-1.2,0,0)}),(25,{})])
animation("Walk",[(1,{"LThigh":(21,0,0),"RThigh":(-21,0,0),"LShin":(-8,0,0),"RShin":(14,0,0),"LUpperArm":(-11,0,0),"RUpperArm":(11,0,0)}),
                  (7,{"LThigh":(0,0,0),"RThigh":(0,0,0),"LShin":(12,0,0),"RShin":(12,0,0)}),
                  (13,{"LThigh":(-21,0,0),"RThigh":(21,0,0),"LShin":(14,0,0),"RShin":(-8,0,0),"LUpperArm":(11,0,0),"RUpperArm":(-11,0,0)}),
                  (19,{"LShin":(12,0,0),"RShin":(12,0,0)}),
                  (25,{"LThigh":(21,0,0),"RThigh":(-21,0,0),"LShin":(-8,0,0),"RShin":(14,0,0),"LUpperArm":(-11,0,0),"RUpperArm":(11,0,0)})])
animation("Attack",[(1,{}),(7,{"LUpperArm":(-25,10,8),"LForearm":(-12,0,0),"Spine":(0,0,-5)}),
                   (12,{"LUpperArm":(25,-18,-12),"LForearm":(23,0,0),"Spine":(0,0,6)}),
                   (22,{})])
for b in pose:
    b.rotation_euler = (0,0,0)

bpy.ops.object.select_all(action="DESELECT")
arm.select_set(True)
mesh.select_set(True)
bpy.context.view_layer.objects.active=arm
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
bpy.ops.export_scene.gltf(filepath=str(OUTPUT),export_format="GLB",
    use_selection=True, export_animations=True,export_animation_mode="NLA_TRACKS",
    export_nla_strips=True,export_apply=False)
print("EXPORTED",OUTPUT,OUTPUT.stat().st_size)
# Visual regression previews for the inferred rig, not included in Android.
if "--preview" in sys.argv:
    scene=bpy.context.scene
    scene.render.engine="BLENDER_EEVEE"
    scene.render.resolution_x=720
    scene.render.resolution_y=820
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    scene.world.color=(.13,.13,.13)
    data=bpy.data.lights.new("RigPreviewKey","AREA")
    data.energy=530
    data.size=3
    light=bpy.data.objects.new("RigPreviewKey",data)
    scene.collection.objects.link(light)
    light.location=Vector((1.5,-2.0,2.5))
    light.rotation_euler=(Vector((0,0,.5))-light.location).to_track_quat("-Z","Y").to_euler()
    cam_data=bpy.data.cameras.new("RigPreviewCamera")
    cam=bpy.data.objects.new("RigPreviewCamera",cam_data)
    scene.collection.objects.link(cam)
    cam.location=(1.45,-2.0,1.2)
    cam.rotation_euler=(Vector((0,-.05,.52))-cam.location).to_track_quat("-Z","Y").to_euler()
    cam_data.type="ORTHO"
    cam_data.ortho_scale=1.58
    scene.camera=cam
    for name,frame in (("Idle",13),("Walk",13),("Attack",12)):
        for track in arm.animation_data.nla_tracks:
            track.mute=track.name!=name
        scene.frame_set(frame)
        scene.render.filepath=str(OUTPUT.parent / ("goblin_warrior_"+name.lower()+"_preview.png"))
        bpy.ops.render.render(write_still=True)
        print("RENDERED",name,scene.render.filepath)
