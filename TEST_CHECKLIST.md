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


## v0.3.191 environment evidence — 2026-11-27, Stormfather
PASS `python scripts/verify_terrain_import.py`: all 7 terrain/tavern GLBs validated, 5 original ground and the tavern source GLB SHA256 verified, all 23 full-resolution extracted image bytes equal embedded original bytes; grass source hash in derivative verified; all 4 material map roles kept.
PASS Blender 5.3 import/measure and original tavern arch cross-section probe. PASS Godot 4.5.1 editor import, actual PC OpenGL 3.3 NVIDIA GTX1080Ti graphical scene screenshot at 1280×720 (see `Tavern Review\godot-tavern-and-ground.png`).
PASS nine headless scripted tests with exit=0: terrain_tavern_smoke, terrain_tavern_physics, lean_cast_smoke, enemy_spawn_smoke, inventory_smoke, inventory_tabs_smoke, male_leather_armor_smoke, pc_update_smoke, pc_platform_smoke (with FOH_TEST_PC=1). Physics test sampled ten doorway approach locations using an 8-foot tall capsule and 2.1m entrance gap. 530 terrain cells in 6 batches.
NOT RUN: on-device Android render and sustained FPS/memory, signed release install and auto-update, full biome seamless transitions, NPC tavern interactions. LIMITATION: Godot4.5.1 normal headless main-scene boot logged dummy renderer/resource preload errors; graphical main menu launch was successful. Verify GitHub 4.4.1 CI separately.


## v0.3.192 acceptance/regression tests
- Run 2014 5e and equipment UI suites, terrain source hash/texture gate and tavern entry with tall capsule; original character rigs/skeletons unchanged.
- inventory_pause_and_blend_smoke.gd: 15 slots, 6 full terrains+underlays, 0-30in slider saved to disk, modal pausing and resume, Materials tab interaction while paused.
- display_camera_persistence_smoke.gd: saved fullscreen/window geometry and camera perspective/zoom to user config; check F11 and gameplay perspective live.
- PC visual review: actual Godot screenshot exports of all four tabs and tavern/ground; live source GLBs render without shader error. Android real-device FPS/scale check still outstanding.

## v0.3.193 QA acceptance
- Run python scripts/verify_ground_cover_import.py: 4 unmodified source checksums when local Review available, 4 derivative model collections, 48 names, all original 2048px image hashes and PBR count unchanged, zero skeletons.
- Run Godot tests/ground_cover_smoke.gd: 40 3D MultiMesh variants, 1,666 placements in six biome types, 2048px imported albedo textures, model origins on floor, narrow material edge shader present. Confirm PC 1600x900 screenshot in Ground Cover Review. Crosscheck current 2014 5e player/enemy/inventory/PC regression suite.
- Verify CI Godot4.4.1 Linux imports, Android and Windows exports, original production APK signature, updater JSON manifests and release assets. Real-device visual/performance still outstanding.

## v0.3.194 verification
- Run Godot tests/inventory_desktop_layout_smoke.gd with FOH_TEST_PC=1: 1600x900 landscape region ordering, no slot overflow, 15 equipment slots, 4 tabs with filters, 10 required custom artwork assets; CI gate. Run portrait inventory_smoke and inventory_tabs_smoke at 720x1280/1536. Run inventory_pause_and_blend_smoke with HUD now asserted hidden/open and visible/closed. Run male armor, character camera, PC updater, terrain, ground cover, enemy and lean cast regression tests unchanged.
- Visual inspection four latest 1600x900 real Godot images in G:/ChatGPT Projects/Forests of Hell/Inventory Review against user originals; no fake screenshots. Verify Android/Windows exports and real signer + updater assets in GitHub release after commit. Android manual UI/performance still needed.

## v0.3.195 release tests
- Verify actual Windows Godot 1600×900 screenshots of four tabs with new panel assets and rectangular controls. Test `inventory_desktop_layout_smoke.gd` with FOH_TEST_PC=1, including 15 slots, tab navigation, Chain Mail Body vs Cloak, Longsword Main Hand vs Body and selected item frame. Portrait inventory_smoke and inventory_tabs_smoke must run without FOH_TEST_PC. Run pause/HUD, camera, male rig armor, terrain/tavern, ground-cover, enemy, casting and PC updater regression. CI signer/Windows/Android updater manifests and actual release assets must be verified after main push. Android on-device review still pending.

## v0.3.196 Equipment source-art review and regression
- SHA/crop fidelity: run python scripts/verify_approved_equipment_art.py (original byte-exact 1280×1229; six exact source crops; 15th slot source-derived; Godot texture lossless/no size limit). CI asserts source hash. 1600×900 OpenGL screenshot equipment.png must show actual artwork and 3D player, no duplicated gold generated borders, and 15 click slots. Inventory desktop smoke asserts all 15 controls, 4 tabs, artwork panels correctly fitted and Chain Mail Body/Longsword Main Hand logic; mobile portrait inventory and tabs, inventory pause/HUD restoration, camera/display persistence, male GLB armor, terrain/tavern/physics, ground clutter, enemy, lean cast, updater and PC controls must pass. CI signed APK, Windows ZIP, updater JSON and app identity verification required. Android on-device visual still pending.

## v0.3.201 — playable goblin and modular equipment

Verified the approved proof of concept in the desktop preview. Automated checks passed for goblin motion, equipment toggle, creator preview, phone inventory at 1280 and 1536 pixels, desktop inventory, and terrain. Physical Android installation and on-device visual review are outstanding.

## v0.3.202 local release candidate

The user-supplied Female No Race Base is the only playable character. Height, frame width and body depth are saved and available in the creator. Experimental weight and muscle sliders were removed. Existing character selections resolve to the sole playable model. The female base is unarmored and contains its original 66 joints and ten source animation clips. Creator, saved scale, PC controls, inventory and terrain headless checks passed. Physical device and equipment fit verification remain outstanding.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## v0.3.205 creator validation
PASS (Godot4.5.1 local): creator_workshop_smoke, creator_v204_scale_smoke, character_appearance_smoke, creator_v205_ui_smoke, and updated creator_preview_smoke (validates actual dedicated scene/hair size/zoom/cleanup). PASS: actual OpenGL 1673x940 desktop screenshot under Creator Review; original source SHA. CI gate: Android/Windows build, signatures and update manifests; device portrait/landscape usability remains to be checked.

## v0.3.205 publication verification — 2026-11-28
GitHub Actions run 46163037849 completed SUCCESS, including production signing and stable release publication. Release v0.3.205 targets d5a5f4043a08780214c12c885a50cfda55f87ef2. Verified public Android APK (540573185 bytes), Windows x64 ZIP (544121874 bytes), and both live updater manifests at /releases/latest/download/{update.json,windows-update.json}: versionCode 226, versionName 0.3.205, matching release download URLs and SHA-256 digests. Android manifest SHA d5d78ce51abc587981918c7027f62631e94d3a1f3b6341ebd72ce34607d83acf; Windows manifest SHA bd64bd7de555672e09fa9a176f71138e7d184f6c528310ebaec7024ef83039ea. On-device visual/UI operation remains unverified.

## v0.3.206 creator accessory gates
- PASS: Godot 4.5.1 creator_workshop_smoke, creator_v204_scale_smoke (now seven frames for dedicated category panel), creator_v205_ui_smoke, creator_preview_smoke (explicit 1.0 baseline hair size / no beard), character_appearance_smoke, appearance_head_fit_smoke (all supplied variants, 66 original bone names/parents, bone rotation lock, AABB top fit, offsets), appearance_xyz_ui_smoke (Hair/Beard panels, nudge, save/reload, reset), inventory_desktop_layout_smoke (FOH_TEST_PC=1), inventory_pause_and_blend_smoke, display_camera_persistence_smoke: 10/10 passed.
- PASS: actual Godot OpenGL screenshots for Wavy, Short, Braid, Full Beard reviewed in Creator Review. Original playable model, hair/beard GLBs, original source UI assets and textures unchanged.
- Required remaining: stable signed Android/Windows release and both public update manifests; physical phone/PC UI/animation validation.

## v0.3.206 publication verification — 2026-11-28
GitHub Actions run 46165658216 (Godot Android and Windows) completed SUCCESS. The production signing identity passed the existing release signature verification, and stable release v0.3.206 targets source commit d9033a5ea3dd2429080ff08c500fc28fb2760bbd. Verified public APK Android-DnD-Roguelike.apk size 540581920 bytes SHA256 e95625c6ae8e63dca77dbdda9b7ef53382e2c05ea2d9bcf382b89249a2759969; Windows ZIP Forests-of-Hell-Windows-x64.zip size 544131935 bytes SHA256 44cdfbc5d8343656fc258512de41b1fc0872a275899af368dc41c26e9018f024. Verified public latest/download/update.json and latest/download/windows-update.json: versionCode 227, versionName 0.3.206, exact release download URLs and matching APK/ZIP SHA256 checksums. All 10 local Godot 4.5.1 creator/inventory tests passed. Visual check on physical Android device remains unverified; rigid unrigged hair strands have no independent deformation.

## v0.3.207 style switching release gate
- PASS Godot4.5.1 headless tests 11/11: creator_workshop_smoke, creator_v204_scale_smoke, creator_v205_ui_smoke, creator_preview_smoke, character_appearance_smoke, appearance_head_fit_smoke, appearance_xyz_ui_smoke, new appearance_style_switch_smoke, inventory_desktop_layout_smoke (FOH_TEST_PC=1), inventory_pause_and_blend_smoke, display_camera_persistence_smoke.
- New style-switch test asserts exact Wavy 1.90 and (-0.03,+0.03,+0.01) values, Short/Braid default 1.00/zero XYZ, same head attachment and head-level world Y before any frame delay, restores Wavy, then stores distinct Short 1.25/+0.02 X while preserving Wavy, verifies ConfigFile save/reload and UI labels.
- PASS actual Godot OpenGL 1673x940 screenshots reviewed for each switch. Required before user-device acceptance: verify Android signature/install, Windows release, both latest updater manifests, physical mobile screen/animation.

## v0.3.207 publication verification — 2026-11-28
GitHub Actions Godot Android and Windows workflow run 46169088954 completed SUCCESS, including production signing/identity verification and stable release publication. Release v0.3.207 targets source commit ae74b31d604c80c733c1b014f610fbbdb0e4cdb7. Verified public Android-DnD-Roguelike.apk size 540590301 bytes SHA256 cef2fe3d8a205b551a37dbe9a2d3aac2f3bc16f96135271e72f51784a48063c8 and Windows ZIP Forests-of-Hell-Windows-x64.zip size 544138520 bytes SHA256 a6a9d46144a1c788649a02d3362bdae00872f1bc868f063f7a02ebae48ca6512. Verified both live /releases/latest/download/update.json and /releases/latest/download/windows-update.json return versionCode 228, versionName 0.3.207, matching exact public release URLs and checksums. Local Godot4.5.1 11/11 regression smokes and 1673x940 GPU screenshots passed. Physical Android device/touch and walking hair behavior remain unverified; supplied hair is static and rigidly follows the real head bone. No original skeleton, bone maps, animations, texture quality or source GLBs changed.

## v0.3.208 hair color regression
- PASS: hair_color_smoke asserts swatches/custom picker, instant material tint, original source material untouched, per-style color carryover, save/reload and unchanged head position.
- PASS: 10 creator/appearance/inventory layout and camera smokes (creator_workshop, creator_v204_scale, creator_v205_ui, creator_preview, character_appearance, appearance_head_fit, appearance_xyz_ui, appearance_style_switch, hair_color, inventory_desktop_layout, display_camera_persistence = 11 total).
- PARTIAL: inventory_pause_and_blend_smoke emitted success marker but headless dummy renderer cleanup errors. Android physical/device acceptance pending.

## v0.3.209 hair color layout regressions`n- PASS: creator/appearance/hair/fit/style/color/inventory desktop/display camera smokes (11 clean). New hair-color smoke asserts preset names, list count, inline widget bounds, custom picker, persistence and material isolation.`n- PARTIAL: inventory_pause_and_blend_smoke emits expected pass marker but also dummy renderer null material/leaked RIDs errors at exit; Windows/Android render/device review outstanding.`n