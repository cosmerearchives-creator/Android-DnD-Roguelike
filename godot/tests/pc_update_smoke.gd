extends SceneTree

func _initialize() -> void:
	call_deferred("_run")

func _fail(reason: String) -> void:
	printerr("PC_UPDATE_SMOKE_FAILED: " + reason)
	quit(1)

func _run() -> void:
	var fullscreen := root.get_node_or_null("DesktopFullscreen")
	var loading := root.get_node_or_null("ForestLoading")
	if fullscreen == null or loading == null:
		_fail("Global F11 and loading overlay autoloads must exist")
		return
	loading.begin()
	loading.report(0.75, "Growing scenery...")
	if not loading.visible or absf(loading.bar.value - 75.0) > 0.001:
		_fail("Forest loading progress bar did not show actual progress")
		return
	loading.finish()
	if loading.visible:
		_fail("Forest loading overlay must close when ready")
		return
	var update_script = load("res://pc_release_updates.gd")
	if update_script == null:
		_fail("Windows updater script could not be loaded")
		return
	var updater: VBoxContainer = update_script.new()
	if not updater.has_method("_installer_script"):
		_fail("Windows automatic installer is missing")
		return
	var installer := str(updater._installer_script())
	if not installer.contains("Wait-Process") or not installer.contains("Expand-Archive") or not installer.contains("update-backup"):
		_fail("Windows installer should wait, extract, and keep rollback backups")
		return
	updater.free()
	print("PC_UPDATE_AND_LOADING_SMOKE_OK")
	quit(0)
