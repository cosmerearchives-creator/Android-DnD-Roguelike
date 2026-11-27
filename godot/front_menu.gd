extends Control
var content: VBoxContainer
var font: Font = preload("res://hud_serif.ttf")
var chosen := 0
const MODELS = ["human_adventurer", "male_human"]
const NAMES = ["Human Adventurer", "Male Human"]
var profile := ConfigFile.new()
const WORLD_SCENE := "res://world.tscn"
var world_prefetch_active := false
var awaiting_world := false
func _ready():
	preload("res://platform_layout.gd").configure_window()
	var bg := TextureRect.new()
	bg.texture = preload("res://menu_camp.webp")
	bg.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	add_child(bg)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	var center := CenterContainer.new()
	add_child(center)
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	content = VBoxContainer.new()
	content.custom_minimum_size.x = 530
	content.add_theme_constant_override("separation",18)
	center.add_child(content)
	var theme_ui := Theme.new()
	theme_ui.default_font = font
	theme_ui.default_font_size = 32
	content.theme = theme_ui
	profile.load("user://hero.cfg")
	main_menu()
	# Prepare the heavy world scene in the background while the user is in
	# the menu instead of blocking the first Continue/New Game transition.
	if not OS.has_feature("headless"):
		call_deferred("_begin_world_prefetch")
func clear():
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
	b.custom_minimum_size.y = 76
	var style := StyleBoxFlat.new()
	style.bg_color = Color("#0b292e")
	style.border_color = Color("#cfb576")
	style.set_border_width_all(2)
	style.set_corner_radius_all(12)
	b.add_theme_stylebox_override("normal",style)
	b.add_theme_color_override("font_color",Color("#f3dfb8"))
	content.add_child(b)
	b.pressed.connect(callback)
func main_menu():
	clear()
	heading("FORESTS OF HELL",52)
	heading("An adventure awaits",30)
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
	content.add_child(preload("res://release_updates.gd").new())
	button("Back",main_menu)
func creator():
	clear()
	heading("Create your character",42)
	var name_field := LineEdit.new()
	name_field.placeholder_text = "Character name"
	content.add_child(name_field)
	var models := OptionButton.new()
	for n in NAMES: models.add_item(n)
	models.select(chosen)
	models.item_selected.connect(func(i): chosen = i)
	content.add_child(models)
	heading("Appearance selection",28)
	button("Begin adventure",func():
		profile.set_value("hero","name",name_field.text.strip_edges() if not name_field.text.strip_edges().is_empty() else "Avatar")
		profile.set_value("hero","model",chosen)
		profile.save("user://hero.cfg")
		enter_world())
	button("Back",main_menu)
func _begin_world_prefetch() -> void:
	if not is_inside_tree(): return
	world_prefetch_active = ResourceLoader.load_threaded_request(WORLD_SCENE,"PackedScene",true) == OK

func _process(_delta: float) -> void:
	if awaiting_world and ResourceLoader.load_threaded_get_status(WORLD_SCENE) != ResourceLoader.THREAD_LOAD_IN_PROGRESS:
		awaiting_world = false
		_open_prepared_world()

func _open_prepared_world() -> void:
	if world_prefetch_active and ResourceLoader.load_threaded_get_status(WORLD_SCENE) == ResourceLoader.THREAD_LOAD_LOADED:
		var packed := ResourceLoader.load_threaded_get(WORLD_SCENE) as PackedScene
		if packed != null:
			get_tree().change_scene_to_packed(packed)
			return
	# Graceful fallback for unavailable platform threading.
	get_tree().change_scene_to_file(WORLD_SCENE)

func enter_world():
	if world_prefetch_active and ResourceLoader.load_threaded_get_status(WORLD_SCENE) == ResourceLoader.THREAD_LOAD_IN_PROGRESS:
		awaiting_world = true
		clear()
		heading("Preparing the forest...",40)
		return
	_open_prepared_world()
