extends SceneTree
const APPEARANCE=preload("res://character_appearance.gd")
func _initialize() -> void:
    call_deferred("_run")
func _run() -> void:
    var cfg=APPEARANCE.settings()
    assert(cfg.has("hair") and cfg.has("beard"))
    for model in ["human_adventurer","male_human"]:
        var packed=load("res://assets/"+model+".glb") as PackedScene
        if packed==null:quit(1);return
        var body=packed.instantiate() as Node3D
        root.add_child(body)
        var data={"hair":1,"beard":1,"hair_offset":Vector3(.02,0,0),"beard_offset":Vector3(0,-.02,0)}
        APPEARANCE.apply(body,data)
        var attachment=body.find_child("ModularAppearance",true,false)
        if attachment==null or not (attachment is BoneAttachment3D):
            printerr("Head attachment missing for ",model)
            quit(1);return
        if attachment.get_child_count()!=2:
            printerr("Hair/beard asset count not two: ",model,attachment.get_child_count())
            quit(1);return
        print("APPEARANCE_OK ",model," head_bone=",attachment.bone_idx," child_count=",attachment.get_child_count())
        APPEARANCE.apply(body,{"hair":0,"beard":0,"hair_offset":Vector3.ZERO,"beard_offset":Vector3.ZERO})
        body.queue_free()
        await process_frame
    var menu=load("res://front_menu.tscn") as PackedScene
    if menu==null:printerr("Creator/menu parse failed");quit(1);return
    var inventory=load("res://inventory_screen.tscn") as PackedScene
    if inventory==null:quit(1);return
    var scene=inventory.instantiate()
    root.add_child(scene)
    await process_frame
    var world=load("res://world.tscn")
    print("APPEARANCE_SMOKE ", "PASSED" if world!=null else "WORLD_LOAD_FAILED")
    quit(0 if world!=null else 1)
