extends SceneTree
const PLATFORM = preload("res://platform_layout.gd")

func _initialize() -> void:
	call_deferred("_run")

func _fail(reason: String) -> void:
	printerr("PC_PLATFORM_SMOKE_FAILED: " + reason)
	quit(1)

func _key(code: Key) -> InputEventKey:
	var key := InputEventKey.new()
	key.keycode = code
	key.pressed = true
	return key

func _run() -> void:
	if not PLATFORM.is_pc():
		_fail("Desktop test mode not active")
		return
	root.size = Vector2i(1600,900)
	var world: Node3D = load("res://world.tscn").instantiate()
	root.add_child(world)
	await process_frame
	await process_frame
	if not world.pc_mode or not world.hud.pc_mode:
		_fail("Desktop mode not recognized by gameplay and HUD")
		return
	if world.camera.projection != Camera3D.PROJECTION_PERSPECTIVE:
		_fail("PC must use a perspective camera")
		return
	world._physics_process(0.016)
	if world.hud.buttons.has("Jump") or world.hud.buttons.has("Attack") or world.hud.buttons.has("Inventory"):
		_fail("Touch controls remain visible in desktop HUD")
		return
	for name in ["Lock","A","Menu"]:
		if not world.hud.buttons.has(name):
			_fail("PC top icon absent: " + name)
			return
	print("PC_HUD_AND_CAMERA_OK")
	# Respect persisted camera preferences: the test must start its V toggle
	# from third person even when the user previously closed in first person.
	var originally_first_person: bool = world.first_person
	if world.first_person: world.set_first_person(false)
	world._unhandled_key(_key(KEY_C))
	if not world.sneaking:
		_fail("C did not toggle crouch")
		return
	world._unhandled_key(_key(KEY_C))
	if world.sneaking:
		_fail("C did not turn off crouch")
		return
	world._unhandled_key(_key(KEY_V))
	if not world.first_person or world.first_person_meshes.is_empty():
		_fail("V did not enable first-person body rendering")
		return
	var entry: Dictionary = world.first_person_meshes[0]
	var original := entry.original as Mesh
	var filtered := entry.filtered as Mesh
	var original_count: int = original.surface_get_arrays(0)[Mesh.ARRAY_INDEX].size()
	var filtered_count: int = filtered.surface_get_arrays(0)[Mesh.ARRAY_INDEX].size()
	if filtered_count < 1000 or filtered_count >= original_count:
		_fail("First-person body filtering did not retain lower body and remove head")
		return
	world.camera_pitch = deg_to_rad(65.0)
	world._physics_process(0.016)
	if world.camera.position.distance_to(world.player.position + Vector3.UP*1.57) > 0.38:
		_fail("First-person camera is not at eye height")
		return
	world._unhandled_key(_key(KEY_V))
	if world.first_person or entry.instance.mesh != original:
		_fail("Third-person toggle failed to restore original mesh")
		return
	print("PC_FIRST_PERSON_VISIBLE_BODY_OK")
	if not world.mouse_looking:
		_fail("PC camera should capture mouse immediately, without right-click")
		return
	var motion := InputEventMouseMotion.new()
	motion.relative = Vector2(30,25)
	var old_yaw: float = world.camera_yaw
	var old_pitch: float = world.camera_pitch
	world._unhandled_input(motion)
	if is_equal_approx(world.camera_yaw,old_yaw) or is_equal_approx(world.camera_pitch,old_pitch):
		_fail("Free mouse motion did not rotate yaw and pitch")
		return
	if world.camera_pitch <= old_pitch:
		_fail("Mouse moving downward must move camera downward, not inverted")
		return
	var right := InputEventMouseButton.new()
	right.button_index = MOUSE_BUTTON_RIGHT
	right.pressed = true
	world._unhandled_input(right)
	right.pressed = false
	world._unhandled_input(right)
	if not world.mouse_looking:
		_fail("Right-click should no longer toggle mouse-look")
		return
	print("PC_FREE_MOUSE_LOOK_OK")
	world._unhandled_key(_key(KEY_Q))
	if world.mouse_looking:
		_fail("Inventory must release the mouse for UI interaction")
		return
	if not is_instance_valid(world.inventory_panel) or world.inventory_panel.active_tab != "Quest":
		_fail("Q did not open inventory directly to Quest")
		return
	var inventory: Control = world.inventory_panel
	if not inventory.desktop_layout:
		_fail("Inventory did not enter landscape arrangement")
		return
	var left := inventory._list_panel.find_child("FilterSidebar",true,false) as Control

	if inventory._list_panel.get_global_rect().end.y > inventory.size.y + 1.0 or left == null:
		_fail("Desktop quest screen does not fit in landscape")
		return
	if inventory.list_scroll.get_global_rect().end.x >= inventory.detail_panel.get_global_rect().position.x:
		_fail("Landscape quest list overlaps detail column")
		return
	world._unhandled_key(_key(KEY_I))
	if is_instance_valid(world.inventory_panel):
		_fail("I failed to close inventory")
		return
	if not world.mouse_looking:
		_fail("Closing inventory should recapture mouse")
		return
	await process_frame
	world._unhandled_key(_key(KEY_I))
	if not is_instance_valid(world.inventory_panel) or world.inventory_panel.active_tab != "Equipment":
		_fail("I failed to open equipment inventory")
		return
	world._unhandled_key(_key(KEY_Q))
	if world.inventory_panel.active_tab != "Quest":
		_fail("Q did not switch existing inventory to Quest")
		return
	world._unhandled_key(_key(KEY_ESCAPE))
	if is_instance_valid(world.inventory_panel):
		_fail("Esc did not close inventory")
		return
	if not world.mouse_looking:
		_fail("Esc should recapture mouse after closing inventory")
		return
	world._unhandled_key(_key(KEY_ESCAPE))
	if not world.menu_panel.visible or world.mouse_looking:
		_fail("Esc menu must show mouse pointer")
		return
	world._unhandled_input(motion)
	if not is_equal_approx(world.camera_yaw,old_yaw-30*world.MOUSE_SENSITIVITY):
		_fail("Camera should not rotate while menu is open")
		return
	world._unhandled_key(_key(KEY_ESCAPE))
	if world.menu_panel.visible or not world.mouse_looking:
		_fail("Closing menu should restore free mouse-look")
		return
	await process_frame
	print("PC_LANDSCAPE_INVENTORY_AND_KEYS_OK")
	world.attack = 0.0
	var click := InputEventMouseButton.new()
	click.button_index = MOUSE_BUTTON_LEFT
	click.pressed = true
	world._unhandled_input(click)
	if world.attack <= 0.0:
		_fail("Left click did not perform attack")
		return
	world.set_first_person(originally_first_person)
	world.queue_free()
	print("PC_PLATFORM_SMOKE_OK")
	quit(0)
