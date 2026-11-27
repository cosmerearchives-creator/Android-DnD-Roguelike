# Consolidated change log (non-destructive index)

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
