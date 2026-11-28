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

## v0.3.201 — playable goblin and modular equipment

Adds the female goblin and female human fitting models, equip/unequip leather, face zoom and hair sizing in the desktop creator, revised inventory and menu artwork, and improved ground blending.

## v0.3.202 local release candidate

The user-supplied Female No Race Base is the only playable character. Height, frame width and body depth are saved and available in the creator. Experimental weight and muscle sliders were removed. Existing character selections resolve to the sole playable model. The female base is unarmored and contains its original 66 joints and ten source animation clips. Creator, saved scale, PC controls, inventory and terrain headless checks passed. Physical device and equipment fit verification remain outstanding.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## v0.3.205 — Creator UI compact styling and pedestal
Actual desktop render validated smaller navigation and arrow borders, stronger active selection, bright slider rails, caption spacing and clean isolated stone stage beneath existing character feet. Original source art, original female 66-joint rig and game mechanics unchanged. Updated stale legacy creator preview regression to dedicated scene. Publication verification separate.

## v0.3.205 publication verification — 2026-11-28
GitHub Actions run 46163037849 completed SUCCESS, including production signing and stable release publication. Release v0.3.205 targets d5a5f4043a08780214c12c885a50cfda55f87ef2. Verified public Android APK (540573185 bytes), Windows x64 ZIP (544121874 bytes), and both live updater manifests at /releases/latest/download/{update.json,windows-update.json}: versionCode 226, versionName 0.3.205, matching release download URLs and SHA-256 digests. Android manifest SHA d5d78ce51abc587981918c7027f62631e94d3a1f3b6341ebd72ce34607d83acf; Windows manifest SHA bd64bd7de555672e09fa9a176f71138e7d184f6c528310ebaec7024ef83039ea. On-device visual/UI operation remains unverified.

## v0.3.206 — Head-fitted modular hair/beard and XYZ placement
- Scalp crown / chin aligned to actual head bone using asset-specific mesh upper bounds; three supplied hair variants and full beard follow the existing 66-joint female rig without modifying it. Hair/Beard panels consolidated and have X/Y/Z centimeter nudges plus Reset; Hair Size now visually matches the gold control family. Positions saved via appearance.cfg and reused by game/inventory. Creator preview lighting softened; original source maps unchanged.
- Ten local Godot 4.5.1 smoke tests passed, four real rendered variant screenshots captured. Physical/device/publication checks reported separately.

## v0.3.206 publication verification — 2026-11-28
GitHub Actions run 46165658216 (Godot Android and Windows) completed SUCCESS. The production signing identity passed the existing release signature verification, and stable release v0.3.206 targets source commit d9033a5ea3dd2429080ff08c500fc28fb2760bbd. Verified public APK Android-DnD-Roguelike.apk size 540581920 bytes SHA256 e95625c6ae8e63dca77dbdda9b7ef53382e2c05ea2d9bcf382b89249a2759969; Windows ZIP Forests-of-Hell-Windows-x64.zip size 544131935 bytes SHA256 44cdfbc5d8343656fc258512de41b1fc0872a275899af368dc41c26e9018f024. Verified public latest/download/update.json and latest/download/windows-update.json: versionCode 227, versionName 0.3.206, exact release download URLs and matching APK/ZIP SHA256 checksums. All 10 local Godot 4.5.1 creator/inventory tests passed. Visual check on physical Android device remains unverified; rigid unrigged hair strands have no independent deformation.

## v0.3.207 — independent hair fitting and stable style switching
- Fixed Wavy offsets/size wrongly carrying into Short/Braid while labels remained stale. Each style now saves and restores its own X/Y/Z and Hair Size; the previous configuration migrates only to the selected hairstyle. Existing head BoneAttachment remains stable through selection; only changed mesh is swapped, avoiding an initial ground-origin flash. Slider saves immediately. Source assets unchanged.
- Godot 4.5.1: 11/11 local creator/inventory/camera tests passed; real OpenGL screenshots checked. Android/Windows published release checks separate.

## v0.3.207 publication verification — 2026-11-28
GitHub Actions Godot Android and Windows workflow run 46169088954 completed SUCCESS, including production signing/identity verification and stable release publication. Release v0.3.207 targets source commit ae74b31d604c80c733c1b014f610fbbdb0e4cdb7. Verified public Android-DnD-Roguelike.apk size 540590301 bytes SHA256 cef2fe3d8a205b551a37dbe9a2d3aac2f3bc16f96135271e72f51784a48063c8 and Windows ZIP Forests-of-Hell-Windows-x64.zip size 544138520 bytes SHA256 a6a9d46144a1c788649a02d3362bdae00872f1bc868f063f7a02ebae48ca6512. Verified both live /releases/latest/download/update.json and /releases/latest/download/windows-update.json return versionCode 228, versionName 0.3.207, matching exact public release URLs and checksums. Local Godot4.5.1 11/11 regression smokes and 1673x940 GPU screenshots passed. Physical Android device/touch and walking hair behavior remain unverified; supplied hair is static and rigidly follows the real head bone. No original skeleton, bone maps, animations, texture quality or source GLBs changed.

## v0.3.208 — 2026-11-28
- Added 12 creator hair color presets, a custom hair color picker, immediate model tint and persistent color in world/inventory, while retaining exact hairstyle size/XYZ settings and original hair textures.

## v0.3.209 — 2026-11-28`n- Replaced overlapping Hair Color swatch grid with compact named presets and a custom color picker, preserving color persistence and per-style fit.`n