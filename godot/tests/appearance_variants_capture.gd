extends SceneTree
func _initialize():call_deferred("_run")
func _run():
	root.size=Vector2i(1673,940)
	var ui=(load("res://creator_workshop.tscn") as PackedScene).instantiate()
	root.add_child(ui)
	await process_frame
	ui._show_category("Hair")
	for style in range(1,4):
		ui.appearance["hair"]=style
		ui.appearance["beard"]=0
		ui.appearance["hair_scale"]=1.0
		ui.appearance["hair_offset"]=Vector3.ZERO
		ui._update_appearance()
		for k in range(3):await process_frame
		await RenderingServer.frame_post_draw
		var shot:Image=root.get_texture().get_image()
		var name:String=["","wavy","short","braid"][style]
		print("VARIANT_CAPTURE ",name," ",shot.save_png("G:/ChatGPT Projects/Forests of Hell/Creator Review/hair_v206_"+name+".png"))
	ui.appearance["hair"]=0
	ui.appearance["beard"]=1
	ui.appearance["beard_offset"]=Vector3.ZERO
	ui._update_appearance()
	ui._show_category("Beard")
	for k in range(3):await process_frame
	await RenderingServer.frame_post_draw
	var shot:Image=root.get_texture().get_image()
	print("VARIANT_CAPTURE beard ",shot.save_png("G:/ChatGPT Projects/Forests of Hell/Creator Review/beard_v206.png"))
	quit()
