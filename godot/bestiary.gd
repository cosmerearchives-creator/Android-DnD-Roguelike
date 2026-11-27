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
const FIRE_BEETLE = {
    "name": "Fire Beetle",
    "asset": "res://assets/fire_beetle.glb",
    "subtitle": "Small beast, unaligned",
    "details": [
        ["Size", "Small"], ["Creature type", "Beast"], ["Alignment", "Unaligned"],
        ["Armor class", "13 (natural armor)"], ["Hit points", "4 (1d6 + 1)"],
        ["Speed", "30 ft."], ["STR / DEX / CON", "8 (-1) / 10 (+0) / 12 (+1)"],
        ["INT / WIS / CHA", "1 (-5) / 7 (-2) / 3 (-4)"],
        ["Senses", "Blindsight 30 ft., passive Perception 8"],
        ["Languages", "None"], ["Challenge rating", "0 (10 XP)"]
    ],
    "abilities": [
        ["Illumination", "The beetle sheds bright light in a 10-foot radius and dim light for an additional 10 feet."]
    ],
    "attacks": [
        ["Bite", "Melee Weapon Attack: +1 to hit, reach 5 ft., one target. Hit: 2 (1d6 - 1) slashing damage."]
    ]
}
const TWIG_BLIGHT = {
    "name": "Twig Blight", "asset": "res://assets/twig_blight.glb",
    "subtitle": "Small plant, neutral evil • Bipedal",
    "details": [
        ["Size", "Small"], ["Creature type", "Plant"], ["Alignment", "Neutral evil"],
        ["Armor class", "13 (natural armor)"], ["Hit points", "4 (1d6 + 1)"],
        ["Speed", "20 ft."], ["STR / DEX / CON", "6 (-2) / 13 (+1) / 12 (+1)"],
        ["INT / WIS / CHA", "4 (-3) / 8 (-1) / 3 (-4)"],
        ["Senses", "Blindsight 60 ft. (blind beyond this radius), passive Perception 9"],
        ["Languages", "Understands Common but can't speak"], ["Challenge rating", "1/8 (25 XP)"],
        ["Damage vulnerability", "Fire"], ["Condition immunity", "Blinded, deafened"]
    ],
    "abilities": [["False Appearance", "While motionless, indistinguishable from a dead shrub."]],
    "attacks": [["Claws", "Melee Weapon Attack: +3 to hit, reach 5 ft., one target. Hit: 3 (1d4 + 1) piercing damage. Animation: Attack_Claw_01_BackSwipe."]]
}
const WOLF = {
    "name": "Wolf",
    "asset": "res://assets/wolf_quadruped.glb",
    "subtitle": "Medium beast, unaligned | Quadruped reference (46 bones)",
    "details": [
        ["Size", "Medium"], ["Creature type", "Beast"], ["Alignment", "Unaligned"],
        ["Armor class", "13 (natural armor)"], ["Hit points", "11 (2d8 + 2)"],
        ["Speed", "40 ft."], ["STR / DEX / CON", "12 (+1) / 15 (+2) / 12 (+1)"],
        ["INT / WIS / CHA", "3 (-4) / 12 (+1) / 6 (-2)"],
        ["Skills", "Perception +3, Stealth +4"],
        ["Senses", "Passive Perception 13"], ["Languages", "None"],
        ["Challenge rating", "1/4 (50 XP)"]
    ],
    "abilities": [
        ["Keen Hearing and Smell", "Advantage on Wisdom (Perception) checks relying on hearing or smell."],
        ["Pack Tactics", "Advantage on an attack roll if an ally is within 5 ft. of the target."]
    ],
    "attacks": [["Bite", "Melee Weapon Attack: +4 to hit, reach 5 ft., one target. Hit: 7 (2d4 + 2) piercing damage; DC 11 Strength saving throw or knocked prone."]]
}
const SHARED = preload("res://humanoid_shared_animations.gd")
const QUADRUPED = preload("res://quadruped_shared_animations.gd")
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
var preview_buttons: Array[Button] = []
var preview_clips: Array[String] = []
var preview_title: Label
var preview_subtitle: Label
var variant_index := 0
var creature_picker: OptionButton
var stats_content: VBoxContainer
var active_creature := "Goblin"

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

	# Dedicated creature buttons remain visible on narrow portrait screens.
	var creature_tabs := GridContainer.new()
	creature_tabs.columns = 2
	creature_tabs.add_theme_constant_override("h_separation",8)
	creature_tabs.add_theme_constant_override("v_separation",8)
	layout.add_child(creature_tabs)
	for item in [["Goblin",0],["Fire Beetle",1],["Twig Blight",2],["Wolf",3]]:
		var selector := _button(item[0],_select_creature.bind(item[1]))
		selector.custom_minimum_size.y=58
		selector.size_flags_horizontal=Control.SIZE_EXPAND_FILL
		creature_tabs.add_child(selector)
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
	for slot in 4:
		var preview_button := _button("", _play_preview_slot.bind(slot))
		preview_button.custom_minimum_size.y = 44
		preview_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		preview_controls.add_child(preview_button)
		preview_buttons.append(preview_button)
	_set_preview_controls(["Walk", "Attack", "Axe Spin"],
		["Walk", "Attack", "Axe Spin"])
	creature_picker = OptionButton.new()
	creature_picker.custom_minimum_size.y = 48
	creature_picker.add_theme_font_override("font", SERIF)
	creature_picker.add_theme_font_size_override("font_size", 23)
	creature_picker.add_item("Goblin")
	creature_picker.add_item("Fire Beetle")
	creature_picker.add_item("Twig Blight")
	creature_picker.add_item("Wolf")
	creature_picker.item_selected.connect(_select_creature)
	layout.add_child(creature_picker)
	layout.move_child(creature_picker, 3)
	var stats_panel := PanelContainer.new()
	stats_panel.add_theme_stylebox_override("panel", _panel())
	stats_panel.size_flags_vertical = Control.SIZE_EXPAND_FILL
	stats_panel.size_flags_stretch_ratio = 1.35
	layout.add_child(stats_panel)
	var scroll := ScrollContainer.new()
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	stats_panel.add_child(scroll)
	stats_content = VBoxContainer.new()
	stats_content.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	stats_content.add_theme_constant_override("separation", 9)
	scroll.add_child(stats_content)
	_render_stats(ENTRIES[0])

func _render_stats(entry: Dictionary) -> void:
	for child in stats_content.get_children():
		stats_content.remove_child(child)
		child.queue_free()
	stats_content.add_child(_label(active_creature.to_upper() + " ATTRIBUTES", 29, true))
	for line in entry.details:
		_add_stat(stats_content, line[0], line[1])
	stats_content.add_child(_label("SPECIAL ABILITIES", 29, true))
	for line in entry.abilities:
		_add_description(stats_content, line[0], line[1])
	stats_content.add_child(_label("ATTACKS", 29, true))
	for line in entry.attacks:
		_add_description(stats_content, line[0], line[1])

func _set_preview_controls(clips: Array[String], labels: Array[String]) -> void:
	preview_clips = clips
	for slot in preview_buttons.size():
		preview_buttons[slot].visible = slot < clips.size()
		if slot < labels.size():
			preview_buttons[slot].text = labels[slot]

func _play_preview_slot(slot: int) -> void:
	if slot >= 0 and slot < preview_clips.size():
		_play_preview_animation(preview_clips[slot])

func _select_creature(index: int) -> void:
	if is_instance_valid(creature_picker): creature_picker.select(index)
	if index == 1:
		if not _load_creature(FIRE_BEETLE.asset, "Walk"):
			creature_picker.select(0)
			return
		active_creature = "Fire Beetle"
		preview_title.text = FIRE_BEETLE.name
		preview_subtitle.text = FIRE_BEETLE.subtitle
		preview_controls.visible = false
		_set_preview_controls([], [])
		_render_stats(FIRE_BEETLE)
	elif index == 2:
		if not _load_creature(TWIG_BLIGHT.asset, "Attack_Claw_01_BackSwipe"):
			creature_picker.select(0)
			return
		active_creature = "Twig Blight"
		preview_title.text = TWIG_BLIGHT.name
		preview_subtitle.text = TWIG_BLIGHT.subtitle
		preview_controls.visible = preview_animator != null
		_set_preview_controls(["Walk", "Claw"], ["Walk", "Claw"])
		_render_stats(TWIG_BLIGHT)
	elif index == 3:
		if not _load_creature(WOLF.asset, "Idle_Standing_01_Standard"):
			creature_picker.select(0)
			return
		active_creature = "Wolf"
		preview_title.text = WOLF.name
		preview_subtitle.text = WOLF.subtitle
		preview_controls.visible = preview_animator != null
		_set_preview_controls(
			["Idle_Standing_01_Standard","Movement_Run_01_Standard",
			"Attack_Pounce_01_Standard","Action_RollOver_01_Standard"],
			["Idle","Run","Pounce","Roll"])
		_render_stats(WOLF)
	else:
		var variant: Dictionary = GOBLINS.ENTRIES[variant_index]
		if not _load_creature(variant.asset):
			creature_picker.select(1)
			return
		active_creature = "Goblin"
		preview_title.text = variant.name
		preview_subtitle.text = variant.get("credit", ENTRIES[0].subtitle)
		preview_controls.visible = preview_animator != null
		_set_preview_controls(["Walk", "Attack", "Axe Spin"],
			["Walk", "Attack", "Axe Spin"])
		_render_stats(ENTRIES[0])

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

func _load_creature(path: String, state := "Idle") -> bool:
	if not ResourceLoader.exists(path):
		return false
	var instance: Node3D = load(path).instantiate()
	# Beetle mesh AABB is ~0.00007 units while its animated rig is ~0.5.
	# Scale and frame it from the actual skeletal rest bounds, not the tiny
	# skinned mesh's un-deformed local vertex box.
	var bounds := _skeleton_bounds_for(instance, Transform3D.IDENTITY) if path == FIRE_BEETLE.asset else _bounds_for(instance, Transform3D.IDENTITY)
	if bounds.size.length() < 0.00000001 or bounds.size.y < 0.00000001:
		instance.free()
		return false
	if is_instance_valid(preview_model):
		creature_root.remove_child(preview_model)
		preview_model.queue_free()
	preview_model = instance
	creature_root.add_child(instance)
	# The wolf is much easier to review from a three-quarter angle.
	creature_root.rotation.y = 0.60 if path == WOLF.asset else 0.0
	preview_animator = _find_animator(instance) if path == FIRE_BEETLE.asset else QUADRUPED.attach(instance) if path == WOLF.asset else SHARED.attach(instance)
	_play_preview_animation(state)
	# Some imported GLBs (notably the fire beetle) use sub-centimeter scale.
	# Normalize the preview independently of world collision/physical height.
	var target_height := 0.65 if path == FIRE_BEETLE.asset else 1.05 if path == WOLF.asset else 1.4
	var preview_scale := target_height / bounds.size.y
	instance.scale *= preview_scale
	instance.position = -bounds.get_center() * preview_scale
	var diameter := maxf(maxf(bounds.size.x, bounds.size.z) * preview_scale, target_height)
	var distance := maxf(diameter * 2.3, 1.65) if path == FIRE_BEETLE.asset else diameter * 1.65
	camera.position = Vector3(0, 0, distance)
	camera.look_at(Vector3.ZERO)
	camera.near = 0.02
	camera.far = 500.0
	return true

func _skeleton_bounds_for(node: Node3D, parent_transform: Transform3D) -> AABB:
	var transform := parent_transform * node.transform
	var result := AABB()
	var found := false
	if node is Skeleton3D:
		var skeleton := node as Skeleton3D
		for index in skeleton.get_bone_count():
			var point: Vector3 = transform * skeleton.get_bone_global_rest(index).origin
			result = result.expand(point) if found else AABB(point, Vector3.ZERO)
			found = true
	for child in node.get_children():
		if child is Node3D:
			var other := _skeleton_bounds_for(child, transform)
			if other.size.length() > 0.00000001:
				result = result.merge(other) if found else other
				found = true
	return result

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
	if preview_animator == null: return
	var local_anim: AnimationPlayer = _find_animator(preview_model) if is_instance_valid(preview_model) else null
	if local_anim != null and local_anim != preview_animator: local_anim.stop()
	if state == "Axe Spin" and local_anim != null and local_anim.has_animation("Axe_Spin_Attack"):
		preview_animator.stop()
		local_anim.play("Axe_Spin_Attack")
		return
	var wanted := "Attack_Claw_01_BackSwipe" if state == "Claw" else "Walking" if state == "Walk" and preview_animator.name == "SharedHumanoidAnimations" else "Running" if state == "Run" else state
	for clip in preview_animator.get_animation_list():
		if clip.to_lower().contains(wanted.to_lower()):
			var animation := preview_animator.get_animation(clip)
			var is_loop := state in ["Walk", "Run", "Idle", "Walking"] or state.begins_with("Movement_") or state.begins_with("Idle_")
			animation.loop_mode = Animation.LOOP_LINEAR if is_loop else Animation.LOOP_NONE
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
