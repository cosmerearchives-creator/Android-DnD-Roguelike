# Asset manifest — verified repository paths, unresolved provenance
Baseline 2026-11-27, `main` `ca48539`. This is an **initial catalog of tracked GLBs**, not an assertion that every supplied source, skeleton, map, animation or texture is validated. Originals and untracked files have not been altered. User-provided compatible skeleton and **every** bone map are mandatory; stop all development until missing/incompatible skeletons are resolved.

| Tracked game-ready file | Family / evidence | Skeleton and bone maps | Texture/material verification | Gate |
|---|---|---|---|---|
| `godot/assets/fire_beetle.glb` | Fire Beetle, legacy notes describe imported skinned Walk | Historical 27 bones (combat doc); verify user-provided source and **all** bone maps | Embedded/external channels, original resolution and import settings unverified | HOLD pending audit |
| `godot/assets/goblin_warrior.glb` | Goblin Warrior, legacy docs describe Idle/Walk/Attack | Historical 15 joints; user-approved rig and complete bone-map provenance not validated now | Untracked texture companion files exist locally; do not stage them automatically | HOLD pending audit |
| `godot/assets/human_adventurer.glb` | Humanoid model | Skeleton identity/map completeness unknown | Local untracked Image_0/1/2 JPG companions; manifest only | HOLD pending audit |
| `godot/assets/humanoid_mixamo_animation_library.glb` | Animation donor | Target skeleton compatibility and all source bone maps unverified | Material fidelity not yet audited | HOLD pending audit |
| `godot/assets/male_human.glb` | Humanoid model | Skeleton identity/map completeness unknown | Local untracked male_human_Image files; verify source originals | HOLD pending audit |
| `godot/assets/twig_blight.glb` | Twig Blight enemy | Skeleton/map provenance unknown | Local untracked twig blight texture companion | HOLD pending audit |
| `godot/assets/wolf_quadruped.glb` | Wolf, quadruped animation references | Skeleton/map provenance unknown | Local untracked wolf image companion | HOLD pending audit |

## Other local assets and scope
The checkout also has **pre-existing untracked** `godot/assets/boulder.glb`, `broadleaf.glb`, `pine.glb`, creature texture image files and several `scripts/` tools (see `HANDOFF.md`). They are **not** in this commit and must not be deleted, modified or assumed published. See `docs/roadmap-asset-pipeline.md` for historical source hashes, rig track counts, imports/review folders and licensing leads. The nearby `G:\ChatGPT Projects\Forests of Hell\Beastiary Imports`, `Beastiary Review`, `Beastiary Complete` and `Master Asset Library` require a separate inventory of source authorship and current file digests.

## Required row-by-row acceptance record for next asset audit
For every future asset, record: stable ID; model/animation source path and SHA256; provenance/license; user-provided skeleton source ID and SHA256; rest pose/hierarchy/bone count; **complete list of bone-map files and bone coverage**; all clips/loops and compatibility evidence; each source texture/material map with size/format/channel and output import settings; in-engine appearance and hardware/frame-time review; explicit user approval; status. Do not invent missing values. If no appropriate user-provided skeleton is available or compatible, **stop development rather than generate a rig**. Preserve original texture resolution and quality; no automatic compression/downscaling except explicit user-requested optimization.

## Fitted male Leather Armor — v0.3.190
- User-approved target skeleton: `godot/assets/male_human.glb` — 84 existing SmartRigArmature bones, full original joint order/parents preserved. Original SHA256: `f5211616730dc2fa1a68a35bcf6bf85f31575a9bfff96142a41b1a7d63a64c48`. No skeleton or bone-map changes, no replacement rig.
- Sources: recovered original female pattern at `C:\Users\thebl\Documents\ChatGPT\Android DnD Roguelike\Leather Armor Review` (unchanged); generated male fitted source `blender/leather_armor/Leather Armor - Male Human.blend`, procedural geometry script `blender/leather_armor/build_leather_armor_male.py`, and standalone skinned armor `blender/leather_armor/leather_armor_male.glb`.
- Game asset: `godot/assets/male_human_leather_armored.glb` — original male GLB binary and Running clip plus 26,908-vertex fitted leather mesh, four original leather material colors, same 84-bone skin; named model selectable in lab and dynamically selected when the male character equips `leather_armor`.
- Material preservation: male Image_0.jpg, Image_1.png, Image_2.png extracted as `male_human_leather_armored_Image_*` and compared **byte-for-byte identical** to the original male external companion images; no reduction or destructive conversion. Leather design uses four scalar PBR materials; no missing armor texture maps.
- Checks: Blender combined-model import and front/rear/running previews PASS; source male binary buffer unchanged; Godot 4.5.1 imported two skinned meshes and all 84 bones; shared animations mapped 65 bones and 40 clips; inventory portrait smoke PASS. Full PC/Android physical visual and frame-time review pending.


## v0.3.191 — six ground types and original tavern room (2026-11-27)
Sources: `G:\ChatGPT Projects\Forests of Hell\Beastiary Imports\` contains `dirtground.glb`, `leafground.glb`, `mossground.glb`, `muddyGRound.glb`, `RockyGRound.glb` and `Meshy_AI_Medieval_Tavern_Room_Diorama_20261127_image-to-3d-texture.glb`. Original source `Background Assets\Meshy_AI_grass_texture(1).glb` is 119,079,144 bytes and has 1,896,609 vertices, four full 2048px PBR images. Source files have NOT been modified or deleted.
Game assets: `godot/assets/terrain/{grass,dirt,leaves,moss,mud,rocky,tavern_room}.glb` plus all 23 full-resolution extracted PBR companion images. Five ground GLBs and tavern room are byte-identical to their provided sources; see SHA256 verification constants in `scripts/verify_terrain_import.py`. Grass is a 4-vertex 1.524×1.524m surface, derived using the source model's exact albedo/normal/metallic-roughness/emissive image bytes; original SHA256 `fa184ea0afd6814f8eb323d6e278f73c74898ae49a1665d5573c04480c1d201e` retained in GLB extras.
Godot converts original ground GLBs to Y-up. Their native widths are about 0.96m with up to 77mm relief. `terrain_layer.gd` instances them at exact 1.524m X/Z spacing and scales ONLY vertical geometry to <=12mm relief, without editing GLB files, and with six MultiMesh batches (530 instances in initial local region). Material shader retains original 2048px albedo, normal and MR maps. Original grass emissive map remains stored but is intentionally not applied to the ground to prevent a neon appearance.
Tavern room source: 415,499 vertices, 56,653,080-byte GLB and four preserved image maps (including 4096px original albedo and normal). Scale 9× footprint and 13× height makes an approximately 17m wide, 15m deep open-roof hall. Wooden floor source Y≈-0.247 aligns with world Y≈0.02. Original doorway was checked against an 8–9ft height range; collision entrance has a 2.1m gap and six primitive wall/hearth colliders. No character skeletons, bone maps or animations were created or altered.
Validation: original source GLB SHA256, 23 extracted image byte comparisons, Godot 4.5.1 import, runtime screenshot, nine headless scene/physics/regression tests PASS. Android visual/performance not yet independently measured.


## v0.3.192 source and art assets
- Grass replacement: Beastiary Review/grassground.glb (unmodified user original); game godot/assets/terrain/grass.glb identical SHA256 ebe40701ac73d59c264f6d8c22c7ed4223f4520039de6be9b93749066126c0d2, 3,028 vertices. Original embedded full-resolution maps: godot/assets/terrain/grass_0.jpg, grass_1.png, grass_2.png, byte-identical. Old optimized grass and its four derived companion maps replaced; all five other ground sources and tavern unchanged.
- Native custom ornate SVG screen skin: godot/ui_skin/panel.svg, panel_active.svg, button.svg, button_active.svg. Editable generator: blender/ui_skin/build_inventory_skin.py; do not flatten UI into screenshot overlays.

## v0.3.193 original clustered ground-cover GLBs
- Four user sources moved unchanged to Stormfather G:/ChatGPT Projects/Forests of Hell/Beastiary Review after Blender inspection. Original filename, SHA256, images and dimensions locked in scripts/verify_ground_cover_import.py and docs/ground-cover-and-seams-v0.3.193.md.
- Grouped in-game assets: godot/assets/ground_cover/woodland_bonus.glb (12 broadleaf extras, four active), plants.glb (4 tall grass, 3 ferns, 4 woodland plants), debris.glb (4 stones, 3 leaves, 3 twigs, 3 branches), elements.glb (4 mushrooms, 3 moss-like, 3 roots, 2 pinecones). 48 intact candidate mesh nodes / 40 active MultiMeshes. All original 2048px embedded image bytes SHA-identical in grouped derivatives, without lost maps or source texture transcoding.
- Extraction source and per-variant coordinate/vertex manifest: blender/ground_cover/extract_grouped_assets.py and variant_audit.json. All are static decorative, no rig/bone/animation required. Requested short grass 4 variants still missing.

## v0.3.194 custom desktop UI art
- New reproducible Python/Pillow art generator: blender/ui_skin/polish_v194_art.py. PNGs in godot/ui_skin/{panel,panel_active,slot,slot_active,button,button_active,tab,tab_active,header,portrait_backdrop}.png. First nine are original 256x256 RGBA full-quality 9-slice textures for Godot actual interactive controls, portrait backdrop is original 380x620 charcoal/stone texture behind existing verified game GLB. No new model/rig, skeleton, bone map or animation changes. Legacy v0.3.192 SVG art preserved. Initial discarded art drafts are not part of published assets.

## v0.3.195 decorative UI textures
- Nine source-generated 512px RGBA unchanged-quality PNGs: godot/ui_skin/panel.png, panel_active.png, header.png, slot.png, slot_active.png, button.png, button_active.png, tab.png, tab_active.png. Portrait backdrop 650×1060 RGBA godot/ui_skin/portrait_backdrop.png; no user-provided image/textures downscaled, modified or removed. Source: blender/ui_skin/forge_v195_art.py, square_v195_art.py. No new GLB, bones, rigs or animation. Stable game item atlas unchanged.

## v0.3.196 approved equipment sprite source
- Full original user PNG: godot/ui_skin/approved_equipment_source.png, 1280×1229 RGBA, 2,270,814 bytes, SHA256 cae4145663bf6d7e2d1f696091aebb5efde58ded01ca4ff91eb14b318c7957e4. Identical off-repo original in G:/ChatGPT Projects/Forests of Hell/Inventory Review/Original Equipment UI Source.png. Source-exact pixel crops under godot/ui_skin/approved_equipment/: top_banner, tab_header, stat_panel, inventory_panel, detail_panel, slot_blank. User-source-derived modifiable character_panel with 7 left + 8 right slots, accessory_slot and occupied_slot. Reproducible extraction/derivative source blender/ui_skin/extract_approved_equipment.py, compose_equipment_15.py; SHA/PIL/import asset fidelity gate scripts/verify_approved_equipment_art.py. Original remains full-resolution, Godot lossless importer compress/mode=0, no size limit. No GLBs/skeletons/animations changed.

## v0.3.201 — playable goblin and modular equipment

The playable female goblin derives from femalegobsimpleskel.glb with all 28 supplied joints and both native locomotion clips. WearableLeather and BootCoverage are separate visual meshes. The detached leather guards are bound to the corresponding forearms. Full-size body and garment maps accompany the GLBs.

## v0.3.202 local release candidate

The user-supplied Female No Race Base is the only playable character. Height, frame width and body depth are saved and available in the creator. Experimental weight and muscle sliders were removed. Existing character selections resolve to the sole playable model. The female base is unarmored and contains its original 66 joints and ten source animation clips. Creator, saved scale, PC controls, inventory and terrain headless checks passed. Physical device and equipment fit verification remain outstanding.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## v0.3.205 Source-derived appearance UI art
Unchanged source: godot/creator_ui/source_elements.png (1900x1900 RGBA), SHA256 4263B46C00712ED5C7B987CF0C426E576410B1619467DF8EE0FC66CA42579623. Five new PNGs under godot/creator_ui: creator_stone_oval (173x87), creator_button_compact (420x100), creator_button_compact_active (420x100), creator_arrow_compact (130x110), creator_slider_diamond_compact (38x44). Generated by scripts/refine_creator_v205_art.py from existing source; new assets are distinct derivatives and do not replace or downscale original texture maps. GLBs, all bone maps, skeletons, animations and materials unchanged.

## v0.3.206 appearance accessory source audit
- All source files unchanged. Female No Race Base GLB SHA256 739554A67C1A3D7BE2B591F965751B8A41AB8F29AA1FCCDC5D13EEF8B19655F8: 66 supplied original bone names/parents, including mixamorig_Head index 5 and headfront. Hair Wavy SHA256 245BF1F734FFA9D245373DCECDB7406C7234248C475F85613499DB1B64F93D9B. Full Beard SHA256 4ADD537BED19A6FE9A03E85F7864417E2B502C760B767855C37420CFDBA2FE31. All original GLBs, source maps, hair_short and hair_braid also untouched. Measured source mesh AABBs and used transform settings documented at docs/appearance-head-fit-v0.3.206.md.
- Hair/beard source GLBs are unrigged static meshes, fitted as children of existing BoneAttachment3D. No new skeleton or bone maps, geometry editing, source-resolution changes, texture compression or loss of material channels.

## v0.3.207 hairstyle switching asset integrity
- No new or modified GLB files, skeletons, bone maps, animations, original textures, geometry or material channels. Wavy, Short and Braid remain the same full-resolution source files and are all instantiated through the original Godot scene importer. Original 66-bone supplied female base and head bone index 5 unchanged. Only per-style runtime attachment defaults and configuration are different; distinct Z defaults are in character_appearance.gd and audited in docs/hair-style-switch-v0.3.207.md.

## v0.3.208 material policy
No new or modified skeleton, bone map, GLB, animation, source or imported texture. Hair tint duplicates each original runtime surface material for hair-only albedo coloring; original PBR texture channels and resolutions remain unchanged.
