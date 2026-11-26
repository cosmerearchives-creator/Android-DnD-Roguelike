# Goblin Warrior — bestiary review candidate

- Source: `goblin_warrior.glb` in the separate `Beastiary Imports` / `Beastiary Review` directories.
- In-game GLB: `godot/assets/goblin_warrior.glb`, used for the world goblin and bestiary preview.
- Source mesh and embedded material/texture retained. The rig adds 15 bones with spatially inferred skin weights; no topology reduction.
- Blender workflow: `blender -b --python scripts/rig_goblin_warrior.py` (requires original source file on the authorized computer).
- Animations: Idle, Walk, Attack. Bestiary has review buttons for each and a rotatable preview.
- Goblin statistics are unchanged (2014 5e goblin CR 1/4). Gameplay goblin remains stationary, with idle animation. The other two clips are accessible from the bestiary, not new combat behavior.
- Collision capsule matched to the approximately 1-metre-high model.
- Tested: Blender 5.3 renders of rest/walk/attack; isolated Godot 4.4.1 imports skeleton, all three clips, and instantiates the bestiary; world script compiles.
- **Approval: pending user visual review.** Do not move the source to Beastiary Complete until explicitly approved.
