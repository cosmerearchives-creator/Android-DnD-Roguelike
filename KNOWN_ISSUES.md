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
