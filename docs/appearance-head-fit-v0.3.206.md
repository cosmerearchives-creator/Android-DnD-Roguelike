# Character appearance head-fitting and workshop controls — v0.3.206

## Scope and evidence
Approved creator refinement using **only existing user-supplied** Female No Race Base and the four imported hair/beard GLBs. The before/after screenshot comparison demonstrates that the original Wavy mesh appeared much too tall above the model's head. Asset inspection used the original Godot 4.5.1 PackedScenes and measured each MeshInstance3D AABB. The supplied female playable model contains exactly **66 original joints**, and the head bone is **index 5, mixamorig_Head**. The original `headfront` bone on this model has positive Z in the face-forward direction. No bones, bone maps, animations, material textures, rig rest poses, or source meshes were changed.

## Verified input geometry
| Source | Mesh AABB size X,Y,Z (m) | Role |
|---|---|---|
| appearance/hair_wavy.glb | 0.903253, 1.898732, 1.034521 | static hairstyle mesh |
| appearance/hair_short.glb | 1.501402, 1.573253, 1.903303 | static hairstyle mesh |
| appearance/hair_braid.glb | 1.437447, 1.896282, 1.600468 | static hairstyle mesh |
| appearance/beard_full.glb | 1.473436, 1.906936, 0.674502 | static beard mesh |

The meshes contain no separate supplied skeleton, so they attach as static followers to the user's existing head bone using Godot BoneAttachment3D; this does **not** create a skeleton. The source AABBs' Y and Z dimensions were previously recorded in the wrong order inside `character_appearance.gd`; the code now uses the measured Godot coordinates, including actual mesh upper bounds. Source files and the historical source JSON are preserved as supplied.

## Implemented
- Each hairstyle scales to a 0.27m baseline width (adjusted by Hair Size) and its upper bound is fitted to 0.19m above the existing head-bone origin. The beard scales to a 0.18m baseline width and its upper bound is fitted to 0.03m above the origin, with an independent positive-Z jaw offset. These are presentation fitting presets relative to the measured user rig, not changes to bones or D&D rules.
- Position controls for Hair **and** Beard: X/Y/Z adjustments in increments of 0.01m, clamped to ±0.25m; reset restores the correct fitted default, not the mesh origin.
- Offsets auto-save to the existing `user://appearance.cfg`; the existing `APPEARANCE.apply` reads them in gameplay and inventory. Adjusting the offsets or Hair Size updates the current BoneAttachment child transform in place, without reloading large GLB assets every click. Selection changes load the chosen model as before.
- Hair and Beard use a dedicated single right-hand panel, avoiding repeated empty Body frames; Hair has a matching gold diamond Hair Size slider, and Beard has its own XYZ+Reset controls.
- Preview illumination was softened (key energy 2.0 to 1.45, fill 0.85 to 0.60, ambient 0.75 to 0.65). This does not modify textures.
- Existing Race/Subrace/Class/etc top navigation, Back/Next-to-game, Body controls, world systems and inventory remain unchanged.

## Tests and review
Local Godot 4.5.1 results: **10/10 passed**: `creator_workshop_smoke`, `creator_v204_scale_smoke`, `creator_v205_ui_smoke`, `creator_preview_smoke`, `character_appearance_smoke`, `appearance_head_fit_smoke`, `appearance_xyz_ui_smoke`, `inventory_desktop_layout_smoke` (using the documented `FOH_TEST_PC=1` headless environment), `inventory_pause_and_blend_smoke` and `display_camera_persistence_smoke`. The legacy creator tests were updated only for the added panel and persistent user appearance state. The attachment test verifies all three hairstyles and beard fit, unchanged 66 bone names/parents, head rotation following, and offset mutation. The XYZ test verifies category layouts, on-model movement, save reload and Reset.

Actual OpenGL screenshots from Stormfather are stored in `G:/ChatGPT Projects/Forests of Hell/Creator Review/hair_v206_{wavy,short,braid}.png`, `beard_v206.png` and the comparison image `appearance_v206_compare.jpg`. Android/Windows signed build publication and actual device usability must be checked independently.

## Known limitations
These hair/beard sources are **unrigged static meshes**. They now follow the head rigidly, but long hair strands cannot individually deform, swing, or avoid shoulder/chest clipping during animations without user-supplied compatible rigged hair assets. Oversized Hair Size values and large XYZ offsets can produce deliberate clipping; controls allow fitting adjustments. Male playable model and additional Appearance categories remain blocked on their own required supplied assets and are not silently implemented. Existing game saves not migrated beyond the appearance configuration.

## v0.3.206 publication verification — 2026-11-28
GitHub Actions run 46165658216 (Godot Android and Windows) completed SUCCESS. The production signing identity passed the existing release signature verification, and stable release v0.3.206 targets source commit d9033a5ea3dd2429080ff08c500fc28fb2760bbd. Verified public APK Android-DnD-Roguelike.apk size 540581920 bytes SHA256 e95625c6ae8e63dca77dbdda9b7ef53382e2c05ea2d9bcf382b89249a2759969; Windows ZIP Forests-of-Hell-Windows-x64.zip size 544131935 bytes SHA256 44cdfbc5d8343656fc258512de41b1fc0872a275899af368dc41c26e9018f024. Verified public latest/download/update.json and latest/download/windows-update.json: versionCode 227, versionName 0.3.206, exact release download URLs and matching APK/ZIP SHA256 checksums. All 10 local Godot 4.5.1 creator/inventory tests passed. Visual check on physical Android device remains unverified; rigid unrigged hair strands have no independent deformation.
