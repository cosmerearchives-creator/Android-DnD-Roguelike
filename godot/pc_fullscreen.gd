extends Node
## Cross-scene Windows display and gameplay-camera preferences.
const CONFIG := "user://display_camera.cfg"
var saved_size := Vector2i(1600, 900)
var saved_position := Vector2i.ZERO
var saved_mode := Window.MODE_WINDOWED
var window_preference_exists := false
var camera_first_person := false
var camera_angle := 60.0
var camera_size := 15.0
var camera_distance := 5.0
var camera_fov := 72.0
var _last_window_state := ""
var _save_delay := 0.0

func _is_desktop() -> bool:
	return not OS.has_feature("mobile") and not OS.has_feature("web") and DisplayServer.get_name() != "headless"

func has_window_preference() -> bool:
	return window_preference_exists

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	var settings := ConfigFile.new()
	if settings.load(CONFIG) == OK:
		window_preference_exists = bool(settings.get_value("window", "saved", false))
		saved_size = settings.get_value("window", "size", saved_size)
		saved_position = settings.get_value("window", "position", saved_position)
		saved_mode = int(settings.get_value("window", "mode", Window.MODE_WINDOWED))
		camera_first_person = bool(settings.get_value("camera", "first_person", false))
		camera_angle = clampf(float(settings.get_value("camera", "angle", 60.0)), 30.0, 85.0)
		camera_size = clampf(float(settings.get_value("camera", "size", 15.0)), 5.0, 30.0)
		camera_distance = clampf(float(settings.get_value("camera", "distance", 5.0)), 2.0, 15.0)
		camera_fov = clampf(float(settings.get_value("camera", "fov", 72.0)), 55.0, 95.0)
	if not _is_desktop(): return
	var window := get_window()
	if window_preference_exists:
		window.mode = Window.MODE_WINDOWED
		window.size = Vector2i(maxi(640,saved_size.x), maxi(480,saved_size.y))
		# Avoid restoring a window entirely outside a newly connected monitor.
		var bounds := DisplayServer.screen_get_size()
		window.position = Vector2i(clampi(saved_position.x, -window.size.x/2, maxi(0,bounds.x-100)),
			clampi(saved_position.y, -window.size.y/2, maxi(0,bounds.y-100)))
		if saved_mode in [Window.MODE_FULLSCREEN, Window.MODE_EXCLUSIVE_FULLSCREEN]:
			window.mode = saved_mode
	_last_window_state = _window_state()
	set_process(true)

func _window_state() -> String:
	var w := get_window()
	return "%d:%d:%d:%d:%d" % [w.mode,w.size.x,w.size.y,w.position.x,w.position.y]

func _process(delta: float) -> void:
	if not _is_desktop(): return
	var current := _window_state()
	if current != _last_window_state:
		_last_window_state = current
		_save_delay = 0.45
	if _save_delay > 0.0:
		_save_delay -= delta
		if _save_delay <= 0.0: _record_window()

func _record_window() -> void:
	var w := get_window()
	window_preference_exists = true
	if w.mode == Window.MODE_WINDOWED:
		saved_size = w.size
		saved_position = w.position
		saved_mode = Window.MODE_WINDOWED
	elif w.mode in [Window.MODE_FULLSCREEN, Window.MODE_EXCLUSIVE_FULLSCREEN]:
		saved_mode = w.mode
	_save()

func save_camera(first_person: bool, angle: float, size_value: float, distance: float, fov: float) -> void:
	camera_first_person = first_person
	camera_angle = angle
	camera_size = size_value
	camera_distance = distance
	camera_fov = fov
	_save()

func _save() -> void:
	var settings := ConfigFile.new()
	settings.set_value("window","saved",window_preference_exists)
	settings.set_value("window","mode",saved_mode)
	settings.set_value("window","size",saved_size)
	settings.set_value("window","position",saved_position)
	settings.set_value("camera","first_person",camera_first_person)
	settings.set_value("camera","angle",camera_angle)
	settings.set_value("camera","size",camera_size)
	settings.set_value("camera","distance",camera_distance)
	settings.set_value("camera","fov",camera_fov)
	settings.save(CONFIG)

func _input(event: InputEvent) -> void:
	if not _is_desktop(): return
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_F11:
		var window := get_window()
		if window.mode not in [Window.MODE_FULLSCREEN, Window.MODE_EXCLUSIVE_FULLSCREEN]:
			saved_size = window.size
			saved_position = window.position
			window.mode = Window.MODE_FULLSCREEN
		else:
			window.mode = Window.MODE_WINDOWED
			window.size = saved_size
			window.position = saved_position
		_record_window()
		_last_window_state = _window_state()
		_save_delay = 0.0
		get_viewport().set_input_as_handled()
