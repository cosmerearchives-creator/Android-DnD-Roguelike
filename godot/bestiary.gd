extends Control
# A reusable, touch-friendly creature compendium. Additional creatures can be added to ENTRIES.
const ENTRIES = [{
	"name": "Goblin Warrior", "asset": "res://assets/goblin_warrior.glb",
	"subtitle": "Small humanoid (goblinoid), neutral evil",
	"details": [
		["Size", "Small"], ["Creature type", "Humanoid (goblinoid)"],
		["Alignment", "Neutral evil"], ["Armor class", "15 (leather armor, shield)"],
		["Hit points", "7 (2d6)"], ["Speed", "30 ft."],
		["STR / DEX / CON", "8 (-1) / 14 (+2) / 10 (+0)"],
		["INT / WIS / CHA", "10 (+0) / 8 (-1) / 8 (-1)"],
		["Skills", "Stealth +6"], ["Senses", "Darkvision 60 ft., passive Perception 9"],
		["Languages", "Common, Goblin"], ["Damage immunities", "None"],
		["Condition immunities", "None"], ["Challenge rating", "1/4 (50 XP)"]
	],
	"abilities": [
		["Nimble Escape", "The goblin can take the Disengage or Hide action as a bonus action on each of its turns."]
	],
	"attacks": [
		["Scimitar", "Melee Weapon Attack: +4 to hit, reach 5 ft., one target. Hit: 5 (1d6 + 2) slashing damage."],
		["Shortbow", "Ranged Weapon Attack: +4 to hit, range 80/320 ft., one target. Hit: 5 (1d6 + 2) piercing damage."]
	]
}]
const GOBLINS = preload("res://goblin_variants.gd")
const WALK_COMPARISON := "res://assets/classic_study_shared_walk.glb"
const GOLD := Color("#cfb576")
const PALE := Color("#f3dfb8")
const DARK := Color("#0b292e")
const SERIF = preload("res://hud_serif.ttf")
var viewport: SubViewport
var creature_root: Node3D
var touch_points: Dictionary = {}
var camera: Camera3D
var preview_animator: AnimationPlayer
var preview_model: Node3D
var preview_controls: HBoxContainer
var preview_title: Label
var preview_subtitle: Label
var comparison_button: Button
var showing_comparison := false
var variant_index := 0
var variant_picker: OptionButton

func _panel() -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = Color(0.035, 0.11, 0.13, 0.93)
	style.border_color = GOLD
	style.set_border_width_all(2)
	style.set_corner_radius_all(12)
	style.content_margin_left = 16
	style.content_margin_right = 16
	style.content_margin_top = 12
	style.content_margin_bottom = 12
	return style

func _label(value: String, size: int = 23, accent: bool = false) -> Label:
	var label := Label.new()
	label.text = value
	label.add_theme_font_override("font", SERIF)
	label.add_theme_font_size_override("font_size", size)
	label.add_theme_color_override("font_color", GOLD if accent else PALE)
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	return label

func _button(value: String, callback: Callable) -> Button:
	var result := Button.new()
	result.text = value
	result.custom_minimum_size.y = 62
	result.add_theme_font_override("font", SERIF)
	result.add_theme_font_size_override("font_size", 25)
	result.add_theme_color_override("font_color", PALE)
	result.add_theme_stylebox_override("normal", _panel())
	result.pressed.connect(callback)
	return result

func _ready() -> void:
	var bg := TextureRect.new()
	bg.texture = preload("res://menu_camp.webp")
	bg.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	add_child(bg)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)

	var safe := MarginContainer.new()
	add_child(safe)
	safe.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	safe.add_theme_constant_override("margin_left", 18)
	safe.add_theme_constant_override("margin_right", 18)
	safe.add_theme_constant_override("margin_top", 24)
	safe.add_theme_constant_override("margin_bottom", 24)
	var layout := VBoxContainer.new()
	layout.add_theme_constant_override("separation", 12)
	safe.add_child(layout)

	var header := HBoxContainer.new()
	layout.add_child(header)
	var back := _button("‹ Back", func(): get_tree().change_scene_to_file("res://front_menu.tscn"))
	back.custom_minimum_size.x = 145
	header.add_child(back)
	var title := _label("BESTIARY", 38, true)
	title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	header.add_child(title)
	var blank := Control.new()
	blank.custom_minimum_size.x = 145
	header.add_child(blank)

	var entry := ENTRIES[0]
	preview_title = _label(GOBLINS.ENTRIES[variant_index].name, 36, true)
	preview_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	layout.add_child(preview_title)
	preview_subtitle = _label(entry.subtitle, 19)
	preview_subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	layout.add_child(preview_subtitle)

	var preview_panel := PanelContainer.new()
	preview_panel.add_theme_stylebox_override("panel", _panel())
	preview_panel.size_flags_vertical = Control.SIZE_EXPAND_FILL
	preview_panel.size_flags_stretch_ratio = 1.0
	preview_panel.custom_minimum_size.y = 235
	layout.add_child(preview_panel)
	var preview_stack := VBoxContainer.new()
	preview_panel.add_child(preview_stack)
	var holder := SubViewportContainer.new()
	holder.stretch = true
	holder.size_flags_vertical = Control.SIZE_EXPAND_FILL
	holder.mouse_filter = Control.MOUSE_FILTER_STOP
	preview_stack.add_child(holder)
	viewport = SubViewport.new()
	viewport.size = Vector2i(600, 430)
	viewport.transparent_bg = true
	viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	holder.add_child(viewport)
	var scene := Node3D.new()
	viewport.add_child(scene)
	creature_root = Node3D.new()
	scene.add_child(creature_root)
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-35, -35, 0)
	sun.light_energy = 2.0
	scene.add_child(sun)
	var fill := OmniLight3D.new()
	fill.position = Vector3(-2, 2, 2)
	fill.light_energy = 1.4
	scene.add_child(fill)
	camera = Camera3D.new()
	scene.add_child(camera)
	camera.current = true
	camera.fov = 45.0
	_load_creature(GOBLINS.ENTRIES[variant_index].asset)
	holder.gui_input.connect(_preview_input)
	var hint := _label("Drag to rotate  •  Pinch to zoom", 17, true)
	hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	preview_stack.add_child(hint)
	# Temporary review controls make the new rig easy to approve in-game.
	preview_controls = HBoxContainer.new()
	preview_controls.add_theme_constant_override("separation", 8)
	preview_stack.add_child(preview_controls)
	for clip in ["Idle", "Walk", "Attack"]:
		var preview_button := _button(clip, _play_preview_animation.bind(clip))
		preview_button.custom_minimum_size.y = 44
		preview_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		preview_controls.add_child(preview_button)
	comparison_button = _button("Compare Walk on Classic Study", _toggle_walk_comparison)
	comparison_button.custom_minimum_size.y = 44
	preview_stack.add_child(comparison_button)
	variant_picker = OptionButton.new()
	variant_picker.custom_minimum_size.y = 44
	variant_picker.add_theme_font_override("font", SERIF)
	variant_picker.add_theme_font_size_override("font_size", 22)
	for variant in GOBLINS.ENTRIES:
		variant_picker.add_item(variant.name)
	variant_picker.item_selected.connect(_select_goblin_variant)
	preview_stack.add_child(variant_picker)

	var stats_panel := PanelContainer.new()
	stats_panel.add_theme_stylebox_override("panel", _panel())
	stats_panel.size_flags_vertical = Control.SIZE_EXPAND_FILL
	stats_panel.size_flags_stretch_ratio = 1.35
	layout.add_child(stats_panel)
	var scroll := ScrollContainer.new()
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	stats_panel.add_child(scroll)
	var contents := VBoxContainer.new()
	contents.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	contents.add_theme_constant_override("separation", 9)
	scroll.add_child(contents)
	contents.add_child(_label("GOBLIN ATTRIBUTES", 29, true))
	for line in entry.details:
		_add_stat(contents, line[0], line[1])
	contents.add_child(_label("SPECIAL ABILITIES", 29, true))
	for line in entry.abilities:
		_add_description(contents, line[0], line[1])
	contents.add_child(_label("ATTACKS", 29, true))
	for line in entry.attacks:
		_add_description(contents, line[0], line[1])

func _add_stat(parent: VBoxContainer, name: String, value: String) -> void:
	var line := HBoxContainer.new()
	parent.add_child(line)
	var caption := _label(name, 20, true)
	caption.custom_minimum_size.x = 185
	caption.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	line.add_child(caption)
	var result := _label(value, 20)
	result.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	line.add_child(result)

func _add_description(parent: VBoxContainer, name: String, description: String) -> void:
	parent.add_child(_label(name, 23, true))
	parent.add_child(_label(description, 20))

func _select_goblin_variant(index: int) -> void:
	var variant: Dictionary = GOBLINS.ENTRIES[index]
	if not _load_creature(variant.asset):
		variant_picker.select(variant_index)
		return
	variant_index = index
	showing_comparison = false
	preview_title.text = variant.name
	preview_subtitle.text = variant.get("credit", ENTRIES[0].subtitle)
	preview_controls.visible = preview_animator != null
	comparison_button.visible = index == 0
	comparison_button.text = "Compare Walk on Classic Study"

func _toggle_walk_comparison() -> void:
	var next_comparison := not showing_comparison
	var path: String = WALK_COMPARISON if next_comparison else GOBLINS.ENTRIES[variant_index].asset
	if not _load_creature(path, "Walk"):
		comparison_button.text = "Walk preview unavailable"
		return
	showing_comparison = next_comparison
	preview_title.text = "Classic Study · Shared Walk" if showing_comparison else GOBLINS.ENTRIES[variant_index].name
	preview_subtitle.text = "Goblin Warrior Walk on a taller rig" if showing_comparison else ENTRIES[0].subtitle
	preview_controls.visible = not showing_comparison
	comparison_button.text = "Return to Goblin Warrior" if showing_comparison else "Compare Walk on Classic Study"

func _load_creature(path: String, state := "Idle") -> bool:
	if not ResourceLoader.exists(path):
		return false
	var instance: Node3D = load(path).instantiate()
	var bounds := _bounds_for(instance, Transform3D.IDENTITY)
	if bounds.size.length() < 0.01:
		instance.free()
		return false
	if is_instance_valid(preview_model):
		creature_root.remove_child(preview_model)
		preview_model.queue_free()
	preview_model = instance
	creature_root.add_child(instance)
	preview_animator = _find_animator(instance)
	_play_preview_animation(state)
	var center := bounds.get_center()
	instance.position = -center
	var height := maxf(bounds.size.y, 0.2)
	var diameter := maxf(maxf(bounds.size.x, bounds.size.z), height)
	var distance := diameter * 1.65
	camera.position = Vector3(0, 0, distance)
	camera.look_at(Vector3.ZERO)
	camera.near = 0.02
	camera.far = 500.0
	return true

func _bounds_for(node: Node3D, parent_transform: Transform3D) -> AABB:
	var local := parent_transform * node.transform
	var result := AABB()
	var has_bounds := false
	if node is MeshInstance3D and node.mesh:
		var mesh_bounds: AABB = node.get_aabb()
		for corner in range(8):
			var point := local * mesh_bounds.get_endpoint(corner)
			if not has_bounds:
				result = AABB(point, Vector3.ZERO)
				has_bounds = true
			else:
				result = result.expand(point)
	for child in node.get_children():
		if child is Node3D:
			var other := _bounds_for(child, local)
			if other.size.length() > 0.0001:
				if not has_bounds:
					result = other
					has_bounds = true
				else:
					result = result.merge(other)
	return result

func _find_animator(node: Node) -> AnimationPlayer:
	if node is AnimationPlayer:
		return node as AnimationPlayer
	for child in node.get_children():
		var result := _find_animator(child)
		if result != null:
			return result
	return null

func _play_preview_animation(state: String) -> void:
	if preview_animator == null:
		return
	for clip in preview_animator.get_animation_list():
		if clip.to_lower().contains(state.to_lower()):
			var animation := preview_animator.get_animation(clip)
			animation.loop_mode = Animation.LOOP_NONE if state == "Attack" else Animation.LOOP_LINEAR
			preview_animator.play(clip)
			return

func _preview_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			touch_points[event.index] = event.position
		else:
			touch_points.erase(event.index)
	elif event is InputEventScreenDrag:
		if touch_points.size() >= 2 and touch_points.has(event.index):
			var old_points := touch_points.values()
			var previous_distance: float = old_points[0].distance_to(old_points[1])
			touch_points[event.index] = event.position
			var new_points := touch_points.values()
			var current_distance: float = new_points[0].distance_to(new_points[1])
			if previous_distance > 1.0 and current_distance > 1.0:
				camera.position.z = clampf(camera.position.z * previous_distance / current_distance, 0.5, 40.0)
		else:
			touch_points[event.index] = event.position
			creature_root.rotation.y += event.relative.x * 0.012
	elif event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		creature_root.rotation.y += event.relative.x * 0.012
	elif event is InputEventMagnifyGesture:
		camera.position.z = clampf(camera.position.z / event.factor, 0.5, 40.0)
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_WHEEL_UP:
			camera.position.z = maxf(camera.position.z * 0.9, 0.5)
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			camera.position.z = minf(camera.position.z * 1.1, 40.0)
