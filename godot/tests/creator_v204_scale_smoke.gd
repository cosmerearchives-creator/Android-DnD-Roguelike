extends SceneTree
func _initialize(): call_deferred("_test")
func _test():
	root.size=Vector2i(1673,940)
	var scene=load("res://creator_workshop.tscn")
	var ui=scene.instantiate()
	root.add_child(ui)
	await process_frame
	await process_frame
	for x in ["Body","Hair","Beard"]:
		ui._show_category(x)
		await process_frame
		print("CREATOR_PANEL",x,"controls",ui.controls.get_child_count())
	ui._show_category("Body")
	var tex=ui._ui_root.get_children().filter(func(n):return n is NinePatchRect)
	print("NINEPATCH",tex.size(),"root",ui._ui_root.size,"scale",ui._ui_root.scale)
	assert(tex.size()==7)
	assert(ui.page_controls.size()==7 and ui.category_controls.size()==8)
	assert(ui._preview.size.x==458)
	var original: Vector2i=Vector2i(1673,940)
	ui.size=Vector2(1600,900)
	ui._fit()
	assert(absf(ui._ui_root.scale.x-900.0/940.0)<0.01)
	assert(ui._ui_root.scale.x==ui._ui_root.scale.y)
	ui.size=Vector2(original)
	ui._fit()
	assert(ui._ui_root.position.y >= 0.0)
	print("CREATOR_V204_SMOKE_OK")
	quit(0)
