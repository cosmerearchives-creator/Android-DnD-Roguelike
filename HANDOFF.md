# Development handoff — 2026-11-27 documentation-only audit

## Source of truth and resume point
Repository: https://github.com/cosmerearchives-creator/Android-DnD-Roguelike; working directory on Stormfather: `G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource`; branch `main`. Before this work: commit `ca48539a876ccf6f9355bbc11f1d89788abd1202`, version `0.3.188`, code `209`. Remote `origin` points to the same GitHub repo. This documentation commit makes **no game, engine, texture, skeleton or gameplay edits**.

## What was inspected
Root: `AGENTS.md` (previous eight-line starter instructions, expanded without removing signing/version safeguards), `README.md` historical Android changelog, `version.properties`, `godot/project.godot`, Godot scene/script/test filenames, CI workflow paths and seven tracked GLBs. Existing documentation: `docs/roadmap-world.md`, `roadmap-asset-pipeline.md`, `world-development.md`, `racial-backlog.md`, `character-pipeline.md`, `inventory-tabs-and-spawn.md`, `forest-bestiary-roaming.md`, `combat-foundation-v0.3.184.md`, and PC v0.3.185/v0.3.186 notes.

## Evidence and caution
Source files indicate current Godot PC/Android menu/world/bestiary/inventory, enemy spawning/combat, updater/fullscreen/loading and smoke scripts. Prior documentation contains **historical and sometimes conflicting** descriptions of source-generated skeletons, temporary rigs, texture compression and legacy Java world rules. New user restrictions in `AGENTS.md` override earlier recommendations **prospectively**. This audit did not prove live 5e coverage, user-provided skeleton provenance, texture fidelity, a game run, successful CI or downloadable binary. Detailed status: `IMPLEMENTATION_STATUS.md`; next work: `ROADMAP.md`, `KNOWN_ISSUES.md`, `ASSET_MANIFEST.md`.

## Existing untracked files — deliberately untouched
`godot/assets/`: `boulder.glb`, `broadleaf.glb`, `pine.glb`; `fire_beetle_texture_0.png`, `goblin_warrior_normal.png`, `goblin_warrior_texture_0.png`, `goblin_warrior_texture_0_metallic_roughness.png`, `human_adventurer_Image_0.jpg`–`_2.jpg`, `male_human_Image_0.jpg`, `male_human_Image_1.png`, `male_human_Image_2.png`, `twig_blight_texture_0.png`, `wolf_quadruped_0.jpg`. `scripts/`: `apply_bestiary_batch.py`, `apply_lean_cast_migration.py`, `audit_goblin_reference.py`, `inspect_glb_content.py`, `inspect_incoming_blender.py`, `inspect_incoming_glbs.py`, `inspect_katie_preview.py`, `preview_candidate.py`. Do not stage them by accident.

## Handoff acceptance and next actions
1. Confirm **only nine documentation paths** staged/committed and remote `origin/main` matches the new local commit.
2. For any future asset integration, FIRST verify appropriate user-provided skeleton and complete bone maps; **stop all development** if unavailable/incompatible. Avoid existing rig-generation and texture-compression tools absent express approval.
3. Audit actual Godot state vs legacy world/race descriptions; run smoke tests, PC/Android device checks and GitHub release validation separately. Never represent this documentation-only audit as runtime verification.
4. Maintain all nine root records and relevant `docs/` after each publishing session. No duplicate tasks, no unknowns silently marked complete.

## Commit verification
This section is populated once the remote push and SHA check complete.

## 2026-11-27 continuation — male leather armor v0.3.190
The historical 0.3.188 audit above remains intact. At implementation time the latest prior commit was `8d717ea` (0.3.189 / versionCode 210). New implementation candidate: versionName 0.3.190 / versionCode 211, new `godot/assets/male_human_leather_armored.glb` and complete source `blender/leather_armor/`, plus world/inventory equip visual switching and animation lab target. The original male GLB hash `f5211616730dc2fa1a68a35bcf6bf85f31575a9bfff96142a41b1a7d63a64c48` is preserved; original female source stays untouched in the Windows Leather Armor Review folder. Tests documented in `TEST_CHECKLIST.md`. Before claiming publication verify GitHub workflow artifacts, release/version matching and the updater manifest. Unrelated untracked models and scripts remain untouched. Local editable review and renders are at `G:\ChatGPT Projects\Forests of Hell\Male Leather Armor Review`.
