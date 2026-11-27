# Consolidated change log (non-destructive index)

## v0.3.191 — six imported terrain types and tavern hall (2026-11-27)
- Added grass, dirt, leaves, moss, mud and rocky soil on exactly 1.524m ground cells. 530 placed tiles are drawn through six MultiMesh batches near the starting area and tavern approach; the existing continuous collidable grass ground remains underneath and beyond this initial region.
- Kept five imported ground GLBs and the open-roof tavern room byte-for-byte unchanged. Replaced only the impractical 1.9-million-vertex grass-source geometry with a four-vertex ground plane using all original 2048px embedded PBR maps, with source untouched. Imported original full-resolution PBR companions for all six terrain types and the tavern.
- Added a darkened runtime ground shading treatment and perimeter-only feathering, preserving original albedo, metallic/roughness and normal texture maps. Grass source's emissive map is retained but deliberately disabled on ground.
- Integrated the full-detail tavern as an accessible open-roof hall in the forest clearing, facing the player approach, with simple wall/hearth colliders and a 2.1m collision aperture. Existing doorway visual was probed at the 8–9ft height range; an 8ft capsule traversed ten test locations without obstruction. Character rigs and scales remain unchanged.
- Godot 4.5.1 graphical PC scene and screenshot confirmed, nine relevant smoke tests passed, all original embedded/extracted source image bytes verified. Physical Android rendering/frame-rate verification, more sophisticated biome transitions and tavern roof/furnishings remain future steps.

## v0.3.190 — fitted male leather armor with equip visuals (2026-11-27)
- Adapted the recovered female leather armor design to the original user-approved 84-bone male rig; changed only armor geometry/weights. Original male and female assets remain unchanged.
- Exported a fitted standalone male armor GLB and an armored male GLB that retains the exact original male binary buffer, skeleton hierarchy, Running animation and full-resolution embedded textures.
- Male Leather Armor on the Body equipment slot selects the armored model in the inventory portrait and active game world; unequipping reverts the male to his original appearance. Added armored male variant to Humanoid Animation Lab.
- Godot 4.5.1 headless import and targeted armor/inventory/PC smoke tests pass. Hardware visual and release-artifact verification remain separate gates; see `TEST_CHECKLIST.md` and `KNOWN_ISSUES.md`.
## 2026-11-27 — documentation continuity milestone
Nine root documentation files established/updated from repository and existing docs; no gameplay, tracked asset or build configuration changes; no application version increment. Verification and commit SHA recorded in `HANDOFF.md` after push.

## v0.3.188 — latest observed local version
`version.properties`: versionCode 209, versionName 0.3.188; checkout commit `ca48539` message: "Publish final PC fullscreen update as v0.3.188". GitHub binary artifacts not independently checked by this audit.

## v0.3.186–v0.3.187 — PC camera/loading and fullscreen follow-up
`docs/pc-free-mouse-look-startup-v0.3.186.md` documents mouse-driven camera and forest first-entry staging. Recent commits `d0d0bd8` and `e00dcdc` add Windows auto-update, F11 fullscreen and fix F11 from maximized windows.

## v0.3.185 — dual-platform build and PC inputs
See `docs/pc-windows-dual-platform-v0.3.185.md`.

## v0.3.184 — combat foundation
See `docs/combat-foundation-v0.3.184.md`.

## v0.3.174–v0.3.182 — tabs, bestiary, casting, quadruped
See `docs/inventory-tabs-and-spawn.md`, `docs/forest-bestiary-roaming.md`, `docs/lean-cast-v0.3.176.md`, `docs/quadruped-wolf-import-v0.3.182.md`.

## Earlier Android and sprite history
`README.md` records the long version history starting with v0.3.53 and prior builds. Preserve it as historical record; do not rewrite or replace that history with this index.


## v0.3.192 — 2026-11-27
- Replaced grass with 3,028-vertex user source + original PBR; moved original to asset review.
- Added terrain hole underlays, nearby-material edge-only feather shader and in-game 0–30in live saved blend slider.
- Saved fullscreen/window position/size and first-/third-person camera perspective and zoom/FOV.
- Inventory now truly pauses world/AI/clock; resumes on close.
- Added ornate 4-tab equipment/item/material/quest UI with actual player model, actual quest progress, 15 equipment slots and save migration for Secondary, new tests and documentation.

## v0.3.193 — 2026-11-27
- Imported 4 original high-resolution forest-clutter sheets as 48 separated named variants, preserving full-res PBR image bytes and moving originals to Beastiary Review.
- Added 40 active biome-aware instanced clutter types (1,666 decorative placements): grasses, ferns, plants, rocks, leaves, twigs, branches, mushrooms, moss-like growth, roots, pinecones; 8 extra broadleaf variants retained and 4 requested short-grass models remain missing.
- Matched irregular terrain mesh rim elevation and lightly normalized same-type tile edge albedos via world coordinates while retaining per-material blend slider and original tile interiors. Added source/checksum/regression gates and PC visual review.

## v0.3.194 — 2026-11-27 — Inventory visual foundation
- Refreshed functional 4-tab desktop inventory with 10 distinct high-quality custom 9-slice/portrait assets, dark leather panels, crafted bronze trims, responsive selection, clear 15-slot character equipment panel, compact 5e stats, corrected icon grid and selected item descriptions.
- Hide in-game HUD when paused inventory opens; restore on close. Retain player models, save migration and all item/quest gameplay. Added landscape geometry and portrait/HUD regression testing; all local tests passing. Android/Windows release verification separate.

## v0.3.195 — 2026-11-27
- New 512px generated carved bronze panel/slot/button/tab skins, embossed leather and high-resolution stone platform portrait backdrop; keep live game character model and skeleton.
- Connected 11x3 inventory grid, enlarged framed selected-item metadata, decorative upper status strip, clearer layout for four tabs. Correct stale equipment-slot descriptions: Chain Mail -> Body and Longsword -> Main Hand even if a previously selected slot differed. Added screenshot review and regression tests. No gameplay mechanics altered.

## v0.3.196 — 2026-11-27
- Replaced generated Equipment UI imitation with original user's approved bronze/leather artwork, exact pixel crops for menu, stats, inventory grid and details, eight right-sided slots adapted from existing source, retained 15 functional click targets and original 3D character preview. Dynamic inventory selection/equipped item icons, stats and gameplay state unaffected; other three tabs remain on their existing designs. Added source-sha integrity and desktop layout regression gates, real hardware-rendered screenshots and review records.
