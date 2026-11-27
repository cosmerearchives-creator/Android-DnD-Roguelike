extends RefCounted
const PC_WIDTH := 1600
const PC_HEIGHT := 900
static func is_pc() -> bool:
	if OS.get_environment("FOH_TEST_PC") == "1": return true
	return not OS.has_feature("mobile") and not OS.has_feature("web") and DisplayServer.get_name() != "headless"

static func configure_window() -> void:
	if not is_pc() or DisplayServer.get_name() == "headless": return
	var window := Engine.get_main_loop().root as Window
	if window == null: return
	var preferences := window.get_node_or_null("/root/DesktopFullscreen")
	if preferences != null and preferences.has_window_preference(): return
	if window.mode != Window.MODE_WINDOWED: return
	if window.size.x < window.size.y or window.size.x < 1200:
		window.size = Vector2i(PC_WIDTH,PC_HEIGHT)
		var screen_size := DisplayServer.screen_get_size()
		window.position = (screen_size-window.size)/2
