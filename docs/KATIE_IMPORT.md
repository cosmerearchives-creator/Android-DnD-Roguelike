# Katie character import (Godot Android)

The source archive `Katie - Version 2.01.zip` was retrieved from the user's Google Drive as `Katie-drive.zip` on Stormfather. It contains `Katie-Project-Red-4K.blend` (Blender 2.76) and Blend Swap license information. The original is only a face and bust with a six-bone portrait rig, not a full-body gameplay character.

## Completed conversion

`blender/build_katie.py` reuses the existing Forests of Hell Petite Scout full-body meshes and deterministic walk/punch deformation. It imports and scales Katie's original face, eyes, brows, lashes and scalp; replaces Blender particle hair with mesh strands; resizes embedded maps to 1024 pixels; and produces 14 complete GLB frames (idle, 8 walk, 5 punch). The optimized model uses the game's existing collision capsule and model selector. Godot does not need a full-body armature because it swaps pre-baked GLB pose frames, matching the other game characters.

To regenerate on Stormfather, run `Import Katie.cmd`. It accepts either `%USERPROFILE%\Downloads\Katie-drive.zip` or `%USERPROFILE%\Downloads\Katie - Version 2.01.zip`. Alternatively:

```
blender -b -t 2 --python-exit-code 1 --python blender/build_katie.py -- "C:/path/Katie-drive.zip"
```

`godot/assets/katie.glb`, `godot/assets/katie_walk_0.glb` through `_7`, and `godot/assets/katie_punch_0.glb` through `_4` are committed to `main`. Original full-size ZIP and Blender file remain on the user's computer and are not shipped in the APK or Git repository.

### QA checks

- Verify all 14 GLB assets import, textures render, pose changes animate, feet remain on ground, and existing playable models remain selectable.
- Confirm the APK publishes from the existing production-signed Android CI and its updater receives the new version.
- This is a reconstructed full-body hybrid, not the original artist's intended complete body or rig. Refining proportions, clothing and hair aesthetics can follow user visual feedback.

Attribution and license: `docs/KATIE_ASSET_ATTRIBUTION.md`.
