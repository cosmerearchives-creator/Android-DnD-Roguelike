extends SceneTree
## Dedicated creator replaced the legacy menu-embedded creator in v0.3.203.
## Keep the old preview/hair/zoom regression coverage against the live workshop.
func _initialize() -> void:
	call_deferred("_run")
func _run() -> void:
	root.size=Vector2i(1600,900)
	var workshop:Control=(load("res://creator_workshop.tscn") as PackedScene).instantiate() as Control
	root.add_child(workshop)
	await process_frame
	await process_frame
	var display:SubViewportContainer=workshop._preview
	if display==null or workshop.model==null or workshop.camera==null:
		printerr("CREATOR_PREVIEW_OR_MODEL_MISSING")
		quit(1)
		return
	var canvas:Control=workshop._ui_root
	var center:Rect2=display.get_global_rect()
	var left:Rect2=workshop.category_controls["Body"].get_global_rect()
	var right:Rect2=Rect2(canvas.global_position+Vector2(926,106)*canvas.scale,Vector2(704,671)*canvas.scale)
	if center.intersects(left) or center.intersects(right):
		printerr("CREATOR_COLUMNS_OVERLAP")
		quit(1)
		return
	workshop.appearance["hair"]=1
	workshop.appearance["beard"]=0
	workshop.appearance["hair_scale"]=1.0
	workshop._update_appearance()
	await process_frame
	var attachment:Node=workshop.model.find_child("ModularAppearance",true,false)
	if attachment==null or attachment.get_child_count()!=1:
		printerr("CREATOR_HAIR_PREVIEW_MISSING")
		quit(1)
		return
	var old_scale:float=attachment.get_child(0).scale.x
	workshop.appearance["hair_scale"]=2.0
	workshop._update_appearance()
	await process_frame
	attachment=workshop.model.find_child("ModularAppearance",true,false)
	if attachment==null or attachment.get_child_count()!=1:
		printerr("CREATOR_HAIR_SCALE_PREVIEW_MISSING")
		quit(1)
		return
	if not is_equal_approx(attachment.get_child(0).scale.x,old_scale*2.0):
		printerr("CREATOR_HAIR_SCALE_FAILED")
		quit(1)
		return
	var old_zoom:float=workshop.camera.size
	workshop._zoom(0.5)
	if workshop.camera.size>=old_zoom:
		printerr("CREATOR_ZOOM_FAILED")
		quit(1)
		return
	print("CREATOR_PREVIEW_OK",display.size," model=",workshop.model.name," hair=",attachment.get_child_count())
	root.remove_child(workshop)
	workshop.queue_free()
	await process_frame
	print("CREATOR_CLEANUP_OK")
	quit(0)
