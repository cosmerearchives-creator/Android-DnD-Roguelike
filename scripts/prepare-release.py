"""Prepare a versioned, checksum-protected updater manifest."""
import hashlib
import json
import os
import pathlib
import shutil
import sys

root = pathlib.Path(__file__).resolve().parent.parent
version = dict(line.strip().split("=", 1) for line in
               (root / "version.properties").read_text().splitlines()
               if "=" in line and not line.startswith("#"))
name = version["versionName"]
code = int(version["versionCode"])
if code < 1 or not all(part.isdigit() for part in name.split(".")) or len(name.split(".")) != 3:
    raise ValueError("Use a positive versionCode and a three-part numeric versionName")
dist = root / "dist"
dist.mkdir(exist_ok=True)
apk = dist / "Android-DnD-Roguelike.apk"
source = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "app/build/outputs/apk/release/app-release.apk"
if source != apk:
    shutil.copyfile(source, apk)
repo = os.environ.get("GITHUB_REPOSITORY", "cosmerearchives-creator/Android-DnD-Roguelike")
manifest = {
    "versionCode": code, "versionName": name,
    "apkUrl": f"https://github.com/{repo}/releases/download/v{name}/{apk.name}",
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
}
(dist / "update.json").write_text(json.dumps(manifest, indent=2) + "\n")
