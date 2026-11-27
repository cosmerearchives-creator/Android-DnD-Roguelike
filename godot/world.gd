extends Node3D
const FOOT = 0.3048
const GOBLINS = preload("res://goblin_variants.gd")
const SHARED = preload("res://humanoid_shared_animations.gd")
const COMBAT = preload("res://creature_combat_stats.gd")
const PLATFORM = preload("res://platform_layout.gd")
const INVENTORY = preload("res://inventory_data.gd")
var player: CharacterBody3D
var enemies
var camera: Camera3D
var stick := Vector2.ZERO
var touch_id := -1
var pc_mode := false
var first_person := false
var mouse_looking := false
var camera_yaw := 0.0
var camera_pitch := deg_to_rad(25.0)
const MOUSE_SENSITIVITY := 0.0035
const PC_CAMERA_DISTANCE := 5.0
var pc_camera_distance := PC_CAMERA_DISTANCE
var camera_ortho_size := 15.0
var camera_fov := 72.0
var first_person_meshes: Array[Dictionary] = []
const TEST_ATTACK_COOLDOWN_SECONDS := 1.0
const TEST_JUMP_COOLDOWN_SECONDS := 1.0
const JUMP_VELOCITY := 4.8
const SNEAK_SPEED_FACTOR := 0.5
const RUN_STICK_THRESHOLD := 0.92
const WALK_SPEED := 30.0 * FOOT / 6.0
var attack := 0.0
var jump_cooldown := 0.0
var jump_animation_left := 0.0
var running := false
var sneaking := false
const AUTO_LOCK_RANGE := 12.0
const AUTO_ATTACK_RANGE := 5.0 * FOOT
var auto_attack_enabled := false
var lock_enabled := false
var locked_target: CharacterBody3D
var target_marker: Label3D
var player_hp := int(COMBAT.TEST_PLAYER.hp)
var combat_notice := ""
var combat_notice_left := 0.0
var yaw := 0.0
var camera_angle := 60.0
var angle_button: Button
var model_index := 0
var punch_left := 0.0
const MODELS = ["human_adventurer", "male_human"]
const MODEL_NAMES = ["Human Adventurer", "Male Human"]
func _player_asset() -> String:
	if MODELS[model_index]=="male_human":
		var inv = INVENTORY.new()
		if inv.equipped_id("Body")=="leather_armor":
			return "res://assets/male_human_leather_armored.glb"
	return "res://assets/%s.glb" % MODELS[model_index]

func _refresh_player_visual() -> void:
	if not is_instance_valid(player): return
	var asset := _player_asset()
	if not ResourceLoader.exists(asset):
		push_error("Equipped character visual missing: "+asset)
		return
	var was_first_person := first_person
	if was_first_person: set_first_person(false)
	first_person_meshes.clear()
	var old_visual := player.get_node_or_null("CharacterVisual")
	if old_visual != null:
		player.remove_child(old_visual)
		old_visual.queue_free()
	var fresh: Node3D = (load(asset) as PackedScene).instantiate()
	fresh.name = "CharacterVisual"
	player.add_child(fresh)
	ground_visual(fresh)
	var animator = SHARED.attach(fresh)
	if animator != null and animator.has_animation("Idle"): animator.play("Idle")
	if was_first_person: set_first_person(true)

func _on_equipment_changed(slot: String, _item_id: String) -> void:
	if slot=="Body" and MODELS[model_index]=="male_human": _refresh_player_visual()
var status: Label
var hud: Control
var menu_panel: PanelContainer
var inventory_panel: Control
var terrain_layer: Node3D
var ground_cover_layer: Node3D
var terrain_blend_label: Label
var quest_goblin_kills := 0
var rng := RandomNumberGenerator.new()
# Building hundreds of imported scenes and collision shapes in _ready
# caused a long frame stall. Queue deterministic specifications first,
# then instantiate a few per frame with a bounded main-thread budget.
const SCENERY_FRAME_BUDGET_USEC := 2500
const SCENERY_MAX_PER_FRAME := 8
var scenery_jobs: Array[Dictionary] = []
var scenery_next := 0
var scenery_scene_cache: Dictionary = {}
var scenery_placed := 0
var scenery_finished := false
func mesh_at(parent: Node3D, mesh: Mesh, pos: Vector3, color: Color) -> MeshInstance3D:
	var m := MeshInstance3D.new()
	m.mesh = mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	m.material_override = mat
	parent.add_child(m)
	m.position = pos
	return m
func lowest_mesh(node: Node3D, relative: Transform3D) -> float:
	var transform := relative * node.transform
	var low := INF
	if node is MeshInstance3D and node.mesh != null:
		var bounds: AABB = node.get_aabb()
		for i in range(8):
			low = minf(low,(transform * bounds.get_endpoint(i)).y)
	for child in node.get_children():
		if child is Node3D: low = minf(low,lowest_mesh(child,transform))
	return low
func ground_visual(model: Node3D):
	var low := lowest_mesh(model,Transform3D.IDENTITY)
	if is_finite(low): model.position.y -= low
func body(pos: Vector3, asset: String, color: Color, is_goblin := false) -> CharacterBody3D:
	var b := CharacterBody3D.new()
	add_child(b)
	b.position = pos
	var c := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.22 if is_goblin else 0.3
	capsule.height = 1.05 if is_goblin else 1.7
	c.shape = capsule
	c.position.y = capsule.height * 0.5
	b.add_child(c)
	if ResourceLoader.exists(asset):
		var model = load(asset).instantiate()
		model.name = "CharacterVisual"
		b.add_child(model)
		ground_visual(model)
		if not is_goblin: SHARED.attach(model)
	else:
		var mesh := CapsuleMesh.new()
		mesh.radius = 0.3
		mesh.height = 1.7
		mesh_at(b,mesh,Vector3(0,0.85,0),color)
	return b
func _ready():
	PLATFORM.configure_window()
	pc_mode = PLATFORM.is_pc()
	var preferences := get_node_or_null("/root/DesktopFullscreen")
	if preferences != null:
		camera_angle = preferences.camera_angle
		camera_ortho_size = preferences.camera_size
		pc_camera_distance = preferences.camera_distance
		camera_fov = preferences.camera_fov
		first_person = preferences.camera_first_person if pc_mode else false
	if ForestLoading.visible:
		ForestLoading.report(0.5, "Initializing character, lighting and world...")
	rng.seed = 812
	var ground := StaticBody3D.new()
	add_child(ground)
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(220,1,220)
	shape.shape = box
	ground.add_child(shape)
	ground.position.y = -0.5
	var floor_mesh := BoxMesh.new()
	floor_mesh.size = box.size
	var floor_visual := mesh_at(ground,floor_mesh,Vector3.ZERO,Color("#526b40"))
	var terrain_shader := Shader.new()
	terrain_shader.code = """shader_type spatial;
uniform sampler2D grass_texture : source_color, repeat_enable, filter_linear_mipmap;
varying vec3 world_pos;
void vertex() { world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz; }
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise2(vec2 p) {
	vec2 i=floor(p), f=fract(p);
	f=f*f*(3.0-2.0*f);
	return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
void fragment() {
	vec2 p=world_pos.xz;
	// Periodic seamless atlas and world-space UVs: no visible square seams.
	// A second rotated sample breaks up the repeat pattern at distance.
	vec2 warp=vec2(noise2(p*0.18+vec2(4.1,2.3)),noise2(p*0.17-vec2(3.7,5.2)))-vec2(0.5);
	vec2 uv=p*0.145+warp*0.035;
	vec2 turned=vec2(p.x*0.102+p.y*0.071,p.y*0.102-p.x*0.071)+vec2(0.38,0.61)-warp*0.028;
	vec3 primary=texture(grass_texture,uv).rgb;
	vec3 secondary=texture(grass_texture,turned).rgb;
	float meadow=noise2(p*0.075);
	float micro=noise2(p*2.8);
	float mix_factor=0.20+0.17*noise2(p*0.14);
	vec3 blades=mix(primary,secondary,mix_factor);
	float soil=smoothstep(0.70,0.91,noise2(p*0.26));
	// Dark forest earth beneath original material tiles; no bright neon seams.
	ALBEDO=mix(blades*(0.55+0.10*meadow+0.025*micro),vec3(0.29,0.27,0.20),0.34);
	ALBEDO=mix(ALBEDO,vec3(0.22,0.20,0.15),soil*0.22);
	ROUGHNESS=0.95;
}"""
	var terrain_material := ShaderMaterial.new()
	terrain_material.shader = terrain_shader
	terrain_material.set_shader_parameter("grass_texture",preload("res://grass_seamless.png"))
	floor_visual.material_override = terrain_material
	# Layer imported 5ft terrain above the existing continuous, collidable grass floor.
	# Terrain is generated in six MultiMesh batches, one asset per frame.
	terrain_layer = preload("res://terrain_layer.gd").new()
	add_child(terrain_layer)
	# Small original-source forest debris/vegetation, grouped into MultiMeshes.
	ground_cover_layer = preload("res://ground_cover_layer.gd").new()
	add_child(ground_cover_layer)
	# Generate the same positions, scales, rotations and proportions as before
	# without creating/rendering/colliding 400+ objects in a single frame.
	for i in range(260):
		var p := Vector3(rng.randf_range(-95,95),0,rng.randf_range(-95,95))
		if p.length() < 8 or (p.x > 5.0 and p.x < 35.0 and absf(p.z) < 12.5): continue
		var scale_factor := rng.randf_range(1.6,3.6)
		if rng.randf() < 0.08: scale_factor *= 1.7
		var path := "res://assets/pine.glb" if rng.randf() < 0.55 else "res://assets/broadleaf.glb"
		scenery_jobs.append({"kind":"tree","position":p,"scale":scale_factor,
			"path":path,"rotation":rng.randf_range(0,TAU)})
	for i in range(160):
		var p := Vector3(rng.randf_range(-95,95),0,rng.randf_range(-95,95))
		if p.length() < 3 or (p.x > 5.0 and p.x < 35.0 and absf(p.z) < 12.5): continue
		scenery_jobs.append({"kind":"rock","position":p,
			"scale":rng.randf_range(0.5,2.0),"rotation":rng.randf_range(0,TAU)})
	# Build nearby scenery first so the immediate play area does not pop in
	# after distant trees. This changes only build order, not placement.
	scenery_jobs.sort_custom(func(a: Dictionary,b: Dictionary) -> bool:
		return (a.position as Vector3).length_squared() < (b.position as Vector3).length_squared())
	# Tavern opens toward the spawn along the dirt approach; build it first
	# while the existing loading overlay remains active.
	scenery_jobs.insert(0, {"kind":"tavern","position":Vector3(21,0,0)})
	var profile := ConfigFile.new()
	profile.load("user://hero.cfg")
	model_index = clampi(int(profile.get_value("hero","model",0)),0,MODELS.size()-1)
	var hero_asset := _player_asset()
	if not ResourceLoader.exists(hero_asset): push_error("Human model is missing")
	player = body(Vector3.ZERO,hero_asset,Color.BEIGE)
	var quest_save:=ConfigFile.new()
	if quest_save.load("user://quest_progress.cfg")==OK:
		quest_goblin_kills=clampi(int(quest_save.get_value("goblins","defeated",0)),0,10)
	enemies = preload("res://enemy_spawner.gd").new()
	add_child(enemies)
	enemies.cleared_goblins=quest_goblin_kills
	enemies.setup(player)
	enemies.on_player_hit = _on_enemy_attack
	enemies.on_kill=func(count: int):
		quest_goblin_kills=mini(count,10)
		var quest_writer:=ConfigFile.new()
		quest_writer.set_value("goblins","defeated",quest_goblin_kills)
		quest_writer.save("user://quest_progress.cfg")
		if is_instance_valid(inventory_panel):
			inventory_panel.set_goblin_kills(count)
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-55,-35,0)
	sun.shadow_enabled = true
	var env := WorldEnvironment.new()
	env.environment = Environment.new()
	env.environment.background_mode = Environment.BG_COLOR
	env.environment.background_color = Color("#a5b8c2")
	env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.environment.ambient_light_color = Color.WHITE
	env.environment.ambient_light_energy = 0.7
	add_child(env)
	camera = Camera3D.new()
	add_child(camera)
	camera.current = true
	camera.projection = Camera3D.PROJECTION_PERSPECTIVE if pc_mode else Camera3D.PROJECTION_ORTHOGONAL
	camera.fov = camera_fov
	camera.size = camera_ortho_size
	camera.rotation = Vector3(-deg_to_rad(camera_angle), yaw, 0)
	camera.far = 300 * FOOT
	if first_person: set_first_person(true)
	var ui := CanvasLayer.new()
	add_child(ui)
	hud = preload("res://hud.gd").new()
	hud.pc_mode = pc_mode
	ui.add_child(hud)
	hud.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	hud.movement.connect(func(value): stick = value)
	hud.command.connect(hud_action)
	var marker := Label3D.new()
	marker.name = "TargetLockMarker"
	marker.text = "LOCKED"
	marker.font_size = 52
	marker.pixel_size = 0.004
	marker.outline_size = 8
	marker.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	marker.no_depth_test = true
	marker.modulate = Color("#ffe17e")
	marker.visible = false
	add_child(marker)
	target_marker = marker
	_update_combat_hud()
	menu_panel = PanelContainer.new()
	menu_panel.position = Vector2(48,110) if pc_mode else Vector2(170,120)
	menu_panel.custom_minimum_size = Vector2(380,0)
	ui.add_child(menu_panel)
	var menu := VBoxContainer.new()
	menu_panel.add_child(menu)
	var releases: VBoxContainer = preload("res://pc_release_updates.gd").new() if pc_mode else preload("res://release_updates.gd").new()
	menu.add_child(releases)
	angle_button = Button.new()
	angle_button.text = "Mouse: Look Around" if pc_mode else "Camera: 60°"
	angle_button.disabled = pc_mode
	menu.add_child(angle_button)
	angle_button.pressed.connect(toggle_angle)
	# Live, saved edge-only terrain blending: 0-30 inches per neighboring edge.
	var blending_row := HBoxContainer.new()
	menu.add_child(blending_row)
	terrain_blend_label = Label.new()
	terrain_blend_label.text = "Terrain edges: %d in" % roundi(terrain_layer.blend_inches)
	terrain_blend_label.custom_minimum_size.x = 170
	blending_row.add_child(terrain_blend_label)
	var blending_slider := HSlider.new()
	blending_slider.name = "TerrainBlendSlider"
	blending_slider.min_value = 0.0
	blending_slider.max_value = 30.0
	blending_slider.step = 1.0
	blending_slider.custom_minimum_size.x = 175
	blending_slider.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	blending_slider.value = terrain_layer.blend_inches
	blending_slider.value_changed.connect(func(value: float):
		terrain_layer.set_blend_inches(value)
		terrain_blend_label.text = "Terrain edges: %d in" % roundi(value))
	blending_row.add_child(blending_slider)
	for title in ["Run","Turn","Main Menu","Close"]:
		var button := Button.new()
		button.text = title
		menu.add_child(button)
		button.pressed.connect(func():
			if title == "Close": hud_action("Menu")
			elif title == "Main Menu": _leave_world()
			else: action(title))
	menu_panel.hide()
	_sync_pc_mouse_mode()
func _place_scenery(job: Dictionary) -> void:
	if str(job.kind) == "tavern":
		var tavern := preload("res://tavern_site.gd").new() as StaticBody3D
		tavern.position = job.position
		tavern.rotation.y = -PI / 2.0
		add_child(tavern)
	elif str(job.kind) == "tree":
		var tree := StaticBody3D.new()
		add_child(tree)
		tree.position = job.position
		var path: String = job.path
		if not scenery_scene_cache.has(path):
			scenery_scene_cache[path] = ForestLoading.get_scenery(path)
		var model := (scenery_scene_cache[path] as PackedScene).instantiate() as Node3D
		tree.add_child(model)
		var scale_factor: float = job.scale
		model.scale = Vector3.ONE * scale_factor
		model.rotation.y = job.rotation
		var col := CollisionShape3D.new()
		var cylinder := CylinderShape3D.new()
		cylinder.radius = 0.095 * scale_factor
		cylinder.height = 1.1 * scale_factor
		col.shape = cylinder
		col.position.y = cylinder.height / 2.0
		tree.add_child(col)
	else:
		if not scenery_scene_cache.has("rock"):
			scenery_scene_cache["rock"] = ForestLoading.get_scenery("res://assets/boulder.glb")
		var rock := (scenery_scene_cache["rock"] as PackedScene).instantiate() as Node3D
		add_child(rock)
		rock.position = job.position
		rock.scale = Vector3.ONE * float(job.scale)
		rock.rotation.y = job.rotation

func _process(_delta: float) -> void:
	if scenery_finished: return
	var deadline: int = Time.get_ticks_usec() + SCENERY_FRAME_BUDGET_USEC
	var placed_this_frame := 0
	while scenery_next < scenery_jobs.size() and placed_this_frame < SCENERY_MAX_PER_FRAME:
		_place_scenery(scenery_jobs[scenery_next])
		scenery_next += 1
		scenery_placed += 1
		placed_this_frame += 1
		if Time.get_ticks_usec() >= deadline: break
	if ForestLoading.visible:
		ForestLoading.report(0.5 + 0.5 * float(scenery_next) / float(maxi(1, scenery_jobs.size())),
			"Growing forest scenery... %d / %d" % [scenery_next, scenery_jobs.size()])
	if scenery_next >= scenery_jobs.size():
		scenery_finished = true
		scenery_jobs.clear()
		scenery_scene_cache.clear()
		if ForestLoading.visible:
			ForestLoading.finish()
			_sync_pc_mouse_mode()
		set_process(false)

func _sync_pc_mouse_mode() -> void:
	if not pc_mode: return
	var ui_open := ForestLoading.visible or is_instance_valid(inventory_panel) or (is_instance_valid(menu_panel) and menu_panel.visible)
	mouse_looking = not ui_open
	if DisplayServer.get_name() != "headless":
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if ui_open else Input.MOUSE_MODE_CAPTURED

func _leave_world() -> void:
	get_tree().paused = false
	if pc_mode:
		mouse_looking = false
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	get_tree().change_scene_to_file("res://front_menu.tscn")

func _notification(what: int) -> void:
	if not pc_mode: return
	if what == NOTIFICATION_WM_WINDOW_FOCUS_OUT:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	elif what == NOTIFICATION_WM_WINDOW_FOCUS_IN and is_inside_tree():
		_sync_pc_mouse_mode()

func _inventory_closed() -> void:
	get_tree().paused = false
	if is_instance_valid(inventory_panel):
		inventory_panel.queue_free()
		inventory_panel = null
	if is_instance_valid(hud): hud.set_process_input(true)
	_sync_pc_mouse_mode()

func _exit_tree() -> void:
	if get_tree() != null: get_tree().paused = false

func toggle_inventory(tab: String = "") -> void:
	if is_instance_valid(inventory_panel) and tab != "":
		inventory_panel._select_tab(tab)
		return
	if is_instance_valid(inventory_panel):
		_inventory_closed()
		return
	if is_instance_valid(menu_panel): menu_panel.hide()
	stick = Vector2.ZERO
	hud.set_process_input(false)
	var overlay := preload("res://inventory_screen.tscn").instantiate() as Control
	overlay.process_mode = Node.PROCESS_MODE_ALWAYS
	inventory_panel = overlay
	hud.get_parent().add_child(overlay)
	overlay.equipment_changed.connect(_on_equipment_changed)
	overlay.set_model(MODELS[model_index])
	overlay.set_goblin_kills(quest_goblin_kills)
	overlay.set_character_summary({"HP":player_hp,"MaxHP":int(COMBAT.TEST_PLAYER.hp),"AC":int(COMBAT.TEST_PLAYER.ac)})
	if tab != "": overlay._select_tab(tab)
	overlay.closed.connect(_inventory_closed)
	_sync_pc_mouse_mode()
	get_tree().paused = true

func _unhandled_key(event: InputEvent) -> void:
	if not pc_mode or not (event is InputEventKey): return
	var key := event as InputEventKey
	if not key.pressed or key.echo: return
	if key.keycode == KEY_ESCAPE:
		if is_instance_valid(inventory_panel):
			toggle_inventory()
		else:
			hud_action("Menu")
	elif key.keycode == KEY_I:
		toggle_inventory()
	elif key.keycode == KEY_Q:
		toggle_inventory("Quest")
	elif is_instance_valid(inventory_panel) or menu_panel.visible:
		return
	elif key.keycode == KEY_SPACE:
		action("Jump")
	elif key.keycode == KEY_C:
		hud_action("Crouch")
	elif key.keycode == KEY_V:
		set_first_person(not first_person)
	elif key.keycode == KEY_TAB:
		hud_action("Lock")
	elif key.keycode == KEY_F:
		hud_action("A")

func _unhandled_input(event: InputEvent) -> void:
	if not pc_mode: return
	if event is InputEventKey:
		_unhandled_key(event)
		return
	if event is InputEventMouseButton:
		var mouse := event as InputEventMouseButton
		if mouse.pressed and not menu_panel.visible and not is_instance_valid(inventory_panel) and mouse.button_index in [MOUSE_BUTTON_WHEEL_UP,MOUSE_BUTTON_WHEEL_DOWN]:
			var increment := -0.5 if mouse.button_index==MOUSE_BUTTON_WHEEL_UP else 0.5
			if first_person:
				camera_fov = clampf(camera_fov + increment*4.0,55.0,95.0)
				camera.fov = camera_fov
			else:
				pc_camera_distance = clampf(pc_camera_distance+increment,2.0,15.0)
			_save_camera_preferences()
			get_viewport().set_input_as_handled()
			return
		if mouse.button_index == MOUSE_BUTTON_LEFT and mouse.pressed and not menu_panel.visible and not is_instance_valid(inventory_panel):
			action("Attack")
	elif event is InputEventMouseMotion and mouse_looking:
		camera_yaw -= event.relative.x * MOUSE_SENSITIVITY
		camera_pitch = clampf(camera_pitch+event.relative.y*MOUSE_SENSITIVITY,deg_to_rad(-75),deg_to_rad(85))
		get_viewport().set_input_as_handled()

func _save_camera_preferences() -> void:
	var preferences := get_node_or_null("/root/DesktopFullscreen")
	if preferences != null:
		preferences.save_camera(first_person,camera_angle,camera_ortho_size,pc_camera_distance,camera_fov)

func set_first_person(enabled: bool) -> void:
	first_person = enabled
	_save_camera_preferences()
	camera.projection = Camera3D.PROJECTION_PERSPECTIVE if enabled or pc_mode else Camera3D.PROJECTION_ORTHOGONAL
	# Originals are restored when returning to third person. Character rigs,
	# textures and animation resources are never altered on disk.
	for entry in first_person_meshes:
		if is_instance_valid(entry.instance):
			(entry.instance as MeshInstance3D).mesh = entry.original
	if not enabled: return
	if not first_person_meshes.is_empty():
		for entry in first_person_meshes:
			if is_instance_valid(entry.instance):
				(entry.instance as MeshInstance3D).mesh = entry.filtered
		return
	var model := player.get_node_or_null("CharacterVisual")
	if model == null: return
	for child in model.find_children("*","MeshInstance3D",true,false):
		var instance := child as MeshInstance3D
		if instance == null or instance.mesh == null: continue
		var truncated := _first_person_body_mesh(instance.mesh)
		if truncated == null: continue
		first_person_meshes.append({"instance":instance,"original":instance.mesh,"filtered":truncated})
		instance.mesh = truncated

func _first_person_body_mesh(original: Mesh) -> ArrayMesh:
	# Both human models use one skinned mesh, so hiding a separate Head
	# MeshInstance would also hide the chest, legs and feet. Keep skinned
	# surfaces intact except triangles in the head/face above 1.43m.
	var result := ArrayMesh.new()
	for surface in original.get_surface_count():
		if original.surface_get_primitive_type(surface) != Mesh.PRIMITIVE_TRIANGLES: return null
		var arrays := original.surface_get_arrays(surface)
		var points: PackedVector3Array = arrays[Mesh.ARRAY_VERTEX]
		var indexes: PackedInt32Array = arrays[Mesh.ARRAY_INDEX]
		if indexes.is_empty(): return null
		var kept := PackedInt32Array()
		for i in range(0,indexes.size(),3):
			if i+2>=indexes.size(): break
			var a: int = indexes[i]
			var b: int = indexes[i+1]
			var c: int = indexes[i+2]
			if points[a].y < 1.43 and points[b].y < 1.43 and points[c].y < 1.43:
				kept.append_array(PackedInt32Array([a,b,c]))
		arrays[Mesh.ARRAY_INDEX] = kept
		result.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
		result.surface_set_material(surface,original.surface_get_material(surface))
	return result

func hud_action(name: String):
	if is_instance_valid(inventory_panel): return
	if menu_panel.visible and name != "Menu": return
	if name == "Menu":
		menu_panel.visible = not menu_panel.visible
		stick = Vector2.ZERO
		_sync_pc_mouse_mode()
	elif name == "Attack": action("Attack")
	elif name == "Lock":
		lock_enabled = not lock_enabled
		if not lock_enabled:
			locked_target = null
		_refresh_target()
		hud.toggles["Lock"] = lock_enabled
		hud.queue_redraw()
	elif name == "A" or name == "Attack Mode":
		auto_attack_enabled = not auto_attack_enabled
		hud.toggles["A"] = auto_attack_enabled
		hud.toggles["Attack Mode"] = auto_attack_enabled
		hud.queue_redraw()
	elif name == "Jump": action("Jump")
	elif name == "Crouch":
		sneaking = not sneaking
		hud.toggles["Crouch"] = sneaking
		hud.queue_redraw()
	elif name == "Inventory": toggle_inventory()
	elif name == "Pause":
		menu_panel.show()
		stick = Vector2.ZERO
		_sync_pc_mouse_mode()
	else:
		var notice := AcceptDialog.new()
		notice.dialog_text = name + " is not yet migrated."
		hud.add_child(notice)
		notice.confirmed.connect(notice.queue_free)
		notice.canceled.connect(notice.queue_free)
		notice.popup_centered()

func play_imported_animation(state: String) -> void:
	var visual := player.get_node_or_null("CharacterVisual")
	if visual == null: return
	var animator := visual.get_node_or_null("SharedHumanoidAnimations") as AnimationPlayer
	if animator == null: animator = SHARED.attach(visual)
	if animator == null: return
	var clip := "Running" if state == "run" else SHARED.gameplay_walk_clip(MODELS[model_index]) if state == "walk" else "Movement_Sneak_01_Standard" if state == "sneak" else "Movement_Jump_01_Standard" if state == "jump" else "Attack_MeleeTwoHanded_01_OverheadSmash" if state == "attack" else "Idle"
	if not animator.has_animation(clip): return
	var resource := animator.get_animation(clip)
	animator.speed_scale = resource.length / 0.5 if state == "attack" else resource.length / 0.65 if state == "jump" else 1.0
	if animator.assigned_animation == clip and animator.is_playing(): return
	animator.play(clip)
func toggle_angle():
	camera_angle = 75.0 if camera_angle == 60.0 else 60.0
	_save_camera_preferences()
	angle_button.text = "Camera: %d°" % int(camera_angle)
func action(title: String):
	if title == "Run": running = not running
	elif title == "Turn": yaw += PI/4
	elif title == "Jump":
		if jump_cooldown <= 0.0 and player.is_on_floor():
			jump_cooldown = TEST_JUMP_COOLDOWN_SECONDS
			jump_animation_left = 0.65
			player.velocity.y = JUMP_VELOCITY
	elif title == "Attack":
		_attack_once()

func _on_enemy_attack(kind: String, hit: bool, damage: int, attack_name: String) -> void:
	if player_hp <= 0: return
	if hit:
		player_hp = maxi(0,player_hp-damage)
		_show_combat_notice("%s %s: -%d HP" % [kind, attack_name, damage])
		if player_hp == 0:
			_show_combat_notice("DEFEATED - return to the main menu")
	else:
		_show_combat_notice("%s missed you" % kind)
	_update_combat_hud()

func _show_combat_notice(message: String) -> void:
	combat_notice = message
	combat_notice_left = 1.6
	_update_combat_hud()

func _refresh_target() -> void:
	if not is_instance_valid(enemies): return
	if not lock_enabled:
		locked_target = null
	else:
		if not enemies.target_is_alive(locked_target) or enemies.flat_distance(
			player.global_position, locked_target.global_position) > AUTO_LOCK_RANGE*1.5:
			locked_target = enemies.nearest_target(player.global_position,AUTO_LOCK_RANGE)
	if is_instance_valid(target_marker):
		target_marker.visible = is_instance_valid(locked_target)
		if is_instance_valid(locked_target):
			var info: Dictionary = enemies.get_target_stats(locked_target)
			target_marker.global_position = locked_target.global_position + Vector3(0,1.8,0)
			if not info.is_empty():
				target_marker.text = "LOCK  %s  %d/%d HP" % [
					str(info.kind),int(info.hp),int(info.max_hp)]

func _attack_once() -> void:
	if attack > 0.0 or player_hp <= 0: return
	attack = TEST_ATTACK_COOLDOWN_SECONDS
	punch_left = 0.5
	if not is_instance_valid(enemies): return
	var target: CharacterBody3D = locked_target if enemies.target_is_alive(locked_target) else null
	if target == null:
		target = enemies.nearest_target(player.global_position,AUTO_ATTACK_RANGE)
	if not is_instance_valid(target): return
	var horizontal_distance: float = enemies.flat_distance(player.global_position,target.global_position)
	if horizontal_distance > AUTO_ATTACK_RANGE:
		_show_combat_notice("Target out of melee range")
		return
	var facing := target.global_position-player.global_position
	facing.y = 0.0
	if facing.length_squared() > 0.001:
		player.rotation.y = atan2(facing.x,facing.z)
	var result: Dictionary = enemies.damage_target(target,COMBAT.TEST_PLAYER)
	if not bool(result.get("valid",false)):return
	if not bool(result.get("hit",false)):
		_show_combat_notice("Miss: %s (AC %d)" % [str(result.kind),
			int(enemies.get_target_stats(target).get("ac",0))])
	elif bool(result.get("defeated",false)):
		_show_combat_notice("Defeated %s" % str(result.kind))
	else:
		_show_combat_notice("Hit %s for %d (%d HP left)" % [
			str(result.kind),int(result.damage),int(result.remaining_hp)])
	_refresh_target()

func _update_combat_hud() -> void:
	if not is_instance_valid(hud): return
	var value := "HP %d/%d   AC %d" % [
		player_hp,int(COMBAT.TEST_PLAYER.hp),int(COMBAT.TEST_PLAYER.ac)]
	if auto_attack_enabled: value += "   AUTO"
	if lock_enabled: value += "   LOCK"
	if hud.combat_status != value or hud.combat_notice != combat_notice:
		hud.combat_status = value
		hud.combat_notice = combat_notice
		hud.queue_redraw()

func _physics_process(dt):
	var input_stick := stick
	if pc_mode:
		input_stick = Vector2(float(Input.is_key_pressed(KEY_D))-float(Input.is_key_pressed(KEY_A)),
			float(Input.is_key_pressed(KEY_S))-float(Input.is_key_pressed(KEY_W))).limit_length()
	if is_instance_valid(inventory_panel) or (is_instance_valid(menu_panel) and menu_panel.visible):
		input_stick = Vector2.ZERO
	var direction := Vector3(input_stick.x,0,input_stick.y).rotated(Vector3.UP,camera_yaw if pc_mode else yaw)
	var stick_running := Input.is_key_pressed(KEY_SHIFT) if pc_mode else stick.length() >= RUN_STICK_THRESHOLD
	var is_running := (running or stick_running) and not sneaking
	var vertical := player.velocity.y
	var speed_multiplier := SNEAK_SPEED_FACTOR if sneaking else 2.0 if is_running else 1.0
	player.velocity.x = direction.x * WALK_SPEED * speed_multiplier
	player.velocity.z = direction.z * WALK_SPEED * speed_multiplier
	player.velocity.y = -0.5 if player.is_on_floor() and vertical <= 0.0 else vertical - 9.8*dt
	player.move_and_slide()
	_refresh_target()
	var look_at_target := is_instance_valid(locked_target) and lock_enabled
	if look_at_target:
		var facing := locked_target.global_position-player.global_position
		facing.y=0.0
		if facing.length_squared()>0.001:
			player.rotation.y=atan2(facing.x,facing.z)
	elif pc_mode and first_person:
		player.rotation.y = camera_yaw + PI
	elif direction.length() > 0.1:
		player.rotation.y = atan2(direction.x,direction.z)
	if jump_animation_left > 0.0:
		play_imported_animation("jump")
		jump_animation_left = maxf(0.0, jump_animation_left - dt)
	elif punch_left > 0.0:
		play_imported_animation("attack")
		punch_left = maxf(0.0, punch_left - dt)
	else:
		play_imported_animation("sneak" if sneaking else "run" if is_running and direction.length() > 0.1 else "walk" if direction.length() > 0.1 else "idle")
	if pc_mode:
		camera.v_offset = 0.0
		camera.h_offset = 0.0
		var cam_eye := player.position+Vector3.UP*1.57
		var orbit := Vector3(0,0,pc_camera_distance).rotated(Vector3.RIGHT,-camera_pitch).rotated(Vector3.UP,camera_yaw)
		# Offset first-person eye slightly forward of the face. Look-down
		# reveals the original skinned torso/legs/feet (head triangles omitted).
		camera.position = cam_eye - Vector3(0,0,0.30).rotated(Vector3.UP,camera_yaw) if first_person else cam_eye + orbit
		camera.rotation = Vector3(-camera_pitch,camera_yaw,0)
	else:
		var pitch := deg_to_rad(camera_angle)
		camera.position = player.position + Vector3(0,20*sin(pitch),20*cos(pitch)).rotated(Vector3.UP,yaw)
		camera.rotation = Vector3(-pitch,yaw,0)
	# Frame the player inside the unobstructed play area, not the full screen.
	# HUD top ends at 100; bottom controls begin 460 logical pixels from bottom.
	var view_height := get_viewport().get_visible_rect().size.y
	var hud_height := hud.size.y
	if not pc_mode and view_height > 0.0 and hud_height > 560.0:
		var target_y := (100.0 + hud_height - 460.0) * 0.5 / hud_height * view_height
		camera.v_offset = 0.0
		var projected_y := camera.unproject_position(player.position + Vector3.UP * 0.85).y
		camera.v_offset = (target_y - projected_y) * camera.size / view_height
	attack = maxf(0.0, attack-dt)
	jump_cooldown = maxf(0.0, jump_cooldown-dt)
	combat_notice_left = maxf(0.0,combat_notice_left-dt)
	if combat_notice_left == 0.0 and not combat_notice.is_empty():
		combat_notice = ""
	if auto_attack_enabled and attack <= 0.0 and player_hp > 0 and not is_instance_valid(inventory_panel) and not menu_panel.visible:
		var target: CharacterBody3D = locked_target if enemies.target_is_alive(locked_target) else enemies.nearest_target(player.global_position,AUTO_ATTACK_RANGE)
		if is_instance_valid(target) and enemies.flat_distance(player.global_position,target.global_position) <= AUTO_ATTACK_RANGE:
			_attack_once()
	_update_combat_hud()

