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
