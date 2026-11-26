extends VBoxContainer

const REPO := "https://github.com/cosmerearchives-creator/Android-DnD-Roguelike"
const API := "https://api.github.com/repos/cosmerearchives-creator/Android-DnD-Roguelike/releases/latest"
const HEADERS := ["Accept: application/vnd.github+json", "Cache-Control: no-cache"]
var status := Label.new()
var check := Button.new()
var download := Button.new()
var request := HTTPRequest.new()
var download_url := ""
var release_tag := ""
var installed_code := 0
var fetching_manifest := false

func _ready() -> void:
	add_child(status)
	check.text = "Check for update"
	check.pressed.connect(_check)
	add_child(check)
	download.hide()
	download.pressed.connect(func(): OS.shell_open(download_url))
	add_child(download)
	var history := Button.new()
	history.text = "Previous releases / rollback"
	history.pressed.connect(_explain_rollback)
	add_child(history)
	request.timeout = 15
	request.body_size_limit = 1024 * 1024
	request.request_completed.connect(_received)
	add_child(request)
	if FileAccess.file_exists("res://release-version.json"):
		var build: Variant = JSON.parse_string(FileAccess.get_file_as_string("res://release-version.json"))
		if build is Dictionary:
			installed_code = int(build.get("versionCode", 0))
	if OS.has_feature("android") and installed_code > 0:
		_check()

func _check() -> void:
	check.disabled = true
	download.hide()
	status.text = "Checking for updates…"
	fetching_manifest = false
	if request.request(API, HEADERS) != OK:
		_finish("Couldn't start the update check.")

func _finish(message: String) -> void:
	status.text = message
	check.disabled = false

func _received(result: int, response: int, _headers: PackedStringArray, body: PackedByteArray) -> void:
	var data: Variant = JSON.parse_string(body.get_string_from_utf8()) if result == HTTPRequest.RESULT_SUCCESS and response == 200 else null
	if not data is Dictionary:
		_finish("Couldn't check for updates.")
		return
	if fetching_manifest:
		var code := int(data.get("versionCode", 0))
		if data.get("versionName") != release_tag.substr(1) or data.get("apkUrl") != download_url or code <= 0:
			_finish("Release information could not be verified.")
		elif code > installed_code:
			download.text = "Download " + release_tag
			download.show()
			_finish("Update available")
		else:
			_finish("You're up to date.")
		return
	release_tag = str(data.get("tag_name", ""))
	var format := RegEx.new()
	format.compile("^v[0-9]+\\.[0-9]+\\.[0-9]+$")
	if data.get("draft", true) or data.get("prerelease", true) or format.search(release_tag) == null:
		_finish("No stable update available.")
		return
	var prefix := REPO + "/releases/download/" + release_tag + "/"
	download_url = prefix + "Android-DnD-Roguelike.apk"
	var manifest := prefix + "update.json"
	var listed_apk := false
	var listed_manifest := false
	for asset in data.get("assets", []):
		if asset is Dictionary:
			var url: String = asset.get("browser_download_url", "")
			listed_apk = listed_apk or url == download_url
			listed_manifest = listed_manifest or url == manifest
	if not listed_apk or not listed_manifest:
		_finish("Release files are missing.")
		return
	fetching_manifest = true
	if request.request(manifest) != OK:
		_finish("Couldn't read release information.")

func _explain_rollback() -> void:
	var dialog := ConfirmationDialog.new()
	dialog.title = "Previous releases"
	dialog.dialog_text = "Android blocks installation of a lower version over this build.\nUninstalling to install an older APK can erase your saves.\nOpen the official release history?"
	dialog.confirmed.connect(func(): OS.shell_open(REPO + "/releases"))
	dialog.confirmed.connect(dialog.queue_free)
	dialog.canceled.connect(dialog.queue_free)
	add_child(dialog)
	dialog.popup_centered()
