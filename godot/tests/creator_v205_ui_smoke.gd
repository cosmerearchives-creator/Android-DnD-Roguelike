extends SceneTree
func _initialize() -> void: call_deferred("_run")
func _run() -> void:
	root.size=Vector2i(1673,940)
	var ui:Control=(load("res://creator_workshop.tscn") as PackedScene).instantiate() as Control
	root.add_child(ui)
	await process_frame
	await process_frame
	assert(ui.category_controls.size()==8 and ui.page_controls.size()==7)
	assert(ui.model!=null and ui.camera!=null)
	var source_preview:Node=ui._ui_root
	var preview_stone_count:=0
	for child in source_preview.get_children():
		if child is TextureRect and child.texture==ui.stone_tex:
			preview_stone_count+=1
			assert(child.position.y==699.0)
	assert(preview_stone_count==1)
	var sliders:Array[HSlider]=[]
	var rails:=0
	for child in ui.controls.get_children():
		if child is HSlider:sliders.append(child)
		elif child is ColorRect:rails+=1
	assert(sliders.size()==3)
	assert(rails==9)
	var before:float=ui.camera.size
	ui._zoom(0.9)
	assert(ui.camera.size < before)
	var initial_rotation:float=ui.model.rotation.y
	ui._rotate(PI/8.0)
	assert(ui.model.rotation.y > initial_rotation)
	sliders[0].value=72.0
	assert(absf(float(ui.appearance["height_scale"])-72.0/68.0)<0.03)
	ui._show_category("Hair")
	ui._show_category("Beard")
	ui._show_category("Body")
	assert(ui.controls.get_child_count()>=30)
	print("CREATOR_V205_VISUAL_AND_INTERACTION_SMOKE_OK","sliders",sliders.size(),"gold_rails",rails,"pedestal",preview_stone_count)
	quit(0)
