# v0.3.203 Character Creator Workshop UI

User supplied 1673 × 940 reference artwork and 1900 × 1900 transparent raw UI components. The originals are retained byte-for-byte as `godot/creator_ui/workshop_reference.png` and `source_elements.png`. Full-resolution, uncompressed, lossless cutouts reside beside them; extraction scripts under `scripts/extract_creator_ui.py` and `scripts/extract_creator_complete.py`.

The opening menu's New Game now selects `res://creator_workshop.tscn`, a dedicated Appearance workshop with race/subrace/class/background/abilities/appearance/review decorative navigation, left-side Appearance categories, a central live female base model preview with rotation/zoom, and Body controls for a display gender choice, height, frame and weight labels. Hair and Beard select existing modular attachments. Next persists existing `appearance.cfg` values and takes the player into the game; Back returns to the opening menu. Original `female_no_race_base.glb` is reused without any skeleton, bone map, geometry or texture edits. Its 66 original bones and ten original animation clips are retained.

## Limits
- Only the user-supplied female base is playable. The Male button records a display preference; it cannot change the model to male and should not be mistaken for a new supported skeleton.
- Height/frame/weight labels use existing **visual proportions**; weight is not a 2014 D&D mechanical weight system, and no skinning/morph edits were made.
- Race, Subrace, Class, Background, Abilities, and Review are placeholders until their own approved screens are restored. Makeup, skin tone, ears, eyes, body markings also require appropriate compatible assets. They do not falsely modify mesh materials.
- Layout is based on a landscape reference, rendered inside the existing game's viewport (which remains portrait on mobile); physical Android testing for usable scaling/orientation is required.
- The actual button/panel source assets were cropped from the transparent PNG, and existing camp artwork is reused as backdrop; this is a first-pass structural approximation, not a pixel-perfect recreation.
- Local Godot 4.5.1 headless script parser and dedicated workshop smoke validated eight sidebar controls, seven tabs, current model and active preview controls. Native Android 4.4.1 Godot CI, release, PC ZIP, and physical device still require separate verification.

## Regressions
Keep all player skeletons/bone maps/animation clips, 2014 game mechanics, inventory, update and release configuration unchanged. Do not publish incomplete or incompatible asset integration as verified. 
