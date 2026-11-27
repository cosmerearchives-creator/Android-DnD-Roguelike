extends VBoxContainer
# Windows releases use a separately checksummed ZIP, never the Android APK.
const REPO := "https://github.com/cosmerearchives-creator/Android-DnD-Roguelike"
const MANIFEST := REPO + "/releases/latest/download/windows-update.json"
const ZIP_NAME := "Forests-of-Hell-Windows-x64.zip"
const HEADERS := ["Accept: application/json", "Cache-Control: no-cache", "User-Agent: ForestsOfHell-Windows-Updater"]
var status := Label.new()
var check := Button.new()
var install := Button.new()
var progress := ProgressBar.new()
var poll := Timer.new()
var request := HTTPRequest.new()
var transfer := HTTPRequest.new()
var installed_code := 0
var expected_hash := ""
var release_tag := ""
var zip_url := ""
var update_verified := false
var zip_path := "user://windows-update.zip"

func _ready() -> void:
	if OS.has_feature("mobile") or OS.has_feature("web") or DisplayServer.get_name() == "headless":
		return
	status.text = "Checking Windows updates..."
	status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	add_child(status)
	check.text = "Check for PC update"
	add_child(check)
	check.pressed.connect(_check)
	progress.custom_minimum_size.y = 16
	progress.hide()
	add_child(progress)
	install.text = "Install downloaded update and restart"
	install.hide()
	add_child(install)
	install.pressed.connect(_install)
	request.timeout = 20.0
	request.body_size_limit = 1024 * 1024
	add_child(request)
	request.request_completed.connect(_received)
	transfer.use_threads = true
	transfer.timeout = 600.0
	add_child(transfer)
	transfer.request_completed.connect(_downloaded)
	poll.wait_time = 300.0
	add_child(poll)
	poll.timeout.connect(_check)
	var build: Variant = JSON.parse_string(FileAccess.get_file_as_string("res://release-version.json")) if FileAccess.file_exists("res://release-version.json") else null
	if build is Dictionary:
		installed_code = int(build.get("versionCode", 0))
	# Older/manual exports may not embed metadata: still offer manual checks.
	call_deferred("_check")
	poll.start()

func _process(_delta: float) -> void:
	if transfer.get_http_client_status() == HTTPClient.STATUS_DISCONNECTED or not progress.visible:
		return
	var total := transfer.get_body_size()
	if total > 0:
		progress.indeterminate = false
		progress.value = clampf(100.0 * float(transfer.get_downloaded_bytes()) / float(total), 0.0, 100.0)
	else:
		progress.indeterminate = true

func _check() -> void:
	if request.get_http_client_status() != HTTPClient.STATUS_DISCONNECTED:
		return
	if transfer.get_http_client_status() != HTTPClient.STATUS_DISCONNECTED:
		return
	check.disabled = true
	status.text = "Checking Windows releases..."
	if request.request(MANIFEST, HEADERS) != OK:
		_finish("Unable to start Windows update check.")

func _finish(message: String) -> void:
	status.text = message
	check.disabled = false

func _received(result: int, response: int, _headers: PackedStringArray, body: PackedByteArray) -> void:
	if result != HTTPRequest.RESULT_SUCCESS or response != 200:
		_finish("PC update check unavailable (HTTP %d). You can retry." % response)
		return
	var data: Variant = JSON.parse_string(body.get_string_from_utf8())
	if not data is Dictionary:
		_finish("Windows release manifest is invalid.")
		return
	var version_name := str(data.get("versionName", ""))
	var version_format := RegEx.new()
	version_format.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$")
	var code := int(data.get("versionCode", 0))
	var expected_url := REPO + "/releases/download/v" + version_name + "/" + ZIP_NAME
	var checksum := str(data.get("sha256", "")).to_lower()
	if version_format.search(version_name) == null or code <= 0 or checksum.length() != 64 or str(data.get("zipUrl", "")) != expected_url:
		_finish("Windows release failed metadata verification.")
		return
	if code <= installed_code:
		_finish("PC version is up to date (v" + version_name + ").")
		return
	expected_hash = checksum
	zip_url = expected_url
	release_tag = "v" + version_name
	if FileAccess.file_exists(zip_path) and FileAccess.get_sha256(zip_path) == expected_hash:
		_ready_to_install()
		return
	update_verified = false
	install.hide()
	progress.value = 0
	progress.indeterminate = true
	progress.show()
	status.text = "Downloading PC " + release_tag + "..."
	transfer.download_file = zip_path
	if transfer.request(zip_url, HEADERS) != OK:
		progress.hide()
		_finish("Unable to download Windows update.")

func _downloaded(result: int, response: int, _headers: PackedStringArray, _body: PackedByteArray) -> void:
	progress.hide()
	if result != HTTPRequest.RESULT_SUCCESS or response != 200 or not FileAccess.file_exists(zip_path) or FileAccess.get_sha256(zip_path) != expected_hash:
		DirAccess.remove_absolute(ProjectSettings.globalize_path(zip_path))
		_finish("Windows update download failed checksum verification. Retry.")
		return
	_ready_to_install()

func _ready_to_install() -> void:
	update_verified = true
	install.show()
	_finish("PC " + release_tag + " downloaded and verified. Install when ready.")

func _install() -> void:
	if not update_verified or FileAccess.get_sha256(zip_path) != expected_hash:
		_finish("Downloaded ZIP has changed. Check again.")
		return
	if not OS.has_feature("windows") or OS.get_executable_path().get_file().to_lower() != "forests-of-hell-pc.exe":
		_finish("Install from a packaged Forests-of-Hell-PC.exe to update automatically.")
		return
	var script_path := ProjectSettings.globalize_path("user://foh-apply-update.ps1")
	var file := FileAccess.open(script_path, FileAccess.WRITE)
	if file == null:
		_finish("Cannot create updater script (file permissions).")
		return
	file.store_string(_installer_script())
	file.close()
	var args := PackedStringArray(["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script_path,
		"-GameExe", OS.get_executable_path(), "-ZipFile", ProjectSettings.globalize_path(zip_path),
		"-GamePid", str(OS.get_process_id())])
	var updater_pid := OS.create_process("powershell.exe", args)
	if updater_pid <= 0:
		_finish("Cannot launch Windows updater. Check Windows permissions.")
		return
	status.text = "Installing verified update after game closes..."
	get_tree().quit()

func _installer_script() -> String:
	return """
param(
  [Parameter(Mandatory=$true)][string]$GameExe,
  [Parameter(Mandatory=$true)][string]$ZipFile,
  [Parameter(Mandatory=$true)][int]$GamePid
)
$ErrorActionPreference = 'Stop'
$folder = Split-Path -Parent $GameExe
$stage = Join-Path $env:TEMP ('ForestsOfHell-Update-' + $GamePid)
$log = Join-Path (Split-Path -Parent $ZipFile) 'windows-update-log.txt'
try {
  $running = Get-Process -Id $GamePid -ErrorAction SilentlyContinue
  if ($running) { $running | Wait-Process -Timeout 60 -ErrorAction Stop }
  New-Item -ItemType Directory -Force -Path $stage | Out-Null
  Expand-Archive -LiteralPath $ZipFile -DestinationPath $stage -Force
  $files = @('Forests-of-Hell-PC.exe', 'Forests-of-Hell-PC.pck')
  foreach ($name in $files) {
    if (!(Test-Path -LiteralPath (Join-Path $stage $name))) { throw ('Missing release file: ' + $name) }
  }
  foreach ($name in $files) {
    $target = Join-Path $folder $name
    if (Test-Path -LiteralPath $target) { Copy-Item -LiteralPath $target -Destination ($target + '.update-backup') -Force }
  }
  try {
    foreach ($name in $files) {
      Copy-Item -LiteralPath (Join-Path $stage $name) -Destination (Join-Path $folder $name) -Force
    }
  } catch {
    foreach ($name in $files) {
      $target = Join-Path $folder $name
      if (Test-Path -LiteralPath ($target + '.update-backup')) { Copy-Item -LiteralPath ($target + '.update-backup') -Destination $target -Force }
    }
    throw
  }
  foreach ($name in $files) {
    $backup = Join-Path $folder ($name + '.update-backup')
    if (Test-Path -LiteralPath $backup) { Remove-Item -LiteralPath $backup -Force }
  }
  ('Installed at ' + (Get-Date)) | Set-Content -LiteralPath $log
  Start-Process -FilePath $GameExe -WorkingDirectory $folder
} catch {
  ('Update failed: ' + $_.Exception.Message) | Set-Content -LiteralPath $log
  Add-Type -AssemblyName PresentationFramework
  [System.Windows.MessageBox]::Show(('PC update failed. Existing installation was preserved where possible. See ' + $log), 'Forests of Hell updater') | Out-Null
} finally {
  Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}
"""
