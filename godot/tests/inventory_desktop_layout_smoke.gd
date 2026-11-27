extends SceneTree
## PC-only regression gate for approved 15-slot landscape inventory skin.
func _initialize() -> void:
	call_deferred("_run")

func _fail(message: String) -> void:
	printerr("INVENTORY_DESKTOP_LAYOUT_FAILED: "+message)
	quit(1)

func _run() -> void:
	root.size=Vector2i(1600,900)
	var screen: Control=(load("res://inventory_screen.tscn") as PackedScene).instantiate()
	root.add_child(screen)
	for i in 5: await process_frame
	if not screen.desktop_layout:
		_fail("PC landscape mode not active")
		return
	if screen.slot_cells.size()!=15 or screen.tab_controls.size()!=4:
		_fail("Lost approved equipment slots or tabs")
		return
	var canvas := screen.find_child("InventoryCanvas",true,false) as Control
	var title := screen.find_child("InventoryTitlePanel",true,false) as Control
	var stats := screen.find_child("CharacterStatsPanel",true,false) as Control
	var grid := screen.find_child("EquipmentInventoryGrid",true,false) as Control
	var details := screen.find_child("EquippedItemDetails",true,false) as Control
	var equipment := screen.find_child("CharacterEquipmentPanel",true,false) as Control
	var backdrop := screen.find_child("InventoryBackdrop",true,false) as ColorRect
	if canvas==null or title==null or stats==null or grid==null or details==null or equipment==null or backdrop==null:
		_fail("Required visual regions are missing")
		return
	if backdrop.color.a<.80:
		_fail("Underlying world will make inventory text hard to read")
		return
	if stats.position.x<=equipment.position.x+equipment.size.x or stats.position.y+stats.size.y>grid.position.y or grid.position.y+grid.size.y>details.position.y:
		_fail("Landscape equipment columns overlap")
		return
	for slot in screen.slot_cells:
		var button: Button=screen.slot_cells[slot]
		if not Rect2(Vector2.ZERO,equipment.size).encloses(Rect2(button.position,button.size)):
			_fail("An equipment slot is outside the character panel: "+str(slot))
			return
	for tab in ["Equipment","Items","Materials","Quest"]:
		screen._select_tab(tab)
		await process_frame
		if screen.active_tab!=tab:
			_fail("Tab navigation failed: "+tab)
			return
		if tab!="Equipment":
			var sidebar := screen.find_child("FilterSidebar",true,false) as Control
			if sidebar==null or screen.filter_buttons.size()!=screen._filters().size():
				_fail("Category sidebar missing for "+tab)
				return
			if screen._list_panel.position.y+screen._list_panel.size.y>901:
				_fail("Tabbed content exceeds viewport "+str(screen._list_panel.get_global_rect())+" root "+str(root.size))
				return
		else:
			if not screen.desktop_equipment.visible:
				_fail("Equipment panel disappeared")
				return
	screen._select_tab("Equipment")
	screen.selection = "chain_mail"
	screen.current_slot = "Cloak"
	screen._refresh()
	if screen._compatible_selected_slot() != "Body":
		_fail("Chain Mail shows stale Cloak slot rather than Body")
		return
	if screen.find_child("SelectedItemFrame",true,false) == null:
		_fail("Large framed selected item missing")
		return
	screen.selection = "longsword"
	screen.current_slot = "Body"
	if screen._compatible_selected_slot() != "Main Hand":
		_fail("Longsword showed Body slot instead of Main Hand")
		return
	if not ResourceLoader.exists("res://ui_skin/panel.png") or not ResourceLoader.exists("res://ui_skin/slot.png") or not ResourceLoader.exists("res://ui_skin/tab_active.png") or not ResourceLoader.exists("res://ui_skin/portrait_backdrop.png"):
		_fail("The custom artwork assets are missing")
		return
	print("INVENTORY_DESKTOP_LAYOUT_OK 1600x900 slots=15 tabs=4 art=true")
	quit(0)
