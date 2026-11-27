# Known issues and verification gaps
Entries below distinguish **unverified** from **reproduced defects**. No fresh game run or hardware testing occurred in this docs-only audit.

| Status | Issue/gap | Next evidence/action |
|---|---|---|
| Mandatory validation gap | User-provided skeleton provenance and completeness of **all** bone maps not verified across tracked GLBs; historical docs describe generated/estimated rigs | Inventory original supplied skeletons/maps, compare SHA/parentage/rest transforms and imported mappings; **stop development** if an appropriate skeleton is unavailable/incompatible |
| Mandatory validation gap | Original textures vs Godot import format, sizes and material maps not verified; local untracked texture companions exist | Compare source/output/channel list, inspect import flags; do not compress/downscale without approval |
| Not verified | Windows updater, F11 from all window states, free mouse camera and forest progress/loading on real PC | Run launcher and update workflow smoke; confirm manifest/version and before/after behavior |
| Not verified | Android signed release, installer, updater/downgrade and performance with mixed enemies | CI release asset/certificate checks and real Android device playthrough |
| Known partial (documented) | D&D 5e engine not complete: traits, initiative/action rules, rest, spellcasting; provisional 1s attacks/player stats | Write rules tests then connect real sheet/item/race data |
| Known partial (documented) | Inventory Use/material catalog, equipment gameplay visuals, quest rewards/markers and full persistence | Audit runtime bindings and verify targeted tests |
| Historical/current mismatch | Old Android `OutdoorActivity` clock, bed, drow/shade/visibility notes may not represent current Godot scene | Audit source and actual Godot gameplay, label each feature honestly |
| Historical review outstanding | Older bestiary notes report algorithmic skin weights, facing/scale and device deformation concerns | Compare against mandatory user-skeleton policy, visually validate in game before any future model changes |
| Not verified | Forest first-entry hitch improvements beyond single benchmark, performance on target Android/PC hardware | Repeat cold/warm startup measurements and FPS capture |
| Workspace hygiene | Local pre-existing untracked model textures, environmental GLBs and Python scripts | Keep out of documentation commit; owner must decide future integration explicitly |

Update status only with evidence; an old bug note or old test pass is not automatically a current reproduced issue.

## Male fitted leather armor — outstanding release/device gates
The male Leather Armor variant passes Blender rest/running previews and Godot headless checks. **Not yet hardware-verified:** PC/Android onscreen body/cloth overlap during full locomotion/combat, first-person near-camera occlusion with added armor surfaces, frame-time/memory increase from the extra skinned mesh, and signed APK/PC release assets. Godot `pc_platform_smoke.gd` passed but produced shutdown RID-leak warnings; classify those as a regression-monitoring warning rather than a demonstrated new armor defect. No materials were downscaled. The old user's original armor/rig files are unchanged.


## v0.3.191 terrain/tavern follow-ups
Imported tiles are assembled into patches, but neighboring material types still have recognizable polygon/square boundaries; the current shader feathers only the exposed OUTER perimeter, not biome-to-biome seamless transition. The original source grass model is too dense (1.9M vertices) for repeated rendering; its full-resolution PBR maps are reused on a simple plane, with emissive intentionally disabled; assess lighting consistency with future natural terrain layers.
Tavern is inherently an open-roof diorama with one complex 415,499-vertex mesh and 4096px texture channels. Six simple collider volumes approximate walls and hearth; test edge alcoves and other playable races. Android device GPU memory, FPS, APK size and actual 8ft character visual test are NOT yet measured (only physical capsule test and PC graphical preview).
Godot 4.5.1 on Stormfather: `--headless --path ... --quit-after` main-scene boot produced renderer dummy RID/preload errors and one crash, while the separate graphical compatibility launch exited successfully, and 9 headless scripted smoke tests passed. Confirm GitHub CI's Godot 4.4.1 standard boot and export independently. Existing headless smoke shutdown RID-leak warnings remain visible.


## v0.3.192 known visual/feature gaps
- Adjacent original terrain mesh sources have dissimilar UV maps and irregular PBR patches; the new real-time 0-30in edge-only crossfade and square earth underlay reduce bright gaps but do not fully remove straight material boundaries, texture-atlas discontinuities, or visual seams. Further topdown texture baking / mesh continuity should be considered.
- Android device FPS and GPU impact of six neighboring sampled textures not benchmarked. Windows GTX1080Ti graphical smoke passes.
- Equipment UI displays actual character and implemented stats only; unimplemented six-ability-score values/proficiencies show absent/unrecorded, and Quest/Mats tabs don't fabricate unavailable game content. World Drop and Use disabled pending real persistent-world item behavior; Compare is preliminary name/weight.
- Custom ornate styling closely follows references structurally but not pixel-for-pixel; mobile UI retains portrait orientation and uses the same ornamental skin. Verify on target Android hardware.

## v0.3.193 unresolved ground-cover and tile matters
- **Missing four short-grass variants:** the provided ground-plants GLBs contain tall grasses, ferns and broadleaf foliage but nothing confidently classifiable as four short-grass clusters. Do not silently substitute other categories. Request appropriate source sheet later.
- Some pale beige moss-like pieces and green/white-edged broadleaf models from generated sources look less natural than the target; original geometry and maps retained faithfully, should replace with better inputs rather than fabricate corrections.
- Ground tiles still show square grid seam edges in Godot screenshot despite world-space edge sampling, rim elevation matching and added scatter. Per-model UV atlas is non-seamless; future continuous mesh + derived topdown/world-space texture blending likely needed. Overlay strips were tested and rejected due worse visual grid.
- Android device memory/FPS and alpha/material artifacts of 40 concurrent MultiMesh types / 1,666 visible placements have not been measured; smaller area-specific streaming or culling may be appropriate later without reducing texture fidelity.

## v0.3.194 inventory art limitations
- The 4-tab UI now has specific textured frames, less repeated ornamentation, opaque background, readable stat columns, functional slots, stable bounded item icons and proper pause/HUD behavior, but is not an exact recreation of the more intricate approved image references. Generated copper/bronze and stone textures are a functional reusable baseline, not all the intended original high-resolution medieval engraved art; compare screenshot under G:/ChatGPT Projects/Forests of Hell/Inventory Review with user reference for followup.
- Only known 5e/item and character values are displayed; unimplemented 5e proficiency/ability fields aren't invented. Materials and quests reflect real collected game data, which is currently sparse. Item Drop/Use remain disabled absent matching gameplay systems. Android mobile portrait and landscape performance remain physically unverified.
