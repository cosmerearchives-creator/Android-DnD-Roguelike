extends SceneTree
## Graphical review harness; run without --headless on a Windows desktop.
func _initialize() -> void:
	call_deferred("_run")

func _run() -> void:
	root.size = Vector2i(1600,900)
	var screen: Control = load("res://inventory_screen.tscn").instantiate() as Control
	root.add_child(screen)
	for i in range(7): await process_frame
	for tab in ["Equipment","Items","Materials","Quest"]:
		screen.call("_select_tab",tab)
		for i in range(4): await process_frame
		var image := root.get_texture().get_image()
		var target: String = "G:/ChatGPT Projects/Forests of Hell/Inventory Review/"+tab.to_lower()+".png"
		var result := image.save_png(target)
		print("INVENTORY_PREVIEW",tab,"saved",result,"pixels",image.get_width(),image.get_height())
	root.remove_child(screen)
	screen.queue_free()
	await process_frame
	quit(0)
