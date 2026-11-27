# Forests of Hell v0.3.192 — combined inventory, terrain, pause, display and camera update
Date: 2026-11-27. User approved implementing this as one milestone, not a sequence of speculative builds.

## Terrain source, quality and organization
- Original new user-provided `grassground.glb` moved from `Beastiary Imports` to `Beastiary Review` after being copied byte-for-byte to `godot/assets/terrain/grass.glb`.
- Grass source SHA256: `ebe40701ac73d59c264f6d8c22c7ed4223f4520039de6be9b93749066126c0d2`, 3,028 vertices, 8,467,128 bytes; original three embedded PBR map bytes extracted unchanged as `grass_0.jpg`, `grass_1.png`, `grass_2.png`. Older 1.9-million-vertex source in Background Assets untouched.
- Other five terrain materials and tavern source unchanged and verified by `scripts/verify_terrain_import.py`. Six material families remain 5×5-foot logical cells, 1.524m spacing, source meshes unchanged; six existing MultiMesh batches cover 530 cells near hub/tavern.
- A second 5ft opaque, flat earth underlay per cell covers mesh edge gaps. New biome shader samples the six original material albedos, preserving normal and metallic/roughness for each primary tile. Neighbor mix is restricted to a noisy strip inward from the shared edge, not throughout the tile. Distinct-neighbor flags are carried in instance custom data.
- The world base grass beneath the imported tiles is darkened to avoid bright neon holes.
- The world Menu now exposes a live `Terrain edges` slider, 0–30 inches, default 6 inches, stored under `user://terrain_blending.cfg`. 0 disables blending; width does not regenerate source meshes.

## Display / camera
- Global F11 fullscreen/window toggle preserved.
- `DesktopFullscreen` autoload now persists actual windowed size/position or fullscreen mode, and the PC first-/third-person state, FOV and third-person mouse-wheel distance, plus mobile camera angle and orthographic size, in `user://display_camera.cfg`.
- Normal startup uses the user's saved windowed size instead of enforcing the legacy minimum 1200px. Window location is constrained to a reasonable visible monitor position.
- Camera perspective restored on world load; PC V remains first-/third-person toggle, mouse-wheel zoom saved on change.

## Inventory UI and gameplay
- Four user-supplied reference screenshots are design targets, not fake static UI. The landscape 1600×900 native layout builds functional Godot controls with separately authored editable ornamental SVG files (`blender/ui_skin/build_inventory_skin.py`, `godot/ui_skin/*.svg`). TextureStyleBox 9-slices skin the actual buttons and frame controls. Portrait-specific layout retained.
- Landscape Equipment: live player GLB preview on left; stats/proficiencies/character on top right, inventory grid middle, item actions and details bottom. Stats only display actual implemented values: existing provisional HP/AC, inventory STR; unknown 2014 5e scores and proficiencies remain labeled missing rather than invented.
- 15 slots: Head, Eyes/Goggles, Main Hand, Off Hand, Secondary, Hands, Wrists, Body, Neck, Cloak, Left Ring, Right Ring, Accessory, Waist, Feet. Ears and Legs removed; saved Ranged entry migrates to Secondary. Existing equip restrictions and male Leather Armor visual preserved.
- Items: category sidebar, scrollable item grid, weight, item detail, Equip for suitable owned weapons/armor. Materials: category sidebar and grid from actual tagged material data (currently empty). Quests: sidebar, actual Goblin Menace quest, current 0–10 progress and Track setting; no invented rewards or additional quests.
- Unsupported interactions such as physical-world item Drop and consumable Use remain disabled rather than performing fake actions. Compared-item functionality is preliminary (owned name/weight) and needs complete item rules/stat calculation later.
- Opening inventory calls `SceneTree.paused=true` and keeps only the overlay processing via `PROCESS_MODE_ALWAYS`. Closing/unloading unpauses. Enemy/world/player processes, timers and world motion stop while the modal is open; tabs and actions continue to work.

## Tests / limitations
- Source integrity: `scripts/verify_terrain_import.py` reports all 7 assets and maps intact.
- Godot 4.5.1 targeted smoke tests: lean-cast, enemy spawn, equipment and portrait tabs, fitted male armor, terrain geometry/doorway, physics entry, PC camera/input, PC updater, plus new `inventory_pause_and_blend_smoke.gd` and `display_camera_persistence_smoke.gd` all passed locally.
- Live graphical OpenGL compatibility preview on NVIDIA GTX 1080Ti: four inventory screens 1600×900 saved under `G:\ChatGPT Projects\Forests of Hell\Inventory Review\`, terrain/tavern world preview under `G:\ChatGPT Projects\Forests of Hell\Tavern Review\godot-tavern-and-ground.png`.
- Material crossfade is an edge-only experimental shader, not a full continuous biome mesh; recognizable straight tile boundaries and material/texture differences can still be seen. More accurate UV/topdown material baking and visual edge tuning would improve future fidelity. Base forest scenery is existing provisional style; no trees/characters were altered.
- Neither Android frame time/memory nor real user-machine fullscreen restore after reinstall has been verified. This is a tested implementation target, subject to CI and device review. The source screenshots aren't game guarantees; live world art still differs from the mocked reference.
- Version 0.3.192/code213; GitHub release and updater artifacts must be verified after CI success (do not infer from this document).
