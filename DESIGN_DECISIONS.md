# Approved design decisions and historical boundaries
These are user-approved **design targets**, not assertions that the latest Godot build implements each item.

- **Continuity/publishing:** Existing Forests of Hell repo `main`, Godot Android + PC, no alternative application or branch; changes publish after test with signing/updater preservation. Two modes: Planning and Publishing. Explicit docs-only work commits documentation without a binary version bump.
- **Rules:** 2014 D&D 5e by default; individually approved custom mechanics take precedence and must be identified. Temporary one-second attacks and test combat stats are implementation scaffolding, not full 5e ([combat foundation](docs/combat-foundation-v0.3.184.md)).
- **Mandatory skeleton ownership:** User provides appropriate rig/skeleton. If missing, incompatible or incomplete, **stop development**; no AI-generated substitute, no guessed retarget rig. Do not edit supplied skeleton without approval.
- **Bone maps:** Import **all** provided maps, every bone/mapping, without guessing what can be omitted.
- **Textures:** Development-quality original resolution and channels preserved, no compression/downscale/degradation unless specifically approved for optimization. Keep source assets and import validation.
- **PC:** Mouse movement directly steers camera without right-click; F11 fullscreen; PC auto-updater; loading transition for forest. Source: [PC startup controls](docs/pc-free-mouse-look-startup-v0.3.186.md) and recent `main` commits. Actual device acceptance pending.
- **World time:** 1 real second = 1 game second during active gameplay; explicit sleep/wait may skip time. Design intervals: 8am–4pm direct sunlight, 4–6am/pm and 6–8pm transitional dim/normal according to [world-development.md](docs/world-development.md), 8pm–4am darkness. Shade blocks direct sunlight. Clear weather initially, but support future overcast, rain, fog. Bed in hub should allow choosing an hour. Check current Godot integration separately.
- **Races/visibility:** High Elf, Wood Elf, Drow; darkvision, Drow sunlight sensitivity, canopy shade, unobscured/sneak rules; full trait/rest/magic infrastructure deferred (see [racial backlog](docs/racial-backlog.md)).
- **Inventory:** Equipment/Items/Materials/Quest tabs and accepted reference art; eyes/goggles and wrist slots; test chest with one of each available item, visible accessible bed near training dummy, quest tracking and quantities. UI presence and game integration require separate validation.
- **World/enemies:** Varied forest canopy, dense trees, goblin variants/Fire Beetle and additional D&D forest enemies; sensible spread-out random spawns and appropriate scaling/animations. Never improvise missing skeletons to add an enemy.
- **Performance:** Prefer load staging, caching, culling, material reuse and measured code optimization before proposing any quality reduction.
- **Historical notes:** Older generated rigs and compressed WebP atlases remain historical implementation records, **not approval** to generate rigs or compress new textures.

Source detail: `docs/roadmap-world.md`, `docs/roadmap-asset-pipeline.md`, `docs/inventory-tabs-and-spawn.md`, `docs/racial-backlog.md`.

## Approved equipment visual behavior — 2026-11-27
The confirmed original female leather armor design is the visual reference. The male receives a fitted, anatomically adjusted variant while keeping his **existing user-approved skeleton completely untouched**. In Godot, the same `leather_armor` Body item selects the male fitted model for world and inventory; removing or replacing it restores the underlying original male visual. Armor material appearance stays faithful to the recovered reference. Neither armor should be permanently fused into the original source body/rig.


## Forest terrain/tavern visual standard — 2026-11-27
Ground remains exactly 5×5ft (1.524m) per tile and effectively flat. Six types are grass, dirt, fallen leaves, moss, mud and rocky soil. The base ground is collidable independently from cosmetic tiles, which are batched to protect frame time and can later gain blending/vegetation. Original texture quality and channels are retained. The provided extremely dense grass mesh is not repeated; a separate 4-vertex material-preserving derivative is used, original untouched.
The supplied tavern is an open-roof room, not a complete roofed exterior. Keep that original look in this milestone; do not invent a roof or change character rig sizes to make the entrance work. Scale building independently to provide clearance for a provisional 8ft playable race. Existing arch is approximately 5ft wide around 9ft height; collision opening is 2.1m. Follow with furnishings, services and roof design later.


## v0.3.192 approved decisions (2026-11-27)
- Custom ornate graphical skin rather than Godot flat-color mockups. Keep original actual 3D character GLB in equipment panel.
- Exactly 15 equipment slots: no Ears or Legs; Ranged becomes Secondary; existing saves migrate.
- Inventory opens as a true pause modal, world time and combat stop while menus remain usable.
- Edge-only grass/dirt/leaves/moss/mud/rocky transition with persistent 0-30in in-game blending slider (6in default), no whole-tile blurring.
- Restore saved fullscreen/window state and camera perspective/zoom when playing next time. Assets move Imports -> Review after importing.

## v0.3.193 ground cover
- Asset sheet geometry may be a single mesh; extract full independent clusters by spatial grouping of connected components, preserve original images and source files rather than split every leaf separately.
- Use 40 active decorative ground-cover meshes in instanced draw batches and retain 8 other usable broadleaf variants in the collection. Do not pretend broadleaf foliage is one of 4 still-missing short-grass models.
- Seeded placement with biome palette, natural rotation/scale, modest boundary clustering, no clutter collision. Terrain grid stays 5ft for game rules; refine geometry/material edge without editing source mesh or compromising textures.

## v0.3.194 — reusable visual UI kit
- Purpose-made RGBA nine-slice texture for each scale family (panel, slot, button, tab, header). Opaque rich leather, narrower bronze tooling, distinct active state; don't use panel-corner ornaments for individual 80px slots. Original user fantasy reference images are visual targets, not noninteractive screenshot backgrounds. Source art generator blender/ui_skin/polish_v194_art.py; Godot godot/ui_skin/*.png. Existing SVG retained historically, no longer driving refreshed desktop rendering.
- Hide gameplay HUD while inventory modal is open, restore on close; world remains fully paused. Real GLB character portrait retained; new charcoal/stone stage uses only a 2D backdrop. Missing abilities/proficiencies remain shown as absent until integrated rather than faked.

## v0.3.195 UI visual architecture
- Keep the user-approved Equipment/Items/Materials/Quests interaction architecture, 15 slots, dark leather and sculpted gold/bronze. Use independent purpose-built textures for large scrollwork panels vs compact nine-slice button/slot elements. Use an original static stone stage backdrop behind real 3D player GLB, not a substitute rig. Display only data actually present. Equip comparison derives from compatibility, not stale slot selection. Top bronze status banner is decorative and noninteractive while gameplay paused.

## v0.3.196 user-provided UI source
- Actual approved 1280×1229 user-supplied image becomes authoritative for the Equipment page's bronze/leather material, frame geometry, color and pedestal. Keep source PNG byte-identical and crop without re-rendering/AI approximation. Overlay fully functional Godot Buttons, Labels, item icon controls, stat data and existing player SubViewport rather than flattening working controls into static screenshot. Adapt source right seven frames into eight with source-derived texture while preserving 15 real Equipment slots. Do not propagate new visual treatment to other 3 tabs until individually reviewed. Keep paused gameplay HUD hidden; scenery visible around fully opaque source panels.

## v0.3.204 creator scale correction candidate
- Replaced contaminated cropped UI composites with clean source-derived nine-slice panels/buttons and slider textures. Original 1900x1900 and 1673x940 source images unchanged. Local Godot4.5.1 tests creator_workshop_smoke and creator_v204_scale_smoke pass. Release and physical screenshot QA pending. See docs/creator-scale-v0.3.204.md.

## v0.3.205 Workshop frame hierarchy
Use separate ornate large-panel/action buttons, subdued compact navigation/category frames and plain thin-corner control arrows. Keep orange glow only for current selection. Native engine sliders retain callbacks with independent, visible gold rails. Show source-exact isolated oval stage behind real GLB, never another screenshot fragment.

## v0.3.206 Head accessory fitting
- Always attach supplied unrigged hair/beard meshes rigidly to the original compatible player skeleton's real head bone (female index 5); never fabricate a substitute rig. Match actual per-mesh Godot AABB Y upper bound to head-relative scalp/chin fitting, using original +Z face direction. Use a +Z jaw offset for beard; allow saved XYZ nudge/reset within ±0.25m. Preserve original rig/bone maps/textures, and keep appearance offsets shared across creator, gameplay and inventory.
- Hair/Beard categories use one appropriate right-hand panel with native controls and should not display unused Body card frames. Native slider visual system matches existing gold UI.
