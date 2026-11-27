extends SceneTree
const SCENE=preload("res://inventory_screen.tscn")
func _initialize() -> void:
	call_deferred("_run")
func _run() -> void:
	for target_height in [1280,1536]:
		root.size=Vector2i(720,target_height)
		var screen=SCENE.instantiate()
		root.add_child(screen)
		await process_frame
		await process_frame
		if screen.tab_controls.size()!=4 or screen.slot_cells.size()!=17:
			printerr("Inventory controls missing",screen.tab_controls.size(),screen.slot_cells.size())
			quit(1);return
		if screen.data.catalog.size()<100 or screen.data.icon("ration")==null:
			printerr("Inventory source catalog or art missing")
			quit(1);return
		var last_slot: Rect2=screen.slot_cells["Accessory"].get_global_rect()
		var list_scroll: Rect2=screen.item_grid.get_parent().get_global_rect()
		var carry: Rect2=screen.find_child("CarryValue",true,false).get_global_rect()
		if last_slot.end.y > list_scroll.position.y or carry.end.y > list_scroll.position.y:
			printerr("Inventory layout overflow",last_slot,carry,list_scroll)
			quit(1);return
		if list_scroll.end.y>target_height+2:
			printerr("Inventory list extends outside viewport",list_scroll,target_height)
			quit(1);return
		for tab in ["Equipment","Items","Materials","Quest"]:
			screen._select_tab(tab)
			if screen.active_tab != tab:
				printerr("Tab selection failed",tab)
				quit(1);return
		if screen.data.equipped_id("Main Hand")=="": 
			printerr("Starter equipment not populated")
			quit(1);return
		print("INVENTORY_SMOKE_OK",target_height,"tabs",screen.tab_controls.size(),"slots",screen.slot_cells.size(),"catalog",screen.data.catalog.size(),"atlas",screen.data.icon("ration")!=null)
		root.remove_child(screen)
		screen.queue_free()
		await process_frame
	quit(0)
