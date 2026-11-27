# Forests of Hell v0.3.186 ? PC free mouse-look and first-entry loading

## Controls

- PC camera yaw and pitch follow the mouse automatically. No right-button hold is needed.
- Mouse is captured when the game world is active and released when the inventory or menu is open. Closing UI recaptures it; switching back to the front menu restores the pointer.
- Mouse look is suppressed while a UI panel is open. Left click attacks, F toggles auto-attack, Tab locks on, I opens inventory, Q opens quests, V switches third/first person.
- Android touchscreen controls, portrait screen and signed application remain unchanged.

## Startup hitch mitigations

- Before: generated ~260 tree candidates and ~160 rocks into the scene during world `_ready`, then instantiated nine skinned enemies in a single deferred frame.
- Now: world `_ready` queues the exact existing terrain specifications without creating the nodes. `_process` builds at most eight pieces per frame, bounded by a 2.5ms CPU budget. Jobs are sorted near-to-far to prioritize the visible play area. Existing locations, meshes, sizes and collision shapes remain identical.
- Enemy scenes for Fire Beetle, Twig Blight and Goblin Warrior begin threaded resource loading; once ready, initial enemies spawn one at a time at 0.18-second intervals rather than simultaneously. Existing nine-enemy target, future four-second spawn cycle and enemy AI are preserved.
- The main menu background-prefetches the world scene. If the user starts before the scene has loaded, a `Preparing the forest...` label replaces a blocking transition. This preserves Android and Windows platform behavior.
- Headless startup benchmark on Stormfather before optimization: scene load ~306ms, world ready ~405ms, first frame ~1208ms. After environment staging and enemy stagger: scene load ~334ms, world ready ~270ms, first frame ~13ms. Timings are single-run measurements on the developer PC and may differ on actual devices; background enemy and scenery creation continues after the first visible frame.
- A real-render PC prefetch smoke test confirmed background scene load completed in ~319ms and changed to the World scene successfully.

## Regression

- `pc_platform_smoke.gd` verifies free mouse-look without button, mouse release/recapture on UI, left-click attack, body-visible first person, PC layout, shortcut keys.
- `enemy_spawn_smoke.gd` verifies initial stagger and safe spawn points using mocked visual assets.
- Existing lean cast, Android portrait inventory and inventory tab tests retain current requirements.
- CI exports and publishes both a signed Android APK and Windows x64 ZIP from `main`.
