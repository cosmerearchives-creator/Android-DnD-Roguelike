extends SceneTree
func _initialize():
	call_deferred("run")
func run():
	root.size=Vector2i(1600,900)
	for page in ["front_menu","inventory_screen"]:
		var ui=load("res://"+page+".tscn").instantiate()
		root.add_child(ui)
		if page=="inventory_screen":
			ui.selection="longsword"
			ui._refresh()
		for i in 12: await process_frame
		await RenderingServer.frame_post_draw
		var err=root.get_texture().get_image().save_png(ProjectSettings.globalize_path("res://../"+page+".png"))
		print("CAPTURE ",page," ",err)
		root.size=Vector2i(1918,1008)
		for i in 6: await process_frame
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png(ProjectSettings.globalize_path("res://../"+page+"_wide.png"))
		root.size=Vector2i(1600,900)
		root.remove_child(ui)
		ui.queue_free()
		await process_frame
	quit()

