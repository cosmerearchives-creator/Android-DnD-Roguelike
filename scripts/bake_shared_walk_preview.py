"""Bake the existing Goblin Warrior Walk onto Classic Study for Bestiary review.

Run: blender -b --python scripts/bake_shared_walk_preview.py -- SOURCE TARGET OUTPUT
SOURCE is goblin_warrior.glb. TARGET is the existing classic_study.blend.
"""
import bpy
import sys


source_path, target_path, output_path = sys.argv[sys.argv.index("--") + 1:]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=source_path)
source = next(obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE")
walk = bpy.data.actions["Walk"]
source.animation_data_clear()
source.animation_data_create().action = walk
first, last = map(round, walk.frame_range)
source_fps = bpy.context.scene.render.fps / bpy.context.scene.render.fps_base
samples = []
for frame in range(first, last + 1):
    bpy.context.scene.frame_set(frame)
    samples.append({bone.name: bone.matrix_basis.to_quaternion() for bone in source.pose.bones})
source_axes = {
    bone.name: (source.matrix_world @ bone.matrix_local).to_quaternion()
    for bone in source.data.bones
}
assert len(samples) > 2 and any(
    sample["LThigh"].rotation_difference(samples[0]["LThigh"]).angle > 0.1
    for sample in samples
), "Walk must animate the source legs"

bpy.ops.wm.open_mainfile(filepath=target_path, use_scripts=False)
scene = bpy.context.scene
scene.render.fps = round(source_fps)
scene.render.fps_base = scene.render.fps / source_fps
parts = [obj for obj in scene.objects if obj.type == "MESH"]
rig = bpy.data.objects.new("ClassicStudyRig", bpy.data.armatures.new("ClassicStudySkeleton"))
scene.collection.objects.link(rig)
bpy.context.view_layer.objects.active = rig
rig.select_set(True)
bpy.ops.object.mode_set(mode="EDIT")
mapping = {}

def add(name, start, end, parent=None, source_name=None):
    bone = rig.data.edit_bones.new(name)
    bone.head, bone.tail = start, end
    if parent:
        bone.parent = rig.data.edit_bones[parent]
    if source_name:
        mapping[name] = source_name

add("Root", (0, 0, 0), (0, 0, .15))
add("Hips", (0, 0, 1.15), (0, 0, 1.3), "Root", "Hips")
add("Spine", (0, 0, 1.3), (0, 0, 1.73), "Hips", "Spine")
add("Head", (0, 0, 1.84), (0, 0, 2.16), "Spine", "Head")
# Both source meshes face Blender -Y. Positive X is the character's left.
for side, sign, source_side in (("Left", 1, "R"), ("Right", -1, "L")):
    shoulder = (sign * .216, 0, 1.66)
    elbow = (sign * .292, 0, 1.34)
    wrist = (sign * .338, -.025, 1.08)
    hip = (sign * .133, 0, 1.08)
    knee = (sign * .133, -.015, .56)
    ankle = (sign * .133, .015, .18)
    add(side + "UpperArm", shoulder, elbow, "Spine", source_side + "UpperArm")
    add(side + "LowerArm", elbow, wrist, side + "UpperArm", source_side + "Forearm")
    add(side + "Hand", wrist, (sign * .34, -.04, 1.0), side + "LowerArm", source_side + "Hand")
    add(side + "UpperLeg", hip, knee, "Hips", source_side + "Thigh")
    add(side + "LowerLeg", knee, ankle, side + "UpperLeg", source_side + "Shin")
    add(side + "Foot", ankle, (sign * .133, -.13, .085), side + "LowerLeg", source_side + "Foot")
bpy.ops.object.mode_set(mode="OBJECT")

def blend(lower, upper, height, low, high):
    t = min(1.0, max(0.0, (height - low) / (high - low)))
    return {lower: 1.0 - t, upper: t}

for obj in parts:
    matrix = obj.matrix_world.copy()
    side = "Left" if sum((matrix @ v.co).x for v in obj.data.vertices) > 0 else "Right"
    groups = {}
    for vertex in obj.data.vertices:
        height = (matrix @ vertex.co).z
        name = obj.name
        if name.startswith("Continuous trouser leg"):
            weights = blend(side + "LowerLeg", side + "UpperLeg", height, .47, .64)
            if height < .27:
                weights = blend(side + "Foot", side + "LowerLeg", height, .18, .27)
        elif name.startswith("Continuous arm"):
            weights = blend(side + "LowerArm", side + "UpperArm", height, 1.26, 1.43)
        else:
            bone = "Spine"
            if name.startswith(("Boot cuff", "Fitted boot")):
                bone = side + "Foot"
            elif name.startswith("Small knee plate"):
                bone = side + "LowerLeg"
            elif name.startswith("Bracer"):
                bone = side + "LowerArm"
            elif name.startswith("Glove"):
                bone = side + "Hand"
            elif name.startswith("Shoulder armor"):
                bone = side + "UpperArm"
            elif name.startswith(("Belt clasp", "Waist belt", "Fitted hip panel")):
                bone = "Hips"
            elif name.startswith(("Brow", "Eye", "Face", "Hair", "Mouth", "Nose", "Pointed ear", "Swept hair", "Tied hair")):
                bone = "Head"
            weights = {bone: 1.0}
        for bone, weight in weights.items():
            if weight > 0.0:
                if bone not in groups:
                    groups[bone] = obj.vertex_groups.new(name=bone)
                groups[bone].add([vertex.index], weight, "REPLACE")
    obj.parent = rig
    obj.matrix_world = matrix
    modifier = obj.modifiers.new("Classic Study Skin", "ARMATURE")
    modifier.object = rig

action = bpy.data.actions.new("Walk")
rig.animation_data_create().action = action
for target_name, source_name in mapping.items():
    bone = rig.pose.bones[target_name]
    bone.rotation_mode = "QUATERNION"
    # Express the source's rest-relative delta in the target bone's rest axes.
    change = bone.bone.matrix_local.to_quaternion().inverted() @ source_axes[source_name]
    for frame, sample in enumerate(samples, 1):
        bone.rotation_quaternion = change @ sample[source_name] @ change.inverted()
        bone.keyframe_insert(data_path="rotation_quaternion", frame=frame, group=target_name)
rig.animation_data.action = None
track = rig.animation_data.nla_tracks.new()
track.name = "Walk"
track.strips.new("Walk", 1, action)
scene.frame_start, scene.frame_end = 1, len(samples)
scene.frame_set(1)
bpy.ops.object.select_all(action="DESELECT")
for obj in [rig, *parts]:
    obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=output_path, export_format="GLB", use_selection=True,
                         export_animation_mode="NLA_TRACKS", export_animations=True)
