from pathlib import Path
p=Path(__file__).resolve().parents[1]
f=p/'godot/humanoid_shared_animations.gd'
s=f.read_text(encoding='utf-8')
needle='static func _rest_height(skeleton: Skeleton3D) -> float:'
assert needle in s
insert='''# Select the actual skinned hand root when Mixamo's nominal Hand is a leaf.
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
        var parent := stack.pop_back()
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
        var label := ["Thumb", "Index", "Middle", "Ring", "Pinky"][label_index]
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

'''
s=s.replace(needle, insert+needle)
a=s.index('    var mappings := {}\n    for bone_idx in origin.get_bone_count():')
b=s.index('    var player := AnimationPlayer.new()',a)
s=s[:a]+'''    var mappings := _build_mapping(origin, destination)
    var validation := validate_humanoid(origin, destination, mappings)
    if not validation.valid:
        push_error("Humanoid import rejected: " + str(validation.missing))
        source_instance.free()
        return null
'''+s[b:]
# IMPORTANT direct native human unchanged; others now strict.
s=s.replace('var dst_bone := _target_bone(canonical, destination)','var dst_bone := _build_mapping(claw_skeleton, destination).get(source_name, [-1, -1])[1]')
# Be careful default mapping 'Hips' correct and no lookups to custom hand default required per track.
f.write_text(s,encoding='utf-8')
print('PATCHED')
