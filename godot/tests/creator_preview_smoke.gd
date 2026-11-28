extends SceneTree
func _initialize() -> void:
    call_deferred("_run")
func _run() -> void:
    root.size=Vector2i(1600,900)
    var menu=(load("res://front_menu.tscn") as PackedScene).instantiate()
    root.add_child(menu)
    await process_frame
    menu.creator()
    await process_frame
    await process_frame
    var display=menu.get_node_or_null("MenuControls/CreatorColumns/CreatorModelPreview")
    if display==null:
        printerr("CREATOR_PREVIEW_MISSING")
        quit(1)
        return
    if menu.preview_model==null:
        printerr("CREATOR_MODEL_MISSING")
        quit(1)
        return
    print("CREATOR_LAYOUT controls=",menu.menu_scroll.get_global_rect()," preview=",display.get_global_rect()," window=",root.size)
    if menu.menu_scroll.get_global_rect().intersects(display.get_global_rect()):
        printerr("CREATOR_COLUMNS_OVERLAP")
        quit(1)
        return
    menu.chosen=0
    menu.edited_appearance["hair"]=1
    menu._refresh_preview()
    await process_frame
    var attachment=menu.preview_model.find_child("ModularAppearance",true,false)
    if attachment==null or attachment.get_child_count()!=1:
        printerr("CREATOR_HAIR_PREVIEW_MISSING")
        quit(1)
        return
    var old_scale=attachment.get_child(0).scale.x
    menu.edited_appearance["hair_scale"]=2.0
    menu._update_appearance()
    menu.face_zoom=true
    menu._frame_preview()
    attachment=menu.preview_model.find_child("ModularAppearance",true,false)
    if not String(attachment.bone_name).ends_with("Head") or not is_equal_approx(attachment.get_child(0).scale.x,old_scale*2.0) or menu.preview_camera.size>=1.0:
        printerr("GOBLIN_FACE_CONTROLS_FAILED")
        quit(1)
        return
    print("CREATOR_PREVIEW_OK ",display.size," model=",menu.preview_model.name," accessories=",attachment.get_child_count())
    menu.main_menu()
    await process_frame
    if menu.preview_view != null or menu.menu_scroll.get_parent() != menu.get_node("MenuControls"):
        printerr("CREATOR_CLEANUP_FAILED")
        quit(1)
        return
    print("CREATOR_CLEANUP_OK")
    quit(0)
