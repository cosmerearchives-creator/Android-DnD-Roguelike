extends SceneTree
func _initialize() -> void:
	call_deferred("_run")
func _fail(msg: String) -> void:
	printerr("DISPLAY_CAMERA_PERSISTENCE_FAILED "+msg)
	quit(1)
func _run() -> void:
	var settings_node := root.get_node_or_null("DesktopFullscreen")
	if settings_node==null:
		_fail("Autoload unavailable")
		return
	var old_mode: int = settings_node.saved_mode
	var old_size: Vector2i = settings_node.saved_size
	var old_pos: Vector2i = settings_node.saved_position
	var old_has: bool = settings_node.window_preference_exists
	var old_first: bool = settings_node.camera_first_person
	var old_angle: float = settings_node.camera_angle
	var old_ortho: float = settings_node.camera_size
	var old_dist: float = settings_node.camera_distance
	var old_fov: float = settings_node.camera_fov
	settings_node.save_camera(true,75.0,12.0,7.5,68.0)
	var config := ConfigFile.new()
	if config.load(settings_node.CONFIG)!=OK:
		_fail("Config file not created")
		return
	if not bool(config.get_value("camera","first_person",false)) or absf(float(config.get_value("camera","distance",0))-7.5)>.01:
		_fail("Camera preferences not saved")
		return
	settings_node.saved_mode = Window.MODE_FULLSCREEN
	settings_node.saved_size = Vector2i(1270,730)
	settings_node.saved_position = Vector2i(40,40)
	settings_node.window_preference_exists = true
	settings_node._save()
	if config.load(settings_node.CONFIG)!=OK or int(config.get_value("window","mode",0))!=Window.MODE_FULLSCREEN or config.get_value("window","size",Vector2i.ZERO)!=Vector2i(1270,730):
		_fail("Window state not written")
		return
	settings_node.saved_mode=old_mode
	settings_node.saved_size=old_size
	settings_node.saved_position=old_pos
	settings_node.window_preference_exists=old_has
	settings_node.save_camera(old_first,old_angle,old_ortho,old_dist,old_fov)
	print("DISPLAY_CAMERA_PERSISTENCE_SMOKE_OK saved_window_and_camera")
	quit(0)
