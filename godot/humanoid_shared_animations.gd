extends RefCounted
## Retargets the 20 original Mixamo clips to a compatible humanoid Skeleton3D.
## Source GLB contains animation data and a tiny marker mesh, not character art.
const SOURCE := preload("res://assets/humanoid_mixamo_animation_library.glb")
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
    var candidates: Array = ALIASES.get(source_name, [source_name])
    for candidate in candidates:
        for candidate_name in [str(candidate), "mixamorig:" + str(candidate), "mixamorig_" + str(candidate)]:
            var index := destination.find_bone(candidate_name)
            if index >= 0:
                return index
    return -1

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
    var mappings := {}
    for bone_idx in origin.get_bone_count():
        var source_name := origin.get_bone_name(bone_idx)
        var dest_idx := _target_bone(_source_bone_name(source_name), destination)
        if dest_idx >= 0:
            mappings[source_name] = [bone_idx, dest_idx]
    if mappings.size() < 8:
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
                library.remove_animation(name)
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
