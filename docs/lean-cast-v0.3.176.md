# Lean Cast / Single Goblin Cleanup — v0.3.176

## Current production creature assets

Only the following characters are shipped by Godot:

- `godot/assets/human_adventurer.glb`: Human Adventurer, from the user's
  `Meshy_AI_Female_Character_Model_20261127_animated_mixamo_All_animation.glb`.
  Retains one Mixamo skeleton (66 bones), reduced to ~85k triangles and 2K
  textures. Body mesh has **no embedded animation clips**; it uses the shared
  humanoid library.
- `godot/assets/goblin_warrior.glb`: the *new*
  `Meshy_AI_goblin_warrior_character_20261127_animated_mixamo_Axe_Spin_Attack.glb`.
  Retains one Mixamo-style skeleton (72 bones), original ~10k triangle geometry,
  2K albedo and normal textures, 1K metallic/roughness and the native
  `Axe_Spin_Attack` clip. Also uses the shared humanoid framework for all
  baseline locomotion and attacks.
- `godot/assets/fire_beetle.glb`: existing original rigged Fire Beetle, retaining
  its unique nonhumanoid walking animation.
- `godot/assets/humanoid_mixamo_animation_library.glb`: original 20 shared
  Mixamo humanoid animations plus one inexpensive generated `Idle` hold pose
  created at runtime by `humanoid_shared_animations.gd`.
- `pine.glb`, `broadleaf.glb`, `boulder.glb`: environment geometry only.

The shared animation mapper handles `mixamorig:` and `mixamorig_` naming
as well as the earlier reduced skeleton conventions. In the local Godot smoke
test, the current Human mapped **60/66 bones**, while the current goblin mapped
**20/72** (its nonstandard extra finger/prop bones have not been retargeted).
Both load all 20 original shared clips plus generated Idle.

## File size reduction

| Metric | Before | After |
| --- | ---: | ---: |
| Local game `godot/assets` file bytes | 271,170,302 | ~26,804,442 |
| Human character GLB | Original 27,416,152 source | 4,801,320 |
| New goblin GLB | Original 52,347,600 source | 10,742,016 |
| Shared animation GLB | No duplicate character | 5,140,288 |

Approximately **90% smaller** asset directory, preserving the original
source files in a separate library. 115 retired model, pose, rig, old fitted
armor and copied character-texture files (~252.5 MB from the working folder
at retirement) were archived outside the game.

**No archived or retired characters should ship in the next APK.**
The GitHub workflow exports only environment geometry and runs
`scripts/verify_lean_cast.py` before compiling. This guards the seven allowed
GLBs, verifies the source skeleton and animation inventory, blocks old Katie
textures and Blender files, and checks that asset-folder bytes remain under
45 MB. `godot/tests/lean_cast_smoke.gd` verifies Bestiary, Animation Lab,
world player, inventory portrait, enemy goblin animations and axe clip.

## Archives (never deleted)

Original source GLBs, unchanged:

`G:\ChatGPT Projects\Forests of Hell\Master Asset Library\Creature Sources\`

Removed/retired built assets and SHA256 manifest:

`G:\ChatGPT Projects\Forests of Hell\Master Asset Library\Retired Character Assets\before-v0.3.176\`

Unapproved incoming originals, including these two new source characters:

`G:\ChatGPT Projects\Forests of Hell\Beastiary Review\`

No models moved to `Beastiary Complete` automatically. Existing source
files in other folders were not deleted. Licenses and original credits must
be retained; model-specific visual approval still remains.

## Future imports

1. Preserve and hash the unchanged source model.
2. Identify humanoid skeleton and separate source mesh from animation tracks.
3. Use the existing shared humanoid framework for all humanoids; keep unique
   native clips (e.g., Axe Spin) as optional named extensions.
4. Re-export the skinned character without duplicating the 20 shared clips.
   `scripts/prepare_lean_cast.py` reconstructs the two current models in
   Blender, then `scripts/optimize_glb_textures.py` repacks their images to
   eliminate unnecessarily large embedded originals.
5. Validate the core rig mapping, equipment and appearance in Animation Lab
   and Bestiary; make measured quality changes, not blanket reductions.
6. Run `scripts/verify_lean_cast.py` and `godot/tests/lean_cast_smoke.gd`.
7. Move successful sources to `Beastiary Review`, publish to `main`, and
   await explicit user approval before `Beastiary Complete`.

## Limitations and intentionally preserved behavior

The existing player inventory, equipment-slot metadata, items, materials and
quests remain. The former *Female Base-specific fitted leather armor mesh*
was removed with that obsolete rig; an appropriately weighted version is
needed to render armor on the new human. The equipment item itself remains.

The Fire Beetle is not a humanoid and retains its own animation clips.
Bestiary now rescales sub-centimeter imported beetle geometry for a usable
preview without changing its world-collision dimensions.

This change removes goblin variants intentionally. Random spawns now choose
between the **new Goblin Warrior** and **Fire Beetle** only. Their movement,
collision, spawn spacing and Bestiary selection remain intact. On-device
appearance, FPS and hands/weapon alignment must still be visually reviewed.
