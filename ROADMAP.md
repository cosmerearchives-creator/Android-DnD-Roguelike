# Forests of Hell — consolidated development roadmap
Status baseline: documentation audit 2026-11-27; checkout `ca48539` / `versionName=0.3.188`. **Source evidence is not a live device acceptance test.** Detailed historical plans remain in [docs/roadmap-world.md](docs/roadmap-world.md) and [docs/roadmap-asset-pipeline.md](docs/roadmap-asset-pipeline.md).

## Priorities and gates
1. **Mandatory asset validation — blocked on user input where required.** Catalog all user-provided skeletons and complete bone maps; validate compatibility and actual texture import fidelity for every character/creature. Do not create or substitute skeletons; stop development if the appropriate skeleton is missing/incomplete/incompatible. Acceptance: traceable user-provided source, full mapping, material dependency and runtime visual checks; no quality loss without approval.
2. **Release and PC experience — implemented in source; device/release confirmation pending.** Verify v0.3.188 Windows release, auto-update, mouse-look, F11 fullscreen and first-entry loading with actual builds. See [PC v0.3.186](docs/pc-free-mouse-look-startup-v0.3.186.md) and `godot/pc_fullscreen.gd`, `godot/pc_release_updates.gd`, `godot/forest_loading.gd`. Acceptance: end-to-end PC and Android regression, update detection, version/signature consistency.
3. **Combat/rules — partial.** Existing basic creature AC/HP, dice, target lock, auto attack, chase and attacks need real 2014 5e trait/initiative/action integration and character-sheet stats. Source: [combat foundation](docs/combat-foundation-v0.3.184.md). Acceptance: trait-level tests and real encounters without provisional test stats.
4. **Inventory/equipment/quests — partial.** Four UI tabs and quest progress have source/tests, but consumable Use, item-derived stats, visuals, many gameplay hooks, quest rewards, material content and complete persistence still need validation/implementation. Source: [inventory tabs](docs/inventory-tabs-and-spawn.md) and [world roadmap](docs/roadmap-world.md). Acceptance: every item action tied to gameplay, actual equip visuals and tested saves.
5. **World/races/visibility — partial/historical migration audit needed.** Clock, clear-weather lighting, canopy shade, elf darkvision, sensitivity and bed waiting are documented in historical Android activity code; confirm which survive current Godot frontend. Proposed weather, rest/Trance, drow spells, true stealth/perception and world persistence require source audit. Acceptance: Godot runtime tests and phone playthroughs, not only legacy Java tests.
6. **Encounters and bestiary — partial.** Goblin Warrior, Twig Blight, Fire Beetle, Wolf source models and stats; recent staged spawns. Audit actual spawn mix and AI against current scenes. New models require the user-provided rig gate. Acceptance: scale, facing, spawn distribution, motion, collision and frame-time validation.
7. **Procedural world and performance — ongoing.** Existing environment staging and forest loading optimization; future weather, biomes, settlements, procedural dungeons, save migration and device benchmarks. Acceptance: measured startup/frame times on representative PC/Android devices; no unauthorized texture or geometry quality cuts.
8. **Persistent dev discipline — this documentation milestone.** Maintain status, decisions, bugs, tests and handoff after each update. Acceptance: docs-only main commit with only these nine documentation paths and remote SHA verification.

## Dependencies
Asset provenance and skeleton/bone-map approval -> rig/animation import -> enemy visuals -> combat integration -> spawn/performance regression. Item definitions -> equip state -> model visuals + AC/damage -> saving/quest interaction. Lighting/clock -> weather -> obscuration/race traits -> perception/stealth. CI signing/update metadata -> release -> on-device update testing.

## Classification
Only a successful verified published artifact is **Published**; source presence = **Implemented/unverified** unless a cited test was run. Deferred mechanics are **Approved** or **Proposed**, not finished. Review each work item against these gates.

## Completed implementation candidate — male leather armor v0.3.190
Original female leather set recovered; male fitted armor now assembled on the existing approved 84-bone male skeleton **without altering that rig**, added to the game as an equipment-dependent male visual in the inventory preview and overworld, and selectable in Humanoid Animation Lab. Headless engine import and targeted smoke tests pass. Remaining acceptance: live PC/Android visual comparison, running/jumping/attacking clipping checks on hardware, performance measurements, and successful CI-published binary/update verification. Separate female/current-rig adoption and broader equipment-class visual coverage remain future work.


## Ground terrain and tavern milestone — v0.3.191
BASE LAYER TESTED LOCALLY: six 5ft ground assets (grass/dirt/leaves/moss/mud/rocky), deterministic region around starting hub, six MultiMesh batches, full original PBR sources. Tavern open-roof room placed in cleared approach, floor at grade, wide navigable entry, simplified walls/hearth collision.
NEXT: refine biome transitions and reduce visible square patch boundaries; then independent detail overlays (twigs, pebbles, roots), small vegetation, bushes/trees, lighting and future weather. Full-building roof and interior NPCs/services are future tavern steps; verify Android FPS/memory and visuals before expanding terrain area. Avoid geometry/texture downscale without permission.


## Implemented v0.3.192 (release target)
- Approved single milestone: replace grass, light 0-30in edge blending slider, close gaps with square earth underlays, window/fullscreen + camera persistence, true inventory pause, handcrafted ornate Equipment/Items/Materials/Quests UI, 15 approved equipment slots with Secondary. All are integrated and locally tested; verify published release artifacts. See docs/inventory-terrain-settings-v0.3.192.md.
- Future: complete item usage/drop/stat/proficiency rules, expand actual material and quest data, improve biome UV tile seamlessness and shader mobile performance; these are separate from the completed UI rendering.

## v0.3.193 ground-cover integration
- Implemented/tested: source-audited Blender extraction, 48 model variants with shared untouched 2048px images; 40 instanced model types/1,666 copies by biome, no player-blocking ground-cover collision. Added common rim elevation + same-material edge sampling, retained 6in default adjustable different-type blends. Four originals moved into Review. See docs/ground-cover-and-seams-v0.3.193.md.
- Future: supply 4 authentic short grass variants; replace questionable pale moss/white-edged foliage; expand ground-cover distribution beyond 530-cell starter patch when terrain chunks grow; benchmark phone FPS/memory; ultimately use world-space seamless material baking/continuous chunk mesh to completely eliminate source tile boundaries.

## v0.3.194 — focused inventory visual foundation
- Locally implemented/tested: custom leather/bronze atlas families for major panels, header, tabs, slots and buttons; recreated reference layout hierarchy for all four tabs, actual 3D model stage, readability and opaque backdrop; hide world HUD while inventory pauses game. Retained and smoke-tested real 15-slot equipment, sorting/filtering, item detail, character data, quest progress, controller/keyboard regression coverage.
- Later visual work: actual high-detail bronze scrollwork comparable to all four supplied targets, UI icons and full 5e character stats, all item descriptions/tooltips, expanded quests and materials from real data, and reuse shared skin in shops, dialogues, character creator, settings. Desktop gets visual baseline first; Android portrait skin requires device review.

## v0.3.195 approved UI fidelity update
- Improved large carved bronze panel art, derived material leather background, separate compact rectangular slot/buttons, raised stone-like portrait background, 11×3 contiguous grid and item detail/slot compatibility. Current UI is still not a pixel-perfect recreation of user's target; improve iconography and fully connected frame art through dedicated individual high-quality production assets rather than more procedural raster approximations. Keep game HUD inactive when inventory pauses.

## v0.3.196 Equipment source artwork integration
- Implemented locally: replaced generated approximate Equipment art with real uploaded source, validated original PNG hash and exact section crops, 15-slot adapted right column, interactive native controls and live character. Source atlas fully preserved as reusable foundation. Equipment only; defer Items/Materials/Quests source-art adoption until Equipment review. Future: finish more legible fonts/stat hierarchy, supply individually isolated empty slots/hover variants, additional accessory/waist icons, verify Android device UI and performance. Full details docs/approved-equipment-ui-v0.3.196.md.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## Character creator visual milestone v0.3.205
Completed locally: selected Body/Appearance highlight, smaller corner ornaments, warm visible slider track, isolated pedestal and portrait model alignment. Remaining: device visual approval, true supplied material/morph targets for disabled appearance categories, playable male rig only if supplied, and wider Race/Class/Background/Abilities UI restoration. Do not fabricate skeletons.

## v0.3.206 Appearance head attachment and UI — implementation validated locally
- Current: existing 66-joint female model's mixamorig_Head and headfront bone verified; source-exact static Wavy/Short/Braid/Full Beard mesh bounds used for distinct scalp/chin fitting, native XYZ ±25cm/1cm steps and Reset, saved to existing appearance.cfg. Hair Size has gold track, Hair/Beard use a single category panel, softer preview lighting. Local desktop 10/10 checks pass; Android/PC release and real-device approval separate.
- Remaining: user-supplied compatible rigged hair if independently deforming locks/physics are wanted; first-party male playable asset/rig if gender change should be functional; unavailable appearance categories and remainder of creator tabs. Preserve all source maps.
