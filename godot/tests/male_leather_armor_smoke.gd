extends SceneTree
# Read-only character/armor import, binding and script parsing test.
const SHARED = preload("res://humanoid_shared_animations.gd")
func _initialize() -> void:
    call_deferred("_run")
func _run() -> void:
    var failed := false
    var male_scene := load("res://assets/male_human.glb") as PackedScene
    var armor_scene := load("res://assets/male_human_leather_armored.glb") as PackedScene
    if male_scene == null or armor_scene == null:
        push_error("Missing source male or armored male GLB")
        quit(1)
        return
    var male := male_scene.instantiate() as Node3D
    var armored := armor_scene.instantiate() as Node3D
    root.add_child(male)
    root.add_child(armored)
    var male_skeletons := male.find_children("*", "Skeleton3D", true, false)
    var armor_skeletons := armored.find_children("*", "Skeleton3D", true, false)
    var armored_meshes := armored.find_children("*", "MeshInstance3D", true, false)
    if male_skeletons.size() != 1 or armor_skeletons.size() != 1 or armored_meshes.size() != 2:
        push_error("Wrong skeleton or skinned mesh counts")
        failed = true
    else:
        var a := male_skeletons[0] as Skeleton3D
        var b := armor_skeletons[0] as Skeleton3D
        if a.get_bone_count() != 84 or b.get_bone_count() != 84:
            push_error("Expected full 84-bone male skeleton")
            failed = true
        else:
            for i in range(84):
                if a.get_bone_name(i) != b.get_bone_name(i) or a.get_bone_parent(i) != b.get_bone_parent(i):
                    push_error("Male skeleton map changed at bone %d" % i)
                    failed = true
                    break
        for mesh in armored_meshes:
            var m := mesh as MeshInstance3D
            if m.mesh == null or m.skin == null:
                push_error("Missing skinned mesh or skin")
                failed = true
        print("MALE ARMOR bones=",b.get_bone_count()," skinned_meshes=",armored_meshes.size())
    var shared := SHARED.attach(armored)
    if shared == null:
        push_error("Armored male cannot load shared animations")
        failed = true
    else:
        print("MALE ARMOR shared_mapped_bones=",shared.get_meta("mapped_bones",0)," clips=",shared.get_animation_list().size())
        if shared.get_animation_list().size() == 0:
            failed = true
    for scene_path in ["res://inventory_screen.tscn","res://world.tscn","res://humanoid_animation_lab.tscn"]:
        if load(scene_path) == null:
            push_error("Cannot parse "+scene_path)
            failed = true
    var inv_scene := load("res://inventory_screen.tscn") as PackedScene
    if inv_scene != null:
        var panel = inv_scene.instantiate()
        root.add_child(panel)
        panel.set_model("male_human")
        panel.data.equipped["Body"]="leather_armor" # Memory only: never write the user save.
        panel._load_model()
        var equipped_model: Node3D = panel.equipped_body.get_child(panel.equipped_body.get_child_count()-1)
        var equipped_meshes := equipped_model.find_children("*","MeshInstance3D",true,false)
        if equipped_meshes.size()!=2:
            push_error("Leather armor did not appear in male inventory portrait")
            failed=true
        else: print("MALE INVENTORY leather_visual_meshes=",equipped_meshes.size())
        panel.free()
    print("MALE ARMOR SMOKE ", "FAILED" if failed else "PASSED")
    quit(1 if failed else 0)
