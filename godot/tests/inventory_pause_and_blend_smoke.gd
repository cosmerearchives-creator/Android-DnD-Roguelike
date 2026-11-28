extends SceneTree
const DATA = preload("res://inventory_data.gd")
const TERRAIN = preload("res://terrain_layer.gd")

func _initialize() -> void:
	call_deferred("_run")

func _fail(message: String) -> void:
	printerr("INVENTORY_PAUSE_BLEND_FAILED: "+message)
	paused = false
	quit(1)

func _run() -> void:
	if DATA.SLOT_NAMES.size()!=15 or DATA.SLOT_NAMES.has("Ears") or DATA.SLOT_NAMES.has("Legs") or DATA.SLOT_NAMES.has("Ranged") or not DATA.SLOT_NAMES.has("Secondary"):
		_fail("Incorrect approved equipment slots")
		return
	var ground := TERRAIN.new()
	root.add_child(ground)
	for i in range(9): await process_frame
	if ground.terrain_counts.size()!=6:
		_fail("Six terrain kinds not loaded")
		return
	if ground.material.get_shader_parameter("biome_map")==null:
		_fail("Missing terrain distribution texture")
		return
	ground.set_blend_inches(13.0)
	var settings := ConfigFile.new()
	if settings.load("user://terrain_blending.cfg")!=OK or absf(float(settings.get_value("terrain","edge_blend_inches",0))-13.0)>.01:
		_fail("Slider value not saved")
		return
	var world: Node3D = load("res://world.tscn").instantiate()
	root.add_child(world)
	for i in 5: await process_frame
	var slider := world.find_child("TerrainBlendSlider",true,false) as HSlider
	if slider==null or slider.max_value!=30.0 or slider.step!=1.0 or slider.value!=13.0:
		_fail("Terrain blending slider not connected/saved")
		return
	world.toggle_inventory()
	if not paused or not is_instance_valid(world.inventory_panel):
		_fail("Inventory did not pause tree")
		return
	if world.hud.visible:
		_fail("Gameplay HUD still visible behind inventory")
		return
	if world.inventory_panel.process_mode != Node.PROCESS_MODE_ALWAYS:
		_fail("Inventory UI doesn't process while paused")
		return
	var initial: Vector3 = world.player.global_position
	await physics_frame
	await physics_frame
	if world.player.global_position.distance_to(initial)>.001:
		_fail("Player moved during paused inventory")
		return
	world.inventory_panel._select_tab("Materials")
	if world.inventory_panel.active_tab!="Materials":
		_fail("Menu selection does not work while paused")
		return
	world._inventory_closed()
	if paused or is_instance_valid(world.inventory_panel) or not world.hud.visible:
		_fail("Inventory didn't fully restore world")
		return
	ground.set_blend_inches(6.0)
	print("INVENTORY_PAUSE_BLEND_SMOKE_OK slots=15 kinds=6 saved=13 paused=true resumed=true")
	quit(0)
