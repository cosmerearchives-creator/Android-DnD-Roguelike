# v0.3.182 — Wolf quadruped reference, creature sizing and animation naming

## Quadruped reference: Wolf

- The original source is **Wolf - Complete With Quadraped Animation Library.glb**, imported without resampling, texture compression, mesh decimation or rig reconstruction.
- Source SHA-256: `137525d12db941491ba5b7b254ffc0920ce3adbaff15f9d2a2a6dc4e873481c4`.
- **46 original skin bones**, one skinned mesh, 12,305 source vertices, **four native animations** with **138 channels per clip** (46 joints × position/rotation/scale).
- The full bone hierarchy, reference rest transforms and source-to-canonical clip names are recorded in `docs/quadruped-wolf-reference.json`.
- Original bytes are archived in `Master Asset Library/Creature Sources` and `Master Asset Library/Animation Sources`; successfully imported source is in `Beastiary Review` pending explicit visual approval, **not** `Beastiary Complete`.
- Runtime `quadruped_shared_animations.gd` is separate from `humanoid_shared_animations.gd`. It validates all 46 names, parent hierarchy, rest transforms and native clip presence before attaching; incompatible rigs fail instead of accepting a partial match. The wolf retains its unmodified source clips and adds canonical aliases.
- A separate Quadruped Animation Lab and a Wolf Bestiary page allow inspecting all four original motions.
- Future non-identical quadruped rest rigs require explicit verified retargeting; do not silently apply the wolf clips.
- `scripts/validate_quadruped_import.py` is a **mandatory CI build gate**. It validates original SHA, source rig, skin, inverse binds and all tracks against the committed reference manifest.

### Wolf source-to-canonical animation names

| Native source | Canonical runtime alias |
|---|---|
| `dog_idle1` | `Idle_Standing_01_Standard` |
| `dog_run` | `Movement_Run_01_Standard` |
| `dog_dog_over` | `Action_RollOver_01_Standard` |
| `leopard_skill_feipu` | `Attack_Pounce_01_Standard` |

## Universal humanoid attack naming

The existing format is `Category_Action_Variant_Descriptor`. One action convention covers both blunt and slashing overhead melee hits.

| Source animation | Canonical runtime name | Use |
|---|---|---|
| `Archery_Shot` | `Attack_BowTwoHanded_01_Standard` | Two-handed bow |
| `Attack` | `Attack_MeleeTwoHanded_01_OverheadSmash` | Overhead blunt **or** slashing hit |
| `Cowboy_Quick_Draw_Shooting` | `Unused_Attack_FirearmOneHanded_01_QuickDraw` | Future-only one-handed firearm |
| `Crossbow` | `Attack_CrossbowTwoHanded_01_Standard` | Two-handed crossbow |

Canonical names are additional aliases to duplicated animation resources at runtime. The archived native GLB, original source animation names and keyframes are preserved. The Humanoid Animation Lab presents canonical names; `Unused_` animations remain inspectable but are not called by ordinary combat.

## Beetle sizing and forest spawning

- Root cause: the original beetle glTF mesh AABB is only about **0.00007 units** tall; its actual skeleton spans much farther. Using the tiny mesh AABB inflated preview and world scale.
- Bestiary framing and forest size now use the imported **skeletal rest bounds** for this creature. Automated tests assert the Bestiary beetle spans about **0.65 game-world units** after normalization and the world beetle about **0.52**.
- An existing minimum-height guard had prevented tiny beetle model normalization in the forest; now the beetle uses an appropriately small source-unit threshold.
- Every fresh forest population attempts to spawn a **Fire Beetle** and **Twig Blight**, then additional randomized enemies farther from the starting location.
- Forest Beetles receive a visible `SPAWNTEST` world label and group marker to flag that their habitat placement is under test.
- Twig Blights are valid woodland creatures and therefore spawn without `SPAWNTEST`; goblin-quest progress still increments **only** on Goblin Warrior defeat.
- The Fire Beetle uses a **27-bone custom rig**, not the wolf's 46-bone quadruped skeleton. It continues playing its native `Walk` animation.

## Testing-phase combat cadence

- Player attack cooldown is **1.0 second**. Attack animation duration remains independent.
- CI integration tests cover the new canonical names, one-second cooldown, beetle framing and world size, `SPAWNTEST` label, forest-native Twig Blight behavior, wolf Bestiary, wolf Quadruped Animation Lab, and the strict quadruped skeleton contract.
- Actual Godot OpenGL previews were captured locally on the NVIDIA GTX 1080 Ti for beetle and wolf.
- Wolf models are available for inspection in Bestiary and Quadruped Animation Lab; **wolf enemy AI/spawning was not enabled** in this pass.
