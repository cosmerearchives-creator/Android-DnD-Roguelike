extends SceneTree
func _initialize() -> void: call_deferred("_test")
func _test() -> void:
	root.size=Vector2i(1673,940)
	var scene=load("res://creator_workshop.tscn") as PackedScene
	if scene==null:quit(1);return
	var ui=scene.instantiate() as Control
	root.add_child(ui)
	await process_frame
	print("CREATOR_UI",ui.size,ui.category_controls.size(),ui.page_controls.size(),ui.active_category)
	ui._show_category("Hair")
	print("CREATOR_HAIR",ui.active_category,ui.controls.get_child_count())
	ui._show_category("Body")
	print("CREATOR_BODY",ui.active_category,ui.controls.get_child_count(),ui.selected_height_inches)
	ui._show_category("Beard")
	print("CREATOR_BEARD",ui.active_category,ui.controls.get_child_count())
	ui._show_category("Body")
	assert(ui.category_controls.size()==8)
	assert(ui.page_controls.size()==7)
	assert(ui.model!=null)
	assert(ui.controls.get_child_count()>10)
	print("CREATOR_WORKSHOP_SMOKE_OK")
	quit()
