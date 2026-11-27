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
