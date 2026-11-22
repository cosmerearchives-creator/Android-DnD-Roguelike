# Blender character pipeline

The `blender/create_character.py` script runs headlessly in the dedicated GitHub Actions Blender workflow and generates an editable Petite Pale Elf `.blend` project plus a four-direction idle atlas. The model is a prototype and is **not** integrated into the Android renderer. The current in-game Petite Pale appearance still uses the existing 2D articulated assets. Next steps: verify Blender render, refine anatomy/hair/outfit against character reference, add armature and walk/punch bakes, and integrate only after asset validation.
