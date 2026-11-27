# Forests of Hell — validation and release checklist
Each item must be marked PASS / FAIL / NOT RUN with command, device, version and evidence before claiming a release complete. This documentation-only audit did **not** run game smoke tests.

## Before editing
- [ ] Read `AGENTS.md` and `HANDOFF.md`; inspect `git status`, `git log`, current `version.properties`.
- [ ] Confirm feature is not already implemented; record acceptance criteria and dependencies.
- [ ] Confirm **appropriate user-provided** skeleton exists and is fully compatible; **STOP ALL DEVELOPMENT** if missing/incompatible/incomplete.
- [ ] Import **all** bone maps and verify every name/hierarchy/track; no omissions.
- [ ] Compare every source texture/map to final Godot import (size, channels, fidelity); no unapproved compression/downscale.
- [ ] Preserve user originals, unrelated files and all pre-existing untracked assets.

## Code/assets and gameplay
- [ ] Validate engine parses scripts and scenes, imports GLBs and maps skeletons/animations correctly.
- [ ] Run applicable Godot smoke scripts in `godot/tests/`: `lean_cast_smoke.gd`, `inventory_smoke.gd`, `inventory_tabs_smoke.gd`, `enemy_spawn_smoke.gd`, `pc_platform_smoke.gd`, `pc_update_smoke.gd` (PC flags/environment as required).
- [ ] Run applicable model validation tooling only if it complies with the user-provided skeleton/texture gates.
- [ ] Check affected combat, enemy spawn/facing, inventory tabs/equipment, races/lighting, clock, save state, front menu and control regressions.
- [ ] PC real-render test: uncaptured/captured mouse, menu transitions, F11/windowed/maximized, forest loading/progress, updater install and version.
- [ ] Android device: portrait/full-screen/input, visual fidelity, frame times, safe install/update/downgrade and persistent saves.
- [ ] Record exceptions, deferred device tests and known mismatches in `KNOWN_ISSUES.md`.

## Publishing
- [ ] Only intended tracked files staged; unrelated/untracked local work left alone.
- [ ] Gameplay version incremented and synchronized; docs-only changes need no new APK.
- [ ] Push to existing `main`; validate remote HEAD.
- [ ] Verify CI result, signed APK/app ID/signature, Windows ZIP, updater manifest and download URLs where relevant.
- [ ] Update `CHANGELOG.md`, `IMPLEMENTATION_STATUS.md`, `ASSET_MANIFEST.md`, `ROADMAP.md`, `HANDOFF.md`.
- [ ] Final user report includes completed/not completed, tests, version, commit, actual publication and links.

Example local smoke (Windows PowerShell, with Godot on PATH): `godot --headless --path godot --script res://tests/inventory_tabs_smoke.gd`. Some PC tests require `FOH_TEST_PC=1`; consult their script and workflow before invocation.

## v0.3.190 targeted test evidence — 2026-11-27, Stormfather
- PASS: Original male GLB SHA256 intact; 84 matching skinned bone names and parent relationships, exported rest-transform error less than 0.000005 model units.
- PASS: Male armor GLB retains all four original leather PBR material values. Three extracted male image companions are byte-identical to the original male texture source images; base GLB binary and original Running clip preserved in combined asset.
- PASS: Blender 5.3 combined asset import: 2 skinned meshes on one original 84-bone skeleton; front, rear, gameplay and Running previews produced.
- PASS: Godot 4.5.1 headless editor import and `godot/tests/male_leather_armor_smoke.gd`: 84 bones, 2 skinned meshes, shared animation 65 mapped bones/40 clips, leather armor inventory preview loaded.
- PASS: `inventory_smoke.gd` (1280/1536), `inventory_tabs_smoke.gd` (1280/1536), and `pc_platform_smoke.gd` (`FOH_TEST_PC=1`), including PC first-person/camera checks.
- NOT RUN: actual Windows/Android visual game playthrough and runtime FPS profiling. Android SDK build-tools path warning appeared during local Godot editor startup. PC-platform smoke logged RID leakage warnings on shutdown.
- PENDING: verify remote `main` push, signed Android APK, PC ZIP and updater manifest, then record confirmed release URLs.
