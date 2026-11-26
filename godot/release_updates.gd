extends VBoxContainer

const REPO := "https://github.com/cosmerearchives-creator/Android-DnD-Roguelike"
const LATEST_MANIFEST := REPO + "/releases/latest/download/update.json"
const HEADERS := ["Accept: application/vnd.github+json", "Cache-Control: no-cache", "User-Agent: ForestsOfHell-Android-Updater"]
var status := Label.new()
var check := Button.new()
var download := Button.new()
var request := HTTPRequest.new()
var download_url := ""
var release_tag := ""
var installed_code := 0
var transfer := HTTPRequest.new()
var expected_hash := ""
var apk_path := "user://update.apk"
var verified := false
var poll := Timer.new()

func _ready() -> void:
	add_child(status)
	check.text = "Check for update"
	check.pressed.connect(_check)
	add_child(check)
	download.hide()
	download.pressed.connect(_install)
	add_child(download)
	var history := Button.new()
	history.text = "Previous releases / rollback"
	history.pressed.connect(_explain_rollback)
	add_child(history)
	request.timeout = 15
	request.body_size_limit = 1024 * 1024
	request.request_completed.connect(_received)
	add_child(request)
	transfer.use_threads = true
	transfer.timeout = 600
	transfer.request_completed.connect(_downloaded)
	add_child(transfer)
	poll.wait_time = 300
	poll.timeout.connect(func():
		if not check.disabled and not verified: _check())
	add_child(poll)
	if FileAccess.file_exists("res://release-version.json"):
		var build: Variant = JSON.parse_string(FileAccess.get_file_as_string("res://release-version.json"))
		if build is Dictionary:
			installed_code = int(build.get("versionCode", 0))
	if OS.has_feature("android") and installed_code > 0:
		_check()
		poll.start()

func _check() -> void:
	verified = false
	check.disabled = true
	download.hide()
	status.text = "Checking for updates…"
	if request.request(LATEST_MANIFEST, HEADERS) != OK:
		_finish("Couldn't start the update check.")

func _finish(message: String) -> void:
	status.text = message
	check.disabled = false

func _received(result: int, response: int, _headers: PackedStringArray, body: PackedByteArray) -> void:
	var data: Variant = JSON.parse_string(body.get_string_from_utf8()) if result == HTTPRequest.RESULT_SUCCESS and response == 200 else null
	if not data is Dictionary:
		if response == 403 or response == 429:
			_finish("GitHub download temporarily limited; retry later.")
		elif response > 0:
			_finish("Update server returned HTTP %d." % response)
		else:
			_finish("Network unavailable. Retry when connected.")
		return
	var version_name := str(data.get("versionName", ""))
	var format := RegEx.new()
	format.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$")
	var code := int(data.get("versionCode", 0))
	var next_url := REPO + "/releases/download/v" + version_name + "/Android-DnD-Roguelike.apk"
	var checksum := str(data.get("sha256", "")).to_lower()
	if format.search(version_name) == null or code <= 0 or str(data.get("apkUrl", "")) != next_url or checksum.length() != 64:
		_finish("Latest release metadata failed verification.")
		return
	release_tag = "v" + version_name
	download_url = next_url
	expected_hash = checksum
	if code <= installed_code:
		_finish("You're up to date.")
		return
	if FileAccess.file_exists(apk_path) and FileAccess.get_sha256(apk_path) == expected_hash:
		_ready_to_install()
		return
	verified = false
	download.hide()
	status.text = "Downloading " + release_tag + "…"
	transfer.download_file = apk_path
	if transfer.request(download_url) != OK:
		_finish("Couldn't start update download.")

func _downloaded(result: int, response: int, _headers: PackedStringArray, _body: PackedByteArray) -> void:
	if result != HTTPRequest.RESULT_SUCCESS or response != 200 or FileAccess.get_sha256(apk_path) != expected_hash:
		DirAccess.remove_absolute(ProjectSettings.globalize_path(apk_path))
		_finish("Update download failed verification. Retry check.")
		return
	_ready_to_install()

func _ready_to_install() -> void:
	verified = true
	download.text = "Install " + release_tag
	download.show()
	_finish("Update ready to install")

func _install() -> void:
	if not verified: return
	if FileAccess.get_sha256(apk_path) != expected_hash:
		_finish("Update checksum changed. Check again.")
		return
	if not Engine.has_singleton("AndroidRuntime"):
		OS.shell_open(download_url)
		return
	var runtime = Engine.get_singleton("AndroidRuntime")
	var activity = runtime.getActivity()
	if activity == null:
		_finish("Android installer unavailable.")
		return
	var intent_class = JavaClassWrapper.wrap("android.content.Intent")
	var uri_class = JavaClassWrapper.wrap("android.net.Uri")
	var package_name: String = activity.getPackageName()
	if not activity.getPackageManager().canRequestPackageInstalls():
		var settings = intent_class.new("android.settings.MANAGE_UNKNOWN_APP_SOURCES", uri_class.parse("package:" + package_name))
		activity.startActivity(settings)
		_finish("Allow installs from this app, then tap Install again.")
		return
	# The actual user:// location includes Godot's app_userdata subdirectory.
	# FileProvider must calculate the content URI from the real file path.
	var provider = JavaClassWrapper.wrap("androidx.core.content.FileProvider")
	var file_class = JavaClassWrapper.wrap("java.io.File")
	var file = file_class.new(ProjectSettings.globalize_path(apk_path))
	var uri = provider.getUriForFile(activity, package_name + ".fileprovider", file)
	if uri == null:
		_finish("Could not share APK with Android installer.")
		return
	var intent = intent_class.new("android.intent.action.INSTALL_PACKAGE")
	intent.setDataAndType(uri, "application/vnd.android.package-archive")
	intent.addFlags(1)
	intent.addFlags(268435456)
	_finish("Opening Android installer…")
	activity.startActivity(intent)

func _explain_rollback() -> void:
	var dialog := ConfirmationDialog.new()
	dialog.title = "Previous releases"
	dialog.dialog_text = "Android blocks installation of a lower version over this build.\nUninstalling to install an older APK can erase your saves.\nOpen the official release history?"
	dialog.confirmed.connect(func(): OS.shell_open(REPO + "/releases"))
	dialog.confirmed.connect(dialog.queue_free)
	dialog.canceled.connect(dialog.queue_free)
	add_child(dialog)
	dialog.popup_centered()
