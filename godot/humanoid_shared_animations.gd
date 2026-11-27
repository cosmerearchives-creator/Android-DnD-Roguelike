extends RefCounted
## Retargets the 20 original Mixamo clips to a compatible humanoid Skeleton3D.
## Source GLB contains animation data and a tiny marker mesh, not character art.
const SOURCE := preload("res://assets/humanoid_mixamo_animation_library.glb")
const CLAW_SOURCE := preload("res://assets/twig_blight.glb")
const CLAW_NAME := "Attack_Claw_01_BackSwipe"
# Cached once per rig/rest pose so crowds can share immutable clip resources.
static var _library_cache: Dictionary = {}
const LOOPING := ["Running", "Walking", "Walking_Woman", "Sneaky_Walk", "Concentrate_Idle"]
const ALIASES := {
    "Hips": ["Hips"], "Spine": ["Spine"], "Spine1": ["Chest", "Spine1"],
    "Spine2": ["UpperChest", "Spine2"], "Neck": ["Neck"], "Head": ["Head"],
    "LeftShoulder": ["LeftShoulder", "LShoulder"],
    "RightShoulder": ["RightShoulder", "RShoulder"],
    "LeftArm": ["LeftUpperArm", "LUpperArm", "LeftArm"],
    "RightArm": ["RightUpperArm", "RUpperArm", "RightArm"],
    "LeftForeArm": ["LeftLowerArm", "LeftForearm", "LForearm"],
    "RightForeArm": ["RightLowerArm", "RightForearm", "RForearm"],
    "LeftHand": ["LeftHand", "LHand"],
    "RightHand": ["RightHand", "RHand"],
    "LeftUpLeg": ["LeftUpperLeg", "LThigh", "LeftThigh"],
    "RightUpLeg": ["RightUpperLeg", "RThigh", "RightThigh"],
    "LeftLeg": ["LeftLowerLeg", "LShin", "LeftShin"],
    "RightLeg": ["RightLowerLeg", "RShin", "RightShin"],
    "LeftFoot": ["LeftFoot", "LFoot"],
    "RightFoot": ["RightFoot", "RFoot"],
    "LeftToeBase": ["LeftToes", "LToes", "LeftToeBase"],
    "RightToeBase": ["RightToes", "RToes", "RightToeBase"]
}

static func find_skeleton(node: Node) -> Skeleton3D:
    if node is Skeleton3D:
        return node
    for child in node.get_children():
        var candidate := find_skeleton(child)
        if candidate != null:
            return candidate
    return null

static func find_animator(node: Node) -> AnimationPlayer:
    if node is AnimationPlayer:
        return node
    for child in node.get_children():
        var candidate := find_animator(child)
        if candidate != null:
            return candidate
    return null

static func _source_bone_name(bone: String) -> String:
    return bone.trim_prefix("mixamorig_").trim_prefix("mixamorig:")

static func _target_bone(source_name: String, destination: Skeleton3D) -> int:
    if destination.find_bone("Bone_018") >= 0 and destination.find_bone("Bone_023") >= 0:
        var custom: Dictionary = {"LeftHand": "Bone_018", "RightHand": "Bone_023", "LeftHandThumb1": "Bone_031", "LeftHandThumb2": "Bone_030", "LeftHandThumb3": "Bone_029", "LeftHandThumb4": "Bone_028", "LeftHandIndex1": "Bone_043", "LeftHandIndex2": "Bone_042", "LeftHandIndex3": "Bone_041", "LeftHandIndex4": "Bone_040", "LeftHandMiddle1": "Bone_039", "LeftHandMiddle2": "Bone_038", "LeftHandMiddle3": "Bone_037", "LeftHandMiddle4": "Bone_036", "LeftHandRing1": "Bone_035", "LeftHandRing2": "Bone_034", "LeftHandRing3": "Bone_033", "LeftHandRing4": "Bone_032", "LeftHandPinky1": "Bone_047", "LeftHandPinky2": "Bone_046", "LeftHandPinky3": "Bone_045", "LeftHandPinky4": "Bone_044", "RightHandThumb1": "Bone_069", "RightHandThumb2": "Bone_068", "RightHandThumb3": "Bone_067", "RightHandThumb4": "Bone_066", "RightHandIndex1": "Bone_065", "RightHandIndex2": "Bone_064", "RightHandIndex3": "Bone_063", "RightHandIndex4": "Bone_062", "RightHandMiddle1": "Bone_061", "RightHandMiddle2": "Bone_060", "RightHandMiddle3": "Bone_059", "RightHandMiddle4": "Bone_058", "RightHandRing1": "Bone_053", "RightHandRing2": "Bone_052", "RightHandRing3": "Bone_051", "RightHandRing4": "Bone_050", "RightHandPinky1": "Bone_057", "RightHandPinky2": "Bone_056", "RightHandPinky3": "Bone_055", "RightHandPinky4": "Bone_054"}
        if custom.has(source_name):
            return destination.find_bone(custom[source_name])
    var candidates: Array = [source_name]
    candidates.append_array(ALIASES.get(source_name, []))
    for candidate in candidates:
        for candidate_name in [str(candidate), "mixamorig:" + str(candidate), "mixamorig_" + str(candidate)]:
            var index := destination.find_bone(candidate_name)
            if index >= 0:
                return index
    return -1

# Select the actual skinned hand root when Mixamo's nominal Hand is a leaf.
# Custom rig families expose their fingers as children of a sibling of Hand.
# This is anatomical hierarchy detection, never a model filename or Bone_NNN rule.
static func _actual_hand_root(skeleton: Skeleton3D, side: String) -> int:
    var arm_index := _named_bone(skeleton, side + "ForeArm")
    if arm_index < 0:
        return -1
    var nominal := _named_bone(skeleton, side + "Hand")
    if nominal < 0:
        return -1
    var chosen := nominal
    var highest := 0
    for index in skeleton.get_bone_count():
        if skeleton.get_bone_parent(index) != arm_index:
            continue
        var descendants := _descendant_bones(skeleton, index)
        if descendants.size() > highest:
            highest = descendants.size()
            chosen = index
    return chosen

static func _named_bone(skeleton: Skeleton3D, canonical: String) -> int:
    for prefix in ["mixamorig:", "mixamorig_", ""]:
        var index := skeleton.find_bone(prefix + canonical)
        if index >= 0:
            return index
    return -1

static func _descendant_bones(skeleton: Skeleton3D, root: int) -> Array[int]:
    var result: Array[int] = []
    var stack: Array[int] = [root]
    while not stack.is_empty():
        var parent: int = stack.pop_back()
        for index in skeleton.get_bone_count():
            if skeleton.get_bone_parent(index) == parent:
                result.append(index)
                stack.append(index)
    return result

# Derive anatomical finger chains from the actual hand root. Variants with 2
# phalanges are allowed; missing groups remain unmapped rather than guessed.
# Five longest chains provide five digits; thumb distinguished by shortest
# effective length, with the other four ordered by the first joint's position.
static func _custom_finger_mapping(skeleton: Skeleton3D, side: String) -> Dictionary:
    var root := _actual_hand_root(skeleton, side)
    if root < 0:
        return {}
    var branches: Array = []
    for child in skeleton.get_bone_count():
        if skeleton.get_bone_parent(child) != root:
            continue
        var chain: Array[int] = [child]
        var current := child
        while chain.size() < 4:
            var children: Array[int] = []
            for index in skeleton.get_bone_count():
                if skeleton.get_bone_parent(index) == current:
                    children.append(index)
            if children.size() != 1:
                break
            current = children[0]
            chain.append(current)
        if chain.size() >= 3:
            branches.append(chain)
    if branches.size() < 5:
        return {}
    # Identify thumb among the five actual finger chains by its joint layout.
    # 2-bone decorative branches and weapons are excluded.
    if branches.size() != 5:
        return {}
    var min_total := INF
    var thumb_index := -1
    for i in branches.size():
        var chain: Array = branches[i]
        var length := 0.0
        for joint in chain:
            length += skeleton.get_bone_rest(joint).origin.length()
        if length < min_total:
            min_total = length
            thumb_index = i
    var thumb_chain: Array = branches[thumb_index]
    branches.remove_at(thumb_index)
    # Order remaining digits by distance from the thumb in local hand space.
    var thumb_pos := skeleton.get_bone_rest(thumb_chain[0]).origin
    branches.sort_custom(func(a, b):
        return skeleton.get_bone_rest(a[0]).origin.distance_to(thumb_pos) < skeleton.get_bone_rest(b[0]).origin.distance_to(thumb_pos))
    var result := {}
    for label_index in 5:
        var label: String = ["Thumb", "Index", "Middle", "Ring", "Pinky"][label_index]
        var chain: Array = thumb_chain if label_index == 0 else branches[label_index - 1]
        for i in mini(chain.size(), 4):
            result[side + "Hand" + label + str(i + 1)] = chain[i]
    return result

static func _build_mapping(origin: Skeleton3D, destination: Skeleton3D) -> Dictionary:
    var custom := {}
    for side in ["Left", "Right"]:
        var root := _actual_hand_root(destination, side)
        var nominal := _named_bone(destination, side + "Hand")
        if root >= 0 and root != nominal:
            custom[side + "Hand"] = root
            custom.merge(_custom_finger_mapping(destination, side), true)
    var result := {}
    for idx in origin.get_bone_count():
        var source := origin.get_bone_name(idx)
        var canonical := _source_bone_name(source)
        var target: int = custom.get(canonical, _target_bone(canonical, destination))
        if target >= 0:
            result[source] = [idx, target]
    return result

static func validate_humanoid(origin: Skeleton3D, destination: Skeleton3D, mappings: Dictionary) -> Dictionary:
    var required := ["Hips", "Spine", "Spine1", "Spine2", "Neck", "Head",
        "LeftShoulder", "RightShoulder", "LeftArm", "RightArm",
        "LeftForeArm", "RightForeArm", "LeftHand", "RightHand",
        "LeftUpLeg", "RightUpLeg", "LeftLeg", "RightLeg", "LeftFoot", "RightFoot"]
    var absent: Array[String] = []
    var used := {}
    for name in required:
        var source_id := _named_bone(origin, name)
        if source_id < 0 or not mappings.has(origin.get_bone_name(source_id)):
            absent.append(name)
        else:
            var target_id: int = mappings[origin.get_bone_name(source_id)][1]
            if used.has(target_id):
                absent.append(name + " (duplicate target)")
            used[target_id] = true
    var fingers := 0
    for mapped_name in mappings:
        if _source_bone_name(mapped_name).contains("Hand") and not _source_bone_name(mapped_name).ends_with("Hand"):
            fingers += 1
    # Fingers required on models with a multi-branch custom hand; simple
    # appendage tips (e.g. Twig Blight) are explicitly supported.
    for side in ["Left", "Right"]:
        var hand := _actual_hand_root(destination, side)
        if hand < 0:
            absent.append(side + "Hand (missing)")
        else:
            var branches := 0
            for id in destination.get_bone_count():
                if destination.get_bone_parent(id) == hand:
                    branches += 1
            if branches >= 5:
                var custom := _custom_finger_mapping(destination, side)
                if custom.size() < 15:
                    absent.append(side + " custom fingers (incomplete)")
                else:
                    for finger in custom:
                        var source_id := _named_bone(origin, finger)
                        if source_id >= 0 and (not mappings.has(origin.get_bone_name(source_id))
                                or mappings[origin.get_bone_name(source_id)][1] != custom[finger]):
                            absent.append(finger)
    return {"valid": absent.is_empty(), "missing": absent, "mapped": mappings.size(), "fingers": fingers}

static func _rest_height(skeleton: Skeleton3D) -> float:
    var hips := skeleton.find_bone("Hips")
    if hips < 0:
        hips = skeleton.find_bone("mixamorig_Hips")
    if hips < 0:
        hips = skeleton.find_bone("mixamorig:Hips")
    if hips >= 0:
        return maxf(skeleton.get_bone_global_rest(hips).origin.y, 0.25)
    return 1.0

## Returns a separate AnimationPlayer, leaving existing imported clips untouched.
## Motion displacement in X/Z is removed for gameplay-controlled movement.
static func attach(model: Node3D, in_place: bool = true) -> AnimationPlayer:
    var existing := model.get_node_or_null("SharedHumanoidAnimations") as AnimationPlayer
    if existing != null:
        return existing
    var destination := find_skeleton(model)
    if destination == null:
        return null
    # The canonical human already contains the original animation tracks.
    # Copy the native library and preserve its root path, keys and track types.
    var native := find_animator(model)
    if native != null and native.has_animation("Walking") and native.has_animation("Crossbow"):
        var direct := AnimationPlayer.new()
        direct.name = "SharedHumanoidAnimations"
        model.add_child(direct)
        direct.root_node = direct.get_path_to(native.get_node(native.root_node))
        for lib_name in native.get_animation_library_list():
            direct.add_animation_library(lib_name, native.get_animation_library(lib_name))
        var idle_library := AnimationLibrary.new()
        var idle := Animation.new()
        idle.length = 1.5
        idle.loop_mode = Animation.LOOP_LINEAR
        var walk := native.get_animation("Walking")
        for i in walk.get_track_count():
            if walk.track_get_key_count(i) == 0: continue
            var idx := idle.add_track(walk.track_get_type(i))
            idle.track_set_path(idx, walk.track_get_path(i))
            idle.track_insert_key(idx, 0.0, walk.track_get_key_value(i, 0))
        # Keep native library untouched by adding the synthetic idle to a copy.
        var base := direct.get_animation_library("").duplicate() as AnimationLibrary
        base.add_animation("Idle", idle)
        direct.remove_animation_library("")
        direct.add_animation_library("", base)
        var direct_claw := _create_claw_attack(destination, model.get_path_to(destination), in_place)
        if direct_claw != null:
            var direct_library := direct.get_animation_library("").duplicate() as AnimationLibrary
            direct_library.add_animation(CLAW_NAME, direct_claw)
            direct.remove_animation_library("")
            direct.add_animation_library("", direct_library)
        direct.set_meta("mapped_bones", destination.get_bone_count())
        direct.set_meta("native_reference", true)
        native.stop()
        return direct
    var source_instance := SOURCE.instantiate()
    var origin := find_skeleton(source_instance)
    var source_player := find_animator(source_instance)
    if origin == null or source_player == null:
        source_instance.free()
        return null
    var mappings := _build_mapping(origin, destination)
    var validation := validate_humanoid(origin, destination, mappings)
    if not validation.valid:
        push_error("Humanoid import rejected: " + str(validation.missing))
        source_instance.free()
        return null
    var player := AnimationPlayer.new()
    player.name = "SharedHumanoidAnimations"
    model.add_child(player)
    player.root_node = NodePath("..")
    var dest_skeleton_path := String(model.get_path_to(destination))
    var cache_key := dest_skeleton_path + ":" + str(in_place)
    for bone_idx in destination.get_bone_count():
        cache_key += "|" + destination.get_bone_name(bone_idx) + "=" + str(destination.get_bone_rest(bone_idx))
    if _library_cache.has(cache_key):
        player.add_animation_library("", _library_cache[cache_key])
        player.set_meta("mapped_bones", mappings.size())
        source_instance.free()
        return player
    var library := AnimationLibrary.new()
    var scale_ratio := clampf(_rest_height(destination) / _rest_height(origin), 0.3, 3.0)
    for clip_name in source_player.get_animation_list():
        var original := source_player.get_animation(clip_name)
        var clip := Animation.new()
        clip.length = original.length
        clip.loop_mode = Animation.LOOP_LINEAR if clip_name in LOOPING else Animation.LOOP_NONE
        clip.step = original.step
        for track_idx in original.get_track_count():
            var kind := original.track_get_type(track_idx)
            if kind != Animation.TYPE_ROTATION_3D and kind != Animation.TYPE_POSITION_3D:
                continue
            var track_path := original.track_get_path(track_idx)
            if track_path.get_subname_count() == 0:
                continue
            var source_name := String(track_path.get_subname(0))
            if not mappings.has(source_name):
                continue
            var src_bone: int = mappings[source_name][0]
            var dst_bone: int = mappings[source_name][1]
            var canonical := _source_bone_name(source_name)
            # Preserve skeleton proportions: translation changes only on pelvis.
            if kind == Animation.TYPE_POSITION_3D and canonical != "Hips":
                continue
            var converted_idx := clip.add_track(kind)
            clip.track_set_path(converted_idx, NodePath(dest_skeleton_path + ":" + destination.get_bone_name(dst_bone)))
            clip.track_set_interpolation_type(converted_idx, original.track_get_interpolation_type(track_idx))
            var src_local := origin.get_bone_rest(src_bone)
            var dst_local := destination.get_bone_rest(dst_bone)
            var src_global_q := origin.get_bone_global_rest(src_bone).basis.get_rotation_quaternion().normalized()
            var dst_global_q := destination.get_bone_global_rest(dst_bone).basis.get_rotation_quaternion().normalized()
            var src_rest_q := src_local.basis.get_rotation_quaternion().normalized()
            var dst_rest_q := dst_local.basis.get_rotation_quaternion().normalized()
            for key_idx in original.track_get_key_count(track_idx):
                var time := original.track_get_key_time(track_idx, key_idx)
                var value: Variant = original.track_get_key_value(track_idx, key_idx)
                if kind == Animation.TYPE_ROTATION_3D:
                    var relative: Quaternion = src_rest_q.inverse() * (value as Quaternion)
                    var world_delta: Quaternion = src_global_q * relative * src_global_q.inverse()
                    var local_delta: Quaternion = dst_global_q.inverse() * world_delta * dst_global_q
                    value = (dst_rest_q * local_delta).normalized()
                elif kind == Animation.TYPE_POSITION_3D:
                    var local_offset: Vector3 = ((value as Vector3) - src_local.origin) * scale_ratio
                    # Model movement stays controlled by the character controller.
                    if in_place:
                        local_offset.x = 0.0
                        local_offset.z = 0.0
                    value = dst_local.origin + local_offset
                clip.track_insert_key(converted_idx, time, value,
                    original.track_get_key_transition(track_idx, key_idx))
        library.add_animation(clip_name, clip)
    var claw_clip := _create_claw_attack(destination, NodePath(dest_skeleton_path), in_place)
    if claw_clip != null:
        library.add_animation(CLAW_NAME, claw_clip)
    # Model-specific native reference clips override unvalidated retargets.
    # Preserve all native tracks and remap only the skeleton node path.
    var reference := find_animator(model)
    if reference != null and reference.has_animation("Axe_Spin_Attack"):
        for name in reference.get_animation_list():
            var exact := reference.get_animation(name).duplicate(true) as Animation
            var reference_root := reference.get_node(reference.root_node)
            for track in exact.get_track_count():
                var path := exact.track_get_path(track)
                var target := reference_root.get_node_or_null(NodePath(String(path.get_concatenated_names())))
                if target != null:
                    var new_path := String(model.get_path_to(target))
                    if path.get_subname_count() > 0:
                        new_path += ":" + String(path.get_concatenated_subnames())
                    exact.track_set_path(track, NodePath(new_path))
            if library.has_animation(name):
                library.rename_animation(name, "Shared_" + name)
            library.add_animation(name, exact)
        player.set_meta("native_reference_clips", reference.get_animation_list())
    # Standing pose derived once from the first Walking frame. This ensures
    # that every compatible rig has an Idle state without another large asset.
    if library.has_animation("Walking") and not library.has_animation("Idle"):
        var walk_clip := library.get_animation("Walking")
        var idle_clip := Animation.new()
        idle_clip.length = 1.5
        idle_clip.loop_mode = Animation.LOOP_LINEAR
        for track_id in walk_clip.get_track_count():
            if walk_clip.track_get_key_count(track_id) == 0:
                continue
            var hold_track := idle_clip.add_track(walk_clip.track_get_type(track_id))
            idle_clip.track_set_path(hold_track, walk_clip.track_get_path(track_id))
            var hold_pose: Variant = walk_clip.track_get_key_value(track_id, 0)
            idle_clip.track_insert_key(hold_track, 0.0, hold_pose)
            idle_clip.track_insert_key(hold_track, 1.5, hold_pose)
        library.add_animation("Idle", idle_clip)
    player.add_animation_library("", library)
    _library_cache[cache_key] = library
    player.set_meta("mapped_bones", mappings.size())
    source_instance.free()
    return player

static func _create_claw_attack(destination: Skeleton3D, dest_path: NodePath, in_place: bool) -> Animation:
    var dest_skeleton_path := String(dest_path)
    var library := AnimationLibrary.new()
    # Additional canonical humanoid attack, sourced from the Twig Blight's
    # original Shield_Push_Left. It is reusable on any compatible humanoid rig.
    var claw_instance := CLAW_SOURCE.instantiate()
    var claw_skeleton := find_skeleton(claw_instance)
    var claw_player := find_animator(claw_instance)
    if claw_skeleton != null and claw_player != null and claw_player.has_animation("Shield_Push_Left"):
        var claw_native := claw_player.get_animation("Shield_Push_Left")
        var claw_clip := Animation.new()
        claw_clip.length = claw_native.length
        claw_clip.loop_mode = Animation.LOOP_NONE
        claw_clip.step = claw_native.step
        for track_idx in claw_native.get_track_count():
            var kind := claw_native.track_get_type(track_idx)
            if kind != Animation.TYPE_ROTATION_3D and kind != Animation.TYPE_POSITION_3D:
                continue
            var track_path := claw_native.track_get_path(track_idx)
            if track_path.get_subname_count() == 0:
                continue
            var source_name := String(track_path.get_subname(0))
            var canonical := _source_bone_name(source_name)
            var src_bone := claw_skeleton.find_bone(source_name)
            var dst_bone: int = _build_mapping(claw_skeleton, destination).get(source_name, [-1, -1])[1]
            if src_bone < 0 or dst_bone < 0 or (kind == Animation.TYPE_POSITION_3D and canonical != "Hips"):
                continue
            var converted_idx := claw_clip.add_track(kind)
            claw_clip.track_set_path(converted_idx, NodePath(dest_skeleton_path + ":" + destination.get_bone_name(dst_bone)))
            claw_clip.track_set_interpolation_type(converted_idx, claw_native.track_get_interpolation_type(track_idx))
            var src_local := claw_skeleton.get_bone_rest(src_bone)
            var dst_local := destination.get_bone_rest(dst_bone)
            var src_global_q := claw_skeleton.get_bone_global_rest(src_bone).basis.get_rotation_quaternion().normalized()
            var dst_global_q := destination.get_bone_global_rest(dst_bone).basis.get_rotation_quaternion().normalized()
            var src_rest_q := src_local.basis.get_rotation_quaternion().normalized()
            var dst_rest_q := dst_local.basis.get_rotation_quaternion().normalized()
            var claw_scale_ratio := clampf(_rest_height(destination) / _rest_height(claw_skeleton), 0.3, 3.0)
            for key_idx in claw_native.track_get_key_count(track_idx):
                var value: Variant = claw_native.track_get_key_value(track_idx, key_idx)
                if kind == Animation.TYPE_ROTATION_3D:
                    var relative: Quaternion = src_rest_q.inverse() * (value as Quaternion)
                    var world_delta: Quaternion = src_global_q * relative * src_global_q.inverse()
                    var local_delta: Quaternion = dst_global_q.inverse() * world_delta * dst_global_q
                    value = (dst_rest_q * local_delta).normalized()
                else:
                    var offset: Vector3 = ((value as Vector3) - src_local.origin) * claw_scale_ratio
                    if in_place:
                        offset.x = 0.0
                        offset.z = 0.0
                    value = dst_local.origin + offset
                claw_clip.track_insert_key(converted_idx, claw_native.track_get_key_time(track_idx, key_idx),
                    value, claw_native.track_get_key_transition(track_idx, key_idx))
        library.add_animation(CLAW_NAME, claw_clip)
    claw_instance.free()
    if library.has_animation(CLAW_NAME):
        return library.get_animation(CLAW_NAME)
    return null
