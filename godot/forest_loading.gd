extends CanvasLayer
# Remains visible across scene transitions; holds imported scenery resources.
const ASSETS := ["res://assets/pine.glb", "res://assets/broadleaf.glb", "res://assets/boulder.glb"]
var cache: Dictionary = {}
var requested: Dictionary = {}
var background := ColorRect.new()
var title := Label.new()
var subtitle := Label.new()
var bar := ProgressBar.new()
var panel := CenterContainer.new()

func _ready() -> void:
	layer = 100
	process_mode = Node.PROCESS_MODE_ALWAYS
	background.color = Color(0.035, 0.065, 0.065, 1.0)
	background.mouse_filter = Control.MOUSE_FILTER_STOP
	add_child(background)
	background.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(panel)
	panel.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	var vertical := VBoxContainer.new()
	vertical.custom_minimum_size.x = 460.0
	vertical.add_theme_constant_override("separation", 20)
	panel.add_child(vertical)
	title.text = "FORESTS OF HELL"
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.add_theme_font_size_override("font_size", 42)
	vertical.add_child(title)
	subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	subtitle.text = "Preparing the forest..."
	vertical.add_child(subtitle)
	bar.custom_minimum_size.y = 24
	bar.max_value = 100
	bar.value = 0
	vertical.add_child(bar)
	hide()

func begin() -> void:
	bar.value = 0
	subtitle.text = "Preparing the forest..."
	show()

func report(fraction: float, message: String) -> void:
	if not visible: return
	bar.value = clampf(fraction * 100.0, 0.0, 100.0)
	subtitle.text = message

func finish() -> void:
	bar.value = 100
	hide()

func prefetch_assets() -> void:
	for path in ASSETS:
		if cache.has(path) or requested.has(path): continue
		var result := ResourceLoader.load_threaded_request(path, "PackedScene", true)
		if result == OK:
			requested[path] = true
		else:
			push_warning("Scenery prefetch unavailable for " + path)
			cache[path] = load(path)

func assets_progress() -> float:
	var ready_count := 0.0
	for path in ASSETS:
		if cache.has(path):
			ready_count += 1.0
		elif requested.has(path):
			var progress: Array = []
			var state := ResourceLoader.load_threaded_get_status(path, progress)
			if state == ResourceLoader.THREAD_LOAD_LOADED:
				var scene := ResourceLoader.load_threaded_get(path) as PackedScene
				if scene:
					cache[path] = scene
					ready_count += 1.0
			elif state == ResourceLoader.THREAD_LOAD_FAILED or state == ResourceLoader.THREAD_LOAD_INVALID_RESOURCE:
				# Use normal synchronous loading only as an error fallback.
				cache[path] = load(path)
				ready_count += 1.0
			elif not progress.is_empty():
				ready_count += clampf(float(progress[0]), 0.0, 0.99)
		else:
			ready_count += 1.0
	return ready_count / float(ASSETS.size())

func all_assets_ready() -> bool:
	assets_progress()
	for path in ASSETS:
		if not cache.has(path): return false
	return true

func get_scenery(path: String) -> PackedScene:
	if cache.has(path): return cache[path] as PackedScene
	var scene := load(path) as PackedScene
	if scene: cache[path] = scene
	return scene
