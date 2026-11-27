# Forests of Hell v0.3.185 — Android + Windows

## Shared project and publishing

One Godot 4.4.1 project and one main branch. The existing signed Android APK/package ID and portrait HUD remain unchanged. The release pipeline also exports a Windows x64 application and packages its executable and optional .pck file into a ZIP attached to the same GitHub release.

## Windows gameplay

- Landscape startup window (1600 x 900 physical pixels), resizeable with Godot expand layout.
- WASD moves relative to the camera, Shift runs, Space jumps, C toggles sneak.
- Left mouse click performs a normal melee attack, retaining the existing 1-second cooldown.
- Right mouse button drag orbits the third-person camera in yaw and pitch.
- V toggles first-person and third-person. First person has eye-height positioning and uses an in-memory filtered version of the original skinned character mesh, removing the head only to prevent obstruction while preserving its animated chest, legs and feet when looking down. Returning to third person restores the original mesh.
- I toggles inventory. Q opens it directly on Quest, or switches an already-open inventory to Quest.
- Esc closes the inventory or toggles the top menu.
- F toggles auto attack and Tab toggles target lock.
- PC HUD retains top icons for lock/auto/menu while hiding the entire mobile control panel. Inventory fits a landscape layout with equipment, item list, sidebar and quest details.
- Windows "Check for updates" opens the release page rather than incorrectly downloading an Android APK. Desktop downloads are manual for now.

## Validation

Run:
godot --headless --path godot --script res://tests/lean_cast_smoke.gd
godot --headless --path godot --script res://tests/inventory_smoke.gd
godot --headless --path godot --script res://tests/inventory_tabs_smoke.gd
FOH_TEST_PC=1 godot --headless --path godot --script res://tests/pc_platform_smoke.gd

Generated environment textures and imported creature GLBs remain unchanged. No alternate branches or apps are introduced.
