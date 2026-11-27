# Shared Humanoid Animation Library — v0.3.175

## Original source and provenance

- Original supplied asset: `Meshy_AI_Female_Character_Model_20261127_animated_mixamo_All_animation.glb`
- Preserved unmodified at:
  `G:\ChatGPT Projects\Forests of Hell\Master Asset Library\Animation Sources\Meshy_AI_Female_Character_Model_20261127_animated_mixamo_All_animation.glb`
- Original still in Downloads; the original was copied rather than moved.
- Original SHA-256:
  `119496A7536E41FCA167B8F1CE6162883CC37BF8D4DB775B4322EB59362AAC68`
- Source size: 27,416,152 bytes.
- Verify distribution permissions/license terms separately; this GLB did not establish a licensing grant.

## Extracted library

- Game asset: `godot/assets/humanoid_mixamo_animation_library.glb`
- Archived game-ready copy: `Master Asset Library\Animations\humanoid_mixamo_animation_library.glb`.
- Size: **5,140,288 bytes**; **81.3% smaller** than the original source GLB.
- 20 clips, 66 skeleton joints, 3,840 original animation channels retained; inverse-bind matrices intact.
- Original large character mesh, textures and materials omitted from the game-ready animation asset.
- Tiny three-vertex skinned marker remains solely to make Godot import the skeleton as `Skeleton3D`.
- Reproducible extraction: `python scripts/extract_mixamo_animation_library.py SOURCE.glb godot/assets/humanoid_mixamo_animation_library.glb`.
- Keep the original archived file unmodified. Don't ship it as a duplicate character asset in the APK.

## Available source clip names

`Running`, `Walking`, `Push`, `Crossbow`, `Throw`,
`_1_hand_cast`, `Self_Cast`, `Concentrate_Idle`, `pick_up`,
`LAy_Down`, `Archery_Shot`, `Attack`,
`Cowboy_Quick_Draw_Shooting`, `Left_Jab_from_Guard`,
`Regular_Jump`, `Right_Hand_Sword_Slash`, `Sneaky_Walk`,
`Step_in_High_Kick`, `Talk_with_Left_Hand_on_Hip`, `Walking_Woman`.

## Runtime adapter / shared framework

`godot/humanoid_shared_animations.gd` dynamically retargets all 20 animation tracks
from the Mixamo source to a destination `Skeleton3D`. It maps common
`mixamorig_*` bones onto both our reduced Goblin Warrior rig and our
Female Base rig, with automatic preservation of existing native animation clips.

- Matches known bone naming conventions, not visual proportions alone.
- Applies source-relative rotation deltas through source and target *global rest*
  frames, then restores the target local bind-space rotation.
- Scales the hip translation for proportions, discards unwanted X/Z root
  movement by default, and preserves vertical motion such as jumps.
- Rejects non-rigged meshes and rigs with fewer than eight matching bones.
- Chooses loop playback for `Running`, `Walking`, `Walking_Woman`,
  `Sneaky_Walk` and `Concentrate_Idle`; other actions play once.
- Does not silently overwrite imported `Idle`/`Walk`/`Attack` clips.

Example use with an instantiated and attached humanoid model:

```gdscript
const Shared = preload("res://humanoid_shared_animations.gd")
var player: AnimationPlayer = Shared.attach(character_model)
if player:
    player.play("Walking")  # also: Push, Crossbow, _1_hand_cast, etc.
```

## Review interface and test results

Main Menu → **Animation Lab** provides the same 20 clips on two real models:
Female Base and Goblin Warrior, with play/replay, rotate and zoom. It is a
visual QA environment; it does not replace the existing gameplay controls.

Validated under Godot **4.4.1** in an isolated, copied Godot project:
- Original source and compact extracted GLB both imported.
- Extracted source: 66 skeleton bones; 20 clip names retained.
- Female Base: **21 mapped bones**, all 20 clips created and activated.
- Goblin Warrior: **15 mapped bones**, all 20 clips created and activated.
- Automated playback on several spells, movement, weapon, jump and shove clips:
  pose quaternions finite, no missing-resource errors.
- Full Animation Lab scene test: switch both models, enumerate and play all 20
  clips, select Crossbow; test exited successfully.

## Explicit limitations / next review

The animation library is **reusable**, but not yet visually approved as
production-ready on every model. Validate arm posture, hand gripping,
foot sliding, root location, clipping, blend transitions and Android FPS
on-device before replacing more gameplay animations.

This task **does not** add a skeleton to static Goblin Alts 2/3/4, provide
insect animation retargeting for Fire Beetle, wire new enemy AI, or change
weapons/combat timing. The shared system can be called from gameplay once
those models are rigged and the retarget preview has been approved.

For future performance, share/cache retargeted animation resources when many
creatures of the same rig are on-screen, rather than generating a fresh set
for every actor. Verify provenance and equipment extraction separately;
the original source mesh remains available in the source archive.
