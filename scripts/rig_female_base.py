"""Rig the supplied Female Base and bake the existing Goblin Warrior clips for review.

blender -b --python scripts/rig_female_base.py -- female_source.glb goblin_warrior.glb output.glb
"""
import sys
import bpy
from mathutils import Vector

source_path, donor_path, output_path = sys.argv[sys.argv.index("--") + 1:]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=donor_path)
donor = next(obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE")
donor.animation_data_clear()
animation = donor.animation_data_create()
fps = bpy.context.scene.render.fps / bpy.context.scene.render.fps_base
samples = {}
for name in ("Idle", "Walk", "Attack"):
    action = bpy.data.actions[name]
    animation.action = action
    start, end = map(round, action.frame_range)
    samples[name] = []
    for frame in range(start, end + 1):
        bpy.context.scene.frame_set(frame)
        samples[name].append({bone.name: bone.matrix_basis.to_quaternion() for bone in donor.pose.bones})
axes = {bone.name: (donor.matrix_world @ bone.matrix_local).to_quaternion() for bone in donor.data.bones}
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=source_path)
body = next(obj for obj in bpy.context.scene.objects if obj.type == "MESH")
bpy.context.view_layer.objects.active = body
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
floor = min(vertex.co.z for vertex in body.data.vertices)
height = max(vertex.co.z for vertex in body.data.vertices) - floor
for vertex in body.data.vertices:
    vertex.co.z -= floor
body.data.update()
body.name = "Female Base"
rig = bpy.data.objects.new("Female Base Rig", bpy.data.armatures.new("Female Base Skeleton"))
bpy.context.collection.objects.link(rig)
bpy.context.view_layer.objects.active = rig
rig.select_set(True)
bpy.ops.object.mode_set(mode="EDIT")
mapping = {}

def bone(name, head, tail, parent=None, donor_name=None):
    edit = rig.data.edit_bones.new(name)
    edit.head, edit.tail = Vector(head) * height, Vector(tail) * height
    if parent:
        edit.parent = rig.data.edit_bones[parent]
    else:
        edit.use_deform = False
    if donor_name:
        mapping[name] = donor_name

bone("Root", (0, 0, 0), (0, 0, .08))
bone("Hips", (0, .025, .52), (0, .018, .59), "Root", "Hips")
bone("Spine", (0, .018, .59), (0, .01, .72), "Hips", "Spine")
bone("Chest", (0, .01, .72), (0, .01, .805), "Spine")
bone("Neck", (0, .01, .805), (0, .003, .875), "Chest")
bone("Head", (0, .003, .875), (0, .003, .985), "Neck", "Head")
for side, sign, donor_side in (("Left", 1, "R"), ("Right", -1, "L")):
    def point(x, y, z):
        return sign * x, y, z
    shoulder = point(.102, .006, .794)
    elbow = point(.153, -.003, .657)
    wrist = point(.203, -.019, .506)
    hip = point(.062, .025, .507)
    knee = point(.092, -.011, .31)
    ankle = point(.118, .001, .066)
    ball = point(.123, -.071, .019)
    bone(side + "Shoulder", (0, .01, .805), shoulder, "Chest")
    bone(side + "UpperArm", shoulder, elbow, side + "Shoulder", donor_side + "UpperArm")
    bone(side + "LowerArm", elbow, wrist, side + "UpperArm", donor_side + "Forearm")
    bone(side + "Hand", wrist, point(.218, -.035, .438), side + "LowerArm", donor_side + "Hand")
    bone(side + "UpperLeg", hip, knee, "Hips", donor_side + "Thigh")
    bone(side + "LowerLeg", knee, ankle, side + "UpperLeg", donor_side + "Shin")
    bone(side + "Foot", ankle, ball, side + "LowerLeg", donor_side + "Foot")
    bone(side + "Toes", ball, point(.124, -.096, .016), side + "Foot")
bpy.ops.object.mode_set(mode="OBJECT")

def smooth(value, start, end):
    t = max(0.0, min(1.0, (value - start) / (end - start)))
    return t * t * (3.0 - 2.0 * t)

def skin_weights(point):
    x, y, z = point / height
    side = "Left" if x >= 0 else "Right"
    lateral = abs(x)
    leg = 1 - smooth(z, .455, .555)
    # The arm moves outward as it descends in the source A-pose.
    seam = .084 + .29 * max(0, .8 - z)
    arm = smooth(lateral, seam, seam + .029) * smooth(z, .37, .425) * (1 - smooth(z, .795, .85))
    hip = smooth(z, .52, .63)
    chest = smooth(z, .67, .75)
    neck = smooth(z, .803, .859)
    head = smooth(z, .858, .905)
    lower = smooth(z, .26, .36)
    thigh = smooth(z, .272, .352)
    foot = 1 - smooth(z, .043, .103)
    toes = (1 - smooth(y, -.075, -.05)) * (1 - smooth(z, .028, .06))
    upper_arm = smooth(z, .61, .705)
    hand = 1 - smooth(z, .48, .535)
    torso = (1 - arm) * (1 - leg)
    leg *= 1 - arm
    return {
        "Hips": torso * (1 - hip),
        "Spine": torso * hip * (1 - chest),
        "Chest": torso * chest * (1 - neck),
        "Neck": torso * neck * (1 - head),
        "Head": torso * head,
        side + "UpperArm": arm * upper_arm,
        side + "LowerArm": arm * (1 - upper_arm) * (1 - hand),
        side + "Hand": arm * (1 - upper_arm) * hand,
        side + "UpperLeg": leg * thigh,
        side + "LowerLeg": leg * (1 - lower) * (1 - foot),
        side + "Foot": leg * foot * (1 - toes),
        side + "Toes": leg * foot * toes,
    }

groups = {bone.name: body.vertex_groups.new(name=bone.name) for bone in rig.data.bones if bone.use_deform}
for vertex in body.data.vertices:
    weights = sorted(skin_weights(vertex.co).items(), key=lambda item: -item[1])[:4]
    total = sum(weight for _, weight in weights)
    assert total > 0
    for name, weight in weights:
        if weight > 0:
            groups[name].add([vertex.index], weight / total, "REPLACE")
body.parent = rig
skin = body.modifiers.new("Skin", "ARMATURE")
skin.object = rig

scene = bpy.context.scene
scene.render.fps = round(fps)
scene.render.fps_base = scene.render.fps / fps
animation = rig.animation_data_create()
for name, frames in samples.items():
    action = bpy.data.actions.new(name)
    animation.action = action
    for target, source in mapping.items():
        pose = rig.pose.bones[target]
        pose.rotation_mode = "QUATERNION"
        change = pose.bone.matrix_local.to_quaternion().inverted() @ axes[source]
        for frame, sample in enumerate(frames, 1):
            pose.rotation_quaternion = change @ sample[source] @ change.inverted()
            pose.keyframe_insert(data_path="rotation_quaternion", frame=frame, group=target)
    animation.action = None
    track = animation.nla_tracks.new()
    track.name = name
    track.strips.new(name, 1, action)
    track.mute = True
for pose in rig.pose.bones:
    pose.rotation_quaternion = (1, 0, 0, 0)
scene.frame_set(1)
bpy.ops.object.select_all(action="DESELECT")
body.select_set(True)
rig.select_set(True)
bpy.ops.export_scene.gltf(filepath=output_path, export_format="GLB", use_selection=True,
                         export_animation_mode="NLA_TRACKS", export_animations=True)
