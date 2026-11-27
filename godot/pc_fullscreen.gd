extends Node
# Global hotkey: independent of game scene and active menus.
var saved_size := Vector2i(1600, 900)
var saved_position := Vector2i.ZERO
var saved_mode := Window.MODE_WINDOWED

func _input(event: InputEvent) -> void:
	if OS.has_feature("mobile") or OS.has_feature("web") or DisplayServer.get_name() == "headless":
		return
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_F11:
		var window := get_window()
		if window.mode != Window.MODE_FULLSCREEN and window.mode != Window.MODE_EXCLUSIVE_FULLSCREEN:
			saved_mode = window.mode
			saved_size = window.size
			saved_position = window.position
			window.mode = Window.MODE_FULLSCREEN
		else:
			window.mode = saved_mode
			if saved_mode == Window.MODE_WINDOWED:
				window.size = saved_size
				window.position = saved_position
		get_viewport().set_input_as_handled()
