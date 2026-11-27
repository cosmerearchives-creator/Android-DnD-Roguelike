"""Write the checksum-protected Windows update manifest alongside the packaged ZIP."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
version = dict(line.strip().split("=", 1) for line in (root / "version.properties").read_text().splitlines() if "=" in line)
name = version["versionName"]
code = int(version["versionCode"])
assert code > 0 and len(name.split(".")) == 3 and all(v.isdigit() for v in name.split("."))
archive = root / "dist" / "Forests-of-Hell-Windows-x64.zip"
assert archive.is_file() and archive.stat().st_size > 100000, "Build and package Windows before creating manifest"
manifest = {
    "versionCode": code,
    "versionName": name,
    "zipUrl": f"https://github.com/cosmerearchives-creator/Android-DnD-Roguelike/releases/download/v{name}/{archive.name}",
    "sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
}
(root / "dist" / "windows-update.json").write_text(json.dumps(manifest, indent=2) + "\n")
print("Windows updater manifest created for", name)
