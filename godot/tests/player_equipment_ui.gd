extends SceneTree
func _initialize(): call_deferred("run")
func fail(message):
	push_error(message)
	quit(1)
func run():
	var world=load("res://world.tscn").instantiate()
	root.add_child(world)
	await process_frame
	var visual=world.player.get_node("CharacterVisual")
	var garment=visual.find_child("WearableLeather",true,false)
	var coverage=visual.find_child("BootCoverage",true,false)
	if not garment or not coverage: return fail("Missing default character wearable")
	world.toggle_inventory("Equipment")
	var panel=world.inventory_panel
	panel._select_item("leather_armor")
	if panel.data.count_of("leather_armor")!=1: return fail("Missing or duplicate starter armor")
	for equipped in [true,false]:
		var button
		for b in panel.find_children("*","Button",true,false):
			if b.tooltip_text==("Equip" if equipped else "Unequip"):button=b
		if not button or button.disabled: return fail("Missing gear action")
		button.pressed.emit()
		await process_frame
		var portrait=panel.equipped_body.get_child(-1)
		var persisted=load("res://inventory_data.gd").new()
		if world.player.get_node("CharacterVisual")!=visual or garment.visible!=equipped or coverage.visible==equipped or portrait.find_child("WearableLeather",true,false).visible!=equipped or (persisted.equipped_id("Body")=="leather_armor")!=equipped:
			return fail("Visibility or saved slot mismatch")
	world.toggle_inventory()
	print("PLAYER_EQUIPMENT_UI_PASS")
	quit()
