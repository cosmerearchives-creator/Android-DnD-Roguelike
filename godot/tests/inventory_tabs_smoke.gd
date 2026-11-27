extends SceneTree
func _initialize():call_deferred("_run")
func _run() -> void:
	for height in [1280,1536]:
		root.size=Vector2i(720,height)
		var screen=load("res://inventory_screen.tscn").instantiate()
		root.add_child(screen)
		await process_frame
		await process_frame
		for tab in ["Items","Materials","Quest"]:
			screen._select_tab(tab)
			if not screen._list_panel.visible:
				printerr("Tabbed list didn't show",tab)
				quit(2);return
			if screen.filter_buttons.size()!=screen._filters().size():
				printerr("Wrong number of sidebar filters",tab)
				quit(3);return
			var bounds=screen._list_panel.get_global_rect()
			if bounds.end.y>height+1:
				printerr("Tabbed list overflows phone",tab,bounds)
				quit(4);return
			var scroll=screen.list_scroll.get_global_rect()
			if scroll.end.y>=screen.detail_panel.get_global_rect().position.y:
				printerr("List overlaps details",tab)
				quit(5);return
			print("TAB_LAYOUT_OK",height,tab,"filters",screen.filter_buttons.size(),"rows",screen.list_container.get_child_count())
		screen._select_tab("Quest")
		screen.set_goblin_kills(4)
		if "4 / 10" not in screen.detail_panel.get_children().map(func(c):return c.text if c is Label else "").reduce(func(a,b):return a+" "+b,""):
			printerr("Live goblin quest progress not reflected in UI")
			quit(6);return
		screen._select_tab("Items")
		screen.section_filter="Tools"
		screen._refresh()
		if screen.content_header.text=="Items (0)":
			printerr("Tools filter unexpectedly empty")
			quit(7);return
		screen._select_tab("Equipment")
		if screen._list_panel.visible:
			printerr("Tabbed list didn't hide on equipment")
			quit(8);return
		root.remove_child(screen)
		screen.queue_free()
		await process_frame
	quit(0)
