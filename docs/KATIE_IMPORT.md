# Katie character import (Android / Godot)

Source: **Katie - Version 2.01.zip**, containing **Katie-Project-Red-4K.blend** (Blender 2.76). The file is not included in the repository until converted and visually verified.

## Local conversion on Stormfather

1. Copy the supplied ZIP from ChatGPT to `%USERPROFILE%\Downloads\Katie - Version 2.01.zip` on Stormfather.
2. Double-click `Import Katie.cmd` in this repository's root.
3. Inspect the summary for mesh counts, missing textures, and exported file size.
4. After verifying how the character appears in Godot, commit `godot/assets/katie.glb` to `main`, bump `version.properties`, and publish with the existing signed Android pipeline.

For inspection only:
`blender --background --python blender/import_katie.py -- "C:\path\Katie - Version 2.01.zip" --inspect`

Export defaults: at most 65,000 triangles per mesh; textures capped at 1024 pixels per largest side; selected visible meshes and armatures; embedded glTF rig actions preserved when present. The source is a legacy .blend; old particle hair and unsupported materials may require additional cleanup after inspection. Keep separate base and adjusted source files; do not overwrite the ZIP.

The in-game model selector exposes Katie **only when** the exported `godot/assets/katie.glb` exists. The imported model is normalized to approximately 1.7 m tall and grounded at its lowest geometry. Walk/idle/attack clips are used by name if present; rigging/animations not supplied by the model must be created separately.

## Attribution

The source archive includes Blend Swap asset #69006, released under Creative Commons Attribution 3.0 (CC BY 3.0). Its included credit file attributes the blend files to **Rick Savage**. Attribution must be retained in distributed game credits. The original license is provided in the user's source ZIP and can be found at https://creativecommons.org/licenses/by/3.0/.
