extends RefCounted
## First quadruped reference: the original, unmodified 46-bone wolf.
## Incompatible skeletons are rejected instead of silently dropping bones.
## Native clips remain byte-for-byte sourced from the original GLB.
const REFERENCE := preload("res://assets/wolf_quadruped.glb")
const ORIGINAL_CLIPS := ["dog_idle1", "dog_run", "dog_dog_over", "leopard_skill_feipu"]
const CANONICAL_CLIPS := {
    "dog_idle1": "Idle_Standing_01_Standard",
    "dog_run": "Movement_Run_01_Standard",
    "dog_dog_over": "Action_RollOver_01_Standard",
    "leopard_skill_feipu": "Attack_Pounce_01_Standard"
}
const LOOPING := ["dog_idle1", "dog_run"]
const EXPECTED_BONES := 46

static func find_skeleton(node: Node) -> Skeleton3D:
    if node is Skeleton3D:
        return node
    for child in node.get_children():
        var value := find_skeleton(child)
        if value != null:
            return value
    return null

static func find_animator(node: Node) -> AnimationPlayer:
    if node is AnimationPlayer:
        return node
    for child in node.get_children():
        var value := find_animator(child)
        if value != null:
            return value
    return null

static func validate(model: Node3D) -> Dictionary:
    var original := REFERENCE.instantiate()
    var source_skeleton := find_skeleton(original)
    var dest := find_skeleton(model)
    var errors: Array[String] = []
    if source_skeleton == null or dest == null:
        errors.append("missing Skeleton3D")
    else:
        if source_skeleton.get_bone_count() != EXPECTED_BONES:
            errors.append("quadruped reference bone count changed")
        if dest.get_bone_count() != EXPECTED_BONES:
            errors.append("expected 46 bones, found %d" % dest.get_bone_count())
        for index in source_skeleton.get_bone_count():
            var name := source_skeleton.get_bone_name(index)
            var mapped := dest.find_bone(name)
            if mapped < 0:
                errors.append("missing bone " + name)
                continue
            var source_parent := source_skeleton.get_bone_parent(index)
            var target_parent := dest.get_bone_parent(mapped)
            var expected_parent := "" if source_parent < 0 else source_skeleton.get_bone_name(source_parent)
            var actual_parent := "" if target_parent < 0 else dest.get_bone_name(target_parent)
            if actual_parent != expected_parent:
                errors.append("hierarchy mismatch " + name)
            # Native animation copying is safe only for the *same rest rig*.
            # Distinct quadrupeds must be explicitly retargeted before use.
            var rest_a := source_skeleton.get_bone_rest(index)
            var rest_b := dest.get_bone_rest(mapped)
            if rest_a.origin.distance_to(rest_b.origin) > 0.0005 or not rest_a.basis.is_equal_approx(rest_b.basis):
                errors.append("rest pose mismatch " + name)
    original.free()
    return {"valid": errors.is_empty(), "errors": errors,
        "mapped_bones": EXPECTED_BONES if errors.is_empty() else 0}

static func attach(model: Node3D) -> AnimationPlayer:
    var existing := model.get_node_or_null("SharedQuadrupedAnimations") as AnimationPlayer
    if existing != null:
        return existing
    var check := validate(model)
    if not check.valid:
        push_warning("Quadruped rig rejected: " + str(check.errors))
        return null
    var native := find_animator(model)
    if native == null:
        push_warning("Quadruped has no native animation player; retargeting requires a separate verified import")
        return null
    for clip in ORIGINAL_CLIPS:
        if not native.has_animation(clip):
            push_warning("Quadruped missing original clip: " + clip)
            return null
    var result := AnimationPlayer.new()
    result.name = "SharedQuadrupedAnimations"
    model.add_child(result)
    result.root_node = result.get_path_to(native.get_node(native.root_node))
    for library_name in native.get_animation_library_list():
        result.add_animation_library(library_name, native.get_animation_library(library_name))
    var aliases := result.get_animation_library("").duplicate() as AnimationLibrary
    for clip in ORIGINAL_CLIPS:
        var alternate := native.get_animation(clip).duplicate(true) as Animation
        alternate.loop_mode = Animation.LOOP_LINEAR if clip in LOOPING else Animation.LOOP_NONE
        aliases.add_animation(CANONICAL_CLIPS[clip], alternate)
    result.remove_animation_library("")
    result.add_animation_library("", aliases)
    result.set_meta("mapped_bones", EXPECTED_BONES)
    result.set_meta("skeleton_family", "Quadruped")
    result.set_meta("native_reference", true)
    native.stop()
    return result
