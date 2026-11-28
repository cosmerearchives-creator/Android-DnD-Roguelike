extends Control
var content: VBoxContainer
var font: Font = preload("res://hud_serif.ttf")
var chosen := 0
const WEARABLE = preload("res://player_equipment_visual.gd")
const MODELS = WEARABLE.MODELS
const NAMES = WEARABLE.NAMES
var profile := ConfigFile.new()
const WORLD_SCENE := "res://world.tscn"
const APPEARANCE = preload("res://character_appearance.gd")
var edited_appearance: Dictionary = {}
var world_prefetch_active := false
var awaiting_world := false
var menu_scroll: ScrollContainer
var preview_view: SubViewportContainer
var preview_model: Node3D
var preview_root: Node3D
var face_zoom := false
var preview_camera: Camera3D
var desktop_creator := false
var preview_base_scale := 1.0
var preview_bottom := 0.0
func _ready():
	preload("res://platform_layout.gd").configure_window()
	custom_minimum_size = Vector2.ZERO
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	clip_contents = true
	var bg := TextureRect.new()
	bg.texture = preload("res://menu_camp.webp")
	bg.name = "MenuBackground"
	bg.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(bg)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	bg.custom_minimum_size = Vector2.ZERO
	var center := CenterContainer.new()
	center.name = "MenuControls"
	add_child(center)
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	center.custom_minimum_size = Vector2.ZERO
	center.mouse_filter = Control.MOUSE_FILTER_PASS
	if not OS.has_feature("mobile"):
		bg.texture = preload("res://menu_landscape.png")
		bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		center.anchor_right = 0.56
	var scroll := ScrollContainer.new()
	menu_scroll=scroll
	scroll.custom_minimum_size = Vector2(560, 680)
	scroll.size_flags_vertical = Control.SIZE_SHRINK_CENTER
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	center.add_child(scroll)
	content = VBoxContainer.new()
	content.custom_minimum_size.x = 530
	content.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	content.add_theme_constant_override("separation",18)
	if not OS.has_feature("mobile"):
		scroll.custom_minimum_size=Vector2(520, 680)
		content.custom_minimum_size.x=490
	scroll.add_child(content)
	var theme_ui := Theme.new()
	theme_ui.default_font = font
	theme_ui.default_font_size = 32
	content.theme = theme_ui
	profile.load("user://hero.cfg")
	main_menu()
	# Prepare the heavy world scene in the background while the user is in
	# the menu instead of blocking the first Continue/New Game transition.
	if DisplayServer.get_name() != "headless":
		call_deferred("_begin_world_prefetch")
func clear():
	_remove_preview()
	if menu_scroll != null:
		menu_scroll.custom_minimum_size=Vector2(520,680)
		menu_scroll.position=Vector2.ZERO
	for child in content.get_children():
		content.remove_child(child)
		child.queue_free()
func heading(value: String, px: int):
	var l := Label.new()
	l.text = value
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.add_theme_font_size_override("font_size",px)
	l.add_theme_color_override("font_color",Color("#f3dfb8"))
	content.add_child(l)
func button(value: String, callback: Callable):
	var b := Button.new()
	b.text = value
	b.custom_minimum_size.y = 66 if not OS.has_feature("mobile") else 76
	var style := StyleBoxFlat.new()
	style.bg_color = Color("#182124e0")
	style.border_color = Color("#cfb576")
	style.set_border_width_all(2)
	style.set_corner_radius_all(12)
	b.add_theme_stylebox_override("normal",style)
	b.add_theme_color_override("font_color",Color("#f3dfb8"))
	content.add_child(b)
	b.pressed.connect(callback)
func main_menu():
	clear()
	if not OS.has_feature("mobile"): menu_scroll.get_parent().anchor_right = 0.56
	if not OS.has_feature("mobile"):
		menu_scroll.custom_minimum_size=Vector2(520,680)
	heading("FORESTS OF HELL",52)
	if OS.has_feature("mobile"): heading("An adventure awaits",30)
	if profile.has_section("hero"):
		button("Continue",enter_world)
	button("New Game",creator)
	button("Bestiary",func(): get_tree().change_scene_to_file("res://bestiary.tscn"))
	button("Animation Lab",func(): get_tree().change_scene_to_file("res://humanoid_animation_lab.tscn"))
	button("Quadruped Animation Lab",func(): get_tree().change_scene_to_file("res://quadruped_animation_lab.tscn"))
	button("Settings & Updates",settings)
func settings():
	clear()
	heading("Settings",44)
	if not OS.has_feature("mobile") and not OS.has_feature("web"):
		content.add_child(preload("res://pc_release_updates.gd").new())
	else:
		content.add_child(preload("res://release_updates.gd").new())
	button("Back",main_menu)
func creator():
	clear()
	# The approved workshop is a dedicated scene; keep the original
	# menu and its world prefetch behavior intact.
	get_tree().change_scene_to_file("res://creator_workshop.tscn")

func _remove_preview() -> void:
	if is_instance_valid(preview_view):
		var layout:=preview_view.get_parent()
		if layout is HBoxContainer:
			layout.remove_child(menu_scroll)
			layout.get_parent().add_child(menu_scroll)
			layout.get_parent().remove_child(layout)
			layout.queue_free()
		else:
			preview_view.get_parent().remove_child(preview_view)
			preview_view.queue_free()
	preview_view=null
	preview_root=null
	preview_model=null
	preview_camera=null

func _set_axis_sliders(kind: String, value: Vector3) -> void:
	for row in content.get_children():
		if row is HBoxContainer:
			for item in row.get_children():
				if item is HSlider and str(item.get_meta("appearance_kind","")) == kind:
					item.value=value[int(item.get_meta("appearance_axis",0))]

func _create_preview() -> void:
	# Give the desktop creator independent columns, keeping every control usable.
	if menu_scroll == null:return
	menu_scroll.custom_minimum_size=Vector2(570,680)
	var center:Control=menu_scroll.get_parent()
	center.anchor_right=1.0
	# A CenterContainer owns child positions. Use a dedicated two-column
	# horizontal layout and let it place both controls and preview.
	var layout:=HBoxContainer.new()
	layout.name="CreatorColumns"
	layout.add_theme_constant_override("separation",32)
	layout.custom_minimum_size=Vector2(1050,680)
	center.remove_child(menu_scroll)
	center.add_child(layout)
	layout.add_child(menu_scroll)
	menu_scroll.size_flags_horizontal=Control.SIZE_FILL
	menu_scroll.size_flags_vertical=Control.SIZE_EXPAND_FILL
	preview_view=SubViewportContainer.new()
	preview_view.name="CreatorModelPreview"
	preview_view.custom_minimum_size=Vector2(420,640)
	preview_view.size=Vector2(420,640)
	preview_view.stretch=true
	preview_view.size_flags_vertical=Control.SIZE_SHRINK_CENTER
	preview_view.mouse_filter=Control.MOUSE_FILTER_STOP
	layout.add_child(preview_view)
	var viewport:=SubViewport.new()
	viewport.size=Vector2i(420,640)
	viewport.transparent_bg=true
	viewport.own_world_3d=true
	viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS
	preview_view.add_child(viewport)
	preview_root=Node3D.new()
	viewport.add_child(preview_root)
	var sun:=DirectionalLight3D.new()
	sun.rotation_degrees=Vector3(-35,30,0)
	sun.light_energy=1.75
	preview_root.add_child(sun)
	var fill:=DirectionalLight3D.new()
	fill.rotation_degrees=Vector3(-20,-130,0)
	fill.light_energy=0.75
	preview_root.add_child(fill)
	var env:=WorldEnvironment.new()
	env.environment=Environment.new()
	env.environment.background_mode=Environment.BG_COLOR
	env.environment.background_color=Color(0.025,0.07,0.075,1)
	env.environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
	env.environment.ambient_light_color=Color(0.78,0.78,0.75)
	env.environment.ambient_light_energy=0.65
	preview_root.add_child(env)
	preview_camera=Camera3D.new()
	preview_camera.projection=Camera3D.PROJECTION_ORTHOGONAL
	preview_camera.size=2.2
	preview_camera.position=Vector3(0,1.0,4.5)
	preview_root.add_child(preview_camera)
	preview_camera.current=true
	preview_camera.look_at(Vector3(0,1.0,0))
	preview_view.gui_input.connect(func(event:InputEvent):
		if event is InputEventMouseButton and event.pressed:
			if event.button_index==MOUSE_BUTTON_WHEEL_UP: preview_camera.size=clampf(preview_camera.size*0.9,0.35,4.0)
			if event.button_index==MOUSE_BUTTON_WHEEL_DOWN: preview_camera.size=clampf(preview_camera.size/0.9,0.35,4.0)
		if event is InputEventMouseMotion:
			if Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT) and is_instance_valid(preview_model):
				preview_model.rotation.y += event.relative.x*0.012
			if Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT) or Input.is_mouse_button_pressed(MOUSE_BUTTON_MIDDLE):
				preview_camera.position.x-=event.relative.x*preview_camera.size/640.0
				preview_camera.position.y+=event.relative.y*preview_camera.size/640.0)
	_refresh_preview()

func _preview_bounds(node:Node3D, base:Transform3D) -> Vector2:
	var t:=base*node.transform
	var bottom:=INF
	var top:=-INF
	if node is MeshInstance3D and node.mesh!=null:
		var bounds:AABB=node.get_aabb()
		for i in range(8):
			var y:float=(t*bounds.get_endpoint(i)).y
			bottom=minf(bottom,y)
			top=maxf(top,y)
	for child in node.get_children():
		if child is Node3D:
			var sub:=_preview_bounds(child,t)
			bottom=minf(bottom,sub.x)
			top=maxf(top,sub.y)
	return Vector2(bottom,top)

func _refresh_preview() -> void:
	if not is_instance_valid(preview_root):return
	if is_instance_valid(preview_model):
		preview_root.remove_child(preview_model)
		preview_model.queue_free()
	var path:=WEARABLE.asset(MODELS[chosen])
	if not ResourceLoader.exists(path):return
	var packed:=load(path) as PackedScene
	if packed==null:return
	preview_model=packed.instantiate() as Node3D
	preview_root.add_child(preview_model)
	WEARABLE.apply(preview_model,false)
	var bounds:=_preview_bounds(preview_model,Transform3D.IDENTITY)
	if is_finite(bounds.x) and is_finite(bounds.y) and bounds.y>bounds.x:
		preview_base_scale=1.85/(bounds.y-bounds.x)
		preview_bottom=bounds.x
		_update_preview_scale()

	_update_appearance()

func _update_preview_scale() -> void:
	if not is_instance_valid(preview_model):return
	preview_model.scale=APPEARANCE.body_scale(edited_appearance)*preview_base_scale
	preview_model.position.y=-preview_bottom*preview_model.scale.y

func _update_appearance() -> void:
	if is_instance_valid(preview_model):
		APPEARANCE.apply(preview_model,edited_appearance)

func _frame_preview() -> void:
	if not is_instance_valid(preview_camera):return
	preview_camera.size=0.68 if face_zoom else 2.2
	preview_camera.position.y=1.66*float(edited_appearance.get("height_scale",1.0)) if face_zoom else 1.0
	if not face_zoom: preview_camera.size=2.2*maxf(1.0,float(edited_appearance.get("height_scale",1.0)))

func _begin_world_prefetch() -> void:
	if not is_inside_tree(): return
	ForestLoading.prefetch_assets()
	if not world_prefetch_active:
		world_prefetch_active = ResourceLoader.load_threaded_request(WORLD_SCENE, "PackedScene", true) == OK

func _process(_delta: float) -> void:
	if not awaiting_world: return
	var scene_progress: Array = []
	var scene_status := ResourceLoader.THREAD_LOAD_INVALID_RESOURCE
	if world_prefetch_active:
		scene_status = ResourceLoader.load_threaded_get_status(WORLD_SCENE, scene_progress)
	var scene_fraction := float(scene_progress[0]) if not scene_progress.is_empty() else (1.0 if scene_status == ResourceLoader.THREAD_LOAD_LOADED else 0.0)
	var asset_fraction: float = ForestLoading.assets_progress()
	ForestLoading.report(0.5 * (0.65 * scene_fraction + 0.35 * asset_fraction),
		"Loading forest resources... %d%%" % int(100.0 * (0.65 * scene_fraction + 0.35 * asset_fraction)))
	if scene_status == ResourceLoader.THREAD_LOAD_FAILED or scene_status == ResourceLoader.THREAD_LOAD_INVALID_RESOURCE:
		world_prefetch_active = false
	if (scene_status == ResourceLoader.THREAD_LOAD_LOADED or not world_prefetch_active) and ForestLoading.all_assets_ready():
		awaiting_world = false
		call_deferred("_open_prepared_world")

func _open_prepared_world() -> void:
	# Draw the persistent overlay before creating the potentially heavy world.
	await get_tree().process_frame
	ForestLoading.report(0.5, "Creating the forest and preparing scenery...")
	if world_prefetch_active and ResourceLoader.load_threaded_get_status(WORLD_SCENE) == ResourceLoader.THREAD_LOAD_LOADED:
		var packed := ResourceLoader.load_threaded_get(WORLD_SCENE) as PackedScene
		if packed != null:
			get_tree().change_scene_to_packed(packed)
			return
	# Fallback when threading fails, keeping the loading overlay visible.
	get_tree().change_scene_to_file(WORLD_SCENE)

func enter_world():
	ForestLoading.begin()
	if not world_prefetch_active:
		_begin_world_prefetch()
	awaiting_world = true

