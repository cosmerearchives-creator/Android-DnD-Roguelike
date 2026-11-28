# Implementation status — evidence-qualified audit
Audited 2026-11-27 against checkout `ca48539a876ccf6f9355bbc11f1d89788abd1202` (`main`, `version.properties` reports 0.3.188 / code 209). This was a **static repository/documentation inspection**, not a fresh build, runtime session, GitHub release retrieval, or hardware playthrough.

| Area | Evidence seen | Status and limitations |
|---|---|---|
| Godot entry + scenes | `godot/project.godot`: `front_menu.tscn`, Compatibility renderer; `world.tscn`, `bestiary.tscn`, `inventory_screen.tscn` | Present in repository; full runtime unverified |
| Android + Windows packaging | `.github/workflows/godot-android.yml`, `version.properties`, `godot/export_presets.cfg`; [docs/pc-windows-dual-platform-v0.3.185.md](docs/pc-windows-dual-platform-v0.3.185.md) | Build/release automation exists; downloadable artifacts and signing identity not independently verified here |
| PC fullscreen, updates, mouse and forest loading | `godot/pc_fullscreen.gd`, `pc_release_updates.gd`, `forest_loading.gd`, [docs/pc-free-mouse-look-startup-v0.3.186.md](docs/pc-free-mouse-look-startup-v0.3.186.md), `godot/tests/pc_platform_smoke.gd`, `pc_update_smoke.gd` | Implemented in source; end-to-end user PC check pending |
| Inventory and quests | `inventory_data.gd`, `inventory_screen.gd`, `item_catalog.json`; [docs/inventory-tabs-and-spawn.md](docs/inventory-tabs-and-spawn.md), `inventory_tabs_smoke.gd` | UI/data and progress documented; some item actions/materials/equip effects deferred; device verification pending |
| Combat and enemies | `creature_combat_stats.gd`, `enemy_spawner.gd`, `goblin_variants.gd`; [combat foundation](docs/combat-foundation-v0.3.184.md) | Basic attacks/AC/HP/lock/chase documented; temporary real-time timing and provisional player stats; incomplete 5e traits |
| Bestiary and model assets | `bestiary.gd`, seven tracked `godot/assets/*.glb` (see [ASSET_MANIFEST.md](ASSET_MANIFEST.md)) | Model files present; full source rig provenance, complete bone-map verification and texture fidelity unverified |
| Races, clock, sunlight and weather | [docs/racial-backlog.md](docs/racial-backlog.md), [docs/world-development.md](docs/world-development.md) | Historical implementation claims predate/currently cross migration; **not verified in present Godot scenes** |
| Shared animation and rig validation | `humanoid_shared_animations.gd`, `quadruped_shared_animations.gd`, `scripts/validate_humanoid_import.py`, `scripts/validate_quadruped_import.py` | Tools exist; the new user-provided skeleton-only policy needs an audit before further asset work |
| Smoke test scripts | `godot/tests/enemy_spawn_smoke.gd`, `inventory_smoke.gd`, `inventory_tabs_smoke.gd`, `lean_cast_smoke.gd`, `pc_platform_smoke.gd`, `pc_update_smoke.gd` | Present; **not executed in this documentation-only audit** |

## Historical vs current
`README.md` begins with v0.3.53 and describes old illustrated sprite rigs; `docs/world-development.md` describes legacy Android `OutdoorActivity`, while current Godot starts from `front_menu.tscn`. Treat those as historical evidence, not proof of 2026 Godot functionality. Specific design details remain preserved in place.

## Pending verification
End-to-end Android and Windows build, model/skeleton/bone-map completeness, original texture vs imported output, device frame times, latest GitHub release artifacts, multiplayer/quests/items as applicable, save compatibility, F11 and updater smoke. See `TEST_CHECKLIST.md`, `KNOWN_ISSUES.md`.

## v0.3.190 update — fitted male leather armor
**Implemented and locally tested, release not independently confirmed by these checks:** standalone fitted male armor and combined male/armor GLB, equipped Leather Armor visual switching in Godot's male inventory portrait and current world player, restoration on unequip, and animation-lab target. Existing male rig untouched; 84/84 bone names and hierarchy match source, 65 animated shared bone mappings and 40 clips reported. Original male textures compare byte-for-byte to imported companion files. Godot 4.5.1 headless GLB import and `male_leather_armor_smoke.gd`, `inventory_smoke.gd`, `inventory_tabs_smoke.gd`, and `pc_platform_smoke.gd` passed. Hardware visual, GPU cost, PC ZIP, signed APK and updater confirmation remain outstanding until explicitly checked.


## v0.3.191 — ground asset layer and tavern
Implemented locally: six authentic imported material families, one 5ft/1.524m ground grid, six MultiMesh draw batches covering 530 visible cells near spawn/tavern, runtime perimeter feathering (internal edges not yet biome-blended), retained base walkable terrain and procedural scenery, and a full-resolution open-roof tavern room with aligned floor, wall/hearth collision and accessible doorway. Existing character rigs/models unchanged.
Verified: original five ground + tavern GLB SHA256 and 23 source image bytes; full PBR channel retention; graphical Windows OpenGL compatibility launch and live 1280×720 screenshot `G:\ChatGPT Projects\Forests of Hell\Tavern Review\godot-tavern-and-ground.png`; 9 smoke tests PASS including 8ft capsule passage. No Android device render/frame-time test. No publication claim until CI + signed APK and Windows ZIP verification.


## v0.3.192 combined publishing milestone (2026-11-27)
- Implemented new user-provided grass source and reviewed original, terrain underlay and edge-only shader with saved 0-30in live slider, display/camera preferences, 15-slot inventory, ornate SVG four-tab desktop UI, and real inventory pause.
- Local Godot 4.5.1 headless tests and Windows OpenGL previews passed; CI signed Android/Windows publishing and device-side FPS remain subject to release verification. Details and known limits: docs/inventory-terrain-settings-v0.3.192.md.

## v0.3.193 — Ground cover + tile rim refinement (2026-11-27)
- Implemented and locally tested: four GLB collection extracts (48 named models), 40 active variants / 1,666 deterministic instanced plants/debris across 530 local terrain cells, 2048x2048 embedded source textures SHA-identical, Godot 4.5.1 gameplay render and headless smoke, vertex shader rim flattening plus limited shared world-space seam color. Original four source GLBs moved Imports -> Beastiary Review.
- Missing exactly 4/4 requested short-grass assets; 12 broadleaf extras are alternatives, not relabeled grass (4 active, 8 reserved). Some pale moss/white foliage artifacts remain. Square grid seams reduced only partially, and real Android performance not verified. Details: docs/ground-cover-and-seams-v0.3.193.md. Signed APK/PC release verification pending.

## v0.3.194 desktop inventory UI focus (2026-11-27)
- Implemented local: complete custom-art desktop landscape presentation refresh, purpose-built nine-slice frames/slots/tabs/buttons, new actual 3D character portrait backdrop, readable four-column stats, bounded item icons and collection list, actual HUD fully hidden while inventory modal paused and restored on close, compact non-invented item descriptions. Original 15 slots and four tabs, portrait UI, data, save, player rigs unchanged.
- Tested: 13 headless Godot 4.5.1 smokes incl new desktop-layout gate + portrait tabs and pause/HUD test; actual Nvidia GTX1080Ti graphical screenshot of each tab saved to Inventory Review. Not a pixel-perfect recreation of reference; Android visual and production release pending verification. Details docs/inventory-ui-visual-v0.3.194.md.

## v0.3.195 inventory UI presentation (2026-11-27)
- Locally implemented/tested: crafted high-res bronze scrollwork and leather texture panels, separate rectangular active/normal button, tab and slot assets, 650x1060 textured stone model-stage backdrop, stronger item preview hierarchy and visual grid consistency. Real player/asset GLBs and skeletons untouched. 15 equipment slots and all 4 functional tabs remain.
- Fixed item info stale selected slot (Chain Mail correctly Body, Longsword Main Hand), tested in inventory_desktop_layout_smoke.gd. Other 12 smoke tests passed, screenshots captured. Android/PC release and real-device render need separate verification; full parity to image target is not achieved. docs/inventory-ui-visual-v0.3.195.md.

## v0.3.196 user reference Equipment tab (2026-11-27)
- Equipment now renders the user's actual source art panel/title/tab/grids/stone stage with real native Control overlays, actual selected item and equipped items, live player GLB, and 15 interactive slots. Original source PNG exactly SHA-preserved; fitted derivative creates eighth right-side Accessory placeholder using user's own frame and blank leather; image crops unchanged. Other tabs remain functional and retain v0.3.195 styling. Tests: all 13 local 4.5.1 regressions pass, original art integrity + import quality pass, OpenGL visual review screenshot saved. CI signed APK/PC and Android device QA not yet independently verified.

## v0.3.201 — playable goblin and modular equipment

Integrated the supplied 28-joint female goblin, modular leather on supported players, persisted hair sizing, desktop face zoom and updated inventory, menu and terrain presentation. The user accepted the goblin outfit correction after motion review.

## v0.3.202 local release candidate

The user-supplied Female No Race Base is the only playable character. Height, frame width and body depth are saved and available in the creator. Experimental weight and muscle sliders were removed. Existing character selections resolve to the sole playable model. The female base is unarmored and contains its original 66 joints and ten source animation clips. Creator, saved scale, PC controls, inventory and terrain headless checks passed. Physical device and equipment fit verification remain outstanding.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## v0.3.205 creator visual refinement — locally validated
Implemented unique compact selection/arrow frame families, selected orange highlight, independent visible slider rails, aligned labels/diamond controls and isolated stone stage. Real OpenGL screenshot and five creator/appearance tests passed. Production binary and physical Android review remain independent gates. No gameplay or rig changes.

## v0.3.205 publication verification — 2026-11-28
GitHub Actions run 46163037849 completed SUCCESS, including production signing and stable release publication. Release v0.3.205 targets d5a5f4043a08780214c12c885a50cfda55f87ef2. Verified public Android APK (540573185 bytes), Windows x64 ZIP (544121874 bytes), and both live updater manifests at /releases/latest/download/{update.json,windows-update.json}: versionCode 226, versionName 0.3.205, matching release download URLs and SHA-256 digests. Android manifest SHA d5d78ce51abc587981918c7027f62631e94d3a1f3b6341ebd72ce34607d83acf; Windows manifest SHA bd64bd7de555672e09fa9a176f71138e7d184f6c528310ebaec7024ef83039ea. On-device visual/UI operation remains unverified.

## v0.3.206 creator appearance — implemented and tested locally, release pending
- User-source female character: all 66 original joint names and parents validated, head bone index 5, face +Z. Static hair/beard GLBs fitted by measured mesh bounds without touching textures or bones. Real-time XYZ adjustment/reset/persistence applies via shared APPEARANCE.apply in creator/world/inventory. Dedicated Hair/Beard panel, gold slider and gentler preview light.
- Godot 4.5.1 full 10-test local suite passed, including source bone-follow rotation and saved offsets, inventory desktop (FOH_TEST_PC=1) and inventory pause. Four actual renderer screenshots exist under Creator Review. See docs/appearance-head-fit-v0.3.206.md. Publication/device behavior not yet verified.

## v0.3.206 publication verification — 2026-11-28
GitHub Actions run 46165658216 (Godot Android and Windows) completed SUCCESS. The production signing identity passed the existing release signature verification, and stable release v0.3.206 targets source commit d9033a5ea3dd2429080ff08c500fc28fb2760bbd. Verified public APK Android-DnD-Roguelike.apk size 540581920 bytes SHA256 e95625c6ae8e63dca77dbdda9b7ef53382e2c05ea2d9bcf382b89249a2759969; Windows ZIP Forests-of-Hell-Windows-x64.zip size 544131935 bytes SHA256 44cdfbc5d8343656fc258512de41b1fc0872a275899af368dc41c26e9018f024. Verified public latest/download/update.json and latest/download/windows-update.json: versionCode 227, versionName 0.3.206, exact release download URLs and matching APK/ZIP SHA256 checksums. All 10 local Godot 4.5.1 creator/inventory tests passed. Visual check on physical Android device remains unverified; rigid unrigged hair strands have no independent deformation.
