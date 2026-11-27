# v0.3.196 — User-supplied Equipment UI artwork, live functionality

Date: 2026-11-27. The user authorized replacing procedural approximations with the actual uploaded 1280 × 1229 source sheet and requested the Equipment page only. All work is within the existing repository and `main` publishing contract.

## Source identity and fidelity

Original named `Ornate Fantasy RPG UI Frames.png`; copied byte-for-byte onto Stormfather at `G:\ChatGPT Projects\Forests of Hell\Inventory Review\Original Equipment UI Source.png` and preserved at `godot/ui_skin/approved_equipment_source.png`. SHA-256: `cae4145663bf6d7e2d1f696091aebb5efde58ded01ca4ff91eb14b318c7957e4`, 2,270,814 bytes. RGBA source 1280×1229. Exact pixel crops are in `godot/ui_skin/approved_equipment/` for tab_header, top_banner, stat_panel, inventory_panel, detail_panel, slot_blank; generated extraction code `blender/ui_skin/extract_approved_equipment.py`. The complete source and all original cropped pixel content are unchanged.

A source-backed, editable equipment-body derivative `character_panel.png` preserves all art outside its right slot column. The source sheet contains seven left-hand and seven right-hand slot graphics, but the approved equipment system requires 15. `blender/ui_skin/compose_equipment_15.py` uses exact source slot crops to compose eight slots on the right, including a neutral source-derived Accessory placeholder; no new rig/model is produced. An `occupied_slot.png` source-backed frame covers baked placeholders only where real equipped item icons must display. These are display/layout derivatives, not replacements for the full-resolution original. Original icons in the sheet are placeholders only when no item occupies a slot.

Godot import settings checked: UI texture `compress/mode=0` (lossless), no mipmaps, `process/size_limit=0`; original art dimensions retained. `scripts/verify_approved_equipment_art.py` verifies original full SHA-256, exact lossless crops, left 370px of body derivative, slot dimensions and per-file import settings. CI verifies the source SHA-256 without requiring Pillow.

## Equipment page integration

`godot/inventory_screen.gd` uses actual source-backed TextureRects under the existing Godot controls. All source art controls ignore pointer input, while the four tabs, 15 equipment slots, item grid selections, inspect/compare/equip and close button remain actual native Godot interactive Controls. Source-image panels are visible only on the Equipment tab; Items/Materials/Quests retain the pre-existing v0.3.195 presentation. Old generic frame art is suppressed where source images already provide the frame, avoiding duplicate ornaments. Normal, hover and selected states are independent transparent hit regions with amber highlights.

The live existing 3D player model (selected male/female appearance, existing equipped armor and turn interactions) renders in a transparent SubViewport in front of the original stone plinth. No skeleton, bone map, animation or model was edited. Real selected item icons, quantity, actual item descriptions, compatibility, weight, live combat stats and equipped slots are still bound to the pre-existing data model. Placeholder source art does not replace equipped item graphics.

The source image's 10×3 connected item grid is now represented by 10 columns and 30 interactive/empty cells on this Equipment tab. Other tabs continue to use their own list/grid layouts. The source sheet's bottom frame and tabs have been aligned to 1600×900, retaining the original `Forest of Hell Inventory` approved visual direction. The source banner is in the upper-left, keeping a functional Close control at top right.

The world remains fully paused in Inventory and the gameplay HUD hidden; the background is partially visible *around* opaque source-art panels. Lowering backdrop opacity is safe for legibility because the source panels themselves are opaque. Existing user-controlled camera/fullscreen and perspective preferences are unchanged.

## Testing

- Full original bytes preserved, crop/alpha and import settings validated by `scripts/verify_approved_equipment_art.py`.
- Godot 4.5.1 OpenGL graphical equipment capture on GTX1080Ti saved `G:\ChatGPT Projects\Forests of Hell\Inventory Review\equipment.png`; other tab captures `items.png`, `materials.png`, `quest.png`.
- 13 local Godot smoke tests passed: inventory portrait, inventory tabs portrait, pause/HUD and blend, PC camera/display persistence, male leather armor, terrain/tavern, four terrain clutter collections, terrain/tavern physics, enemy spawn, lean cast, updater, new desktop-layout/artwork/15-slot + item compatibility regression, and PC platform controls.
- Android physical device scaling, FPS/memory and touchscreen inspection remain unverified.
- CI release export, signer identity and updater manifests must be verified after committing to main; don't claim publication from a git push alone.

## Known gaps

- The provided raster sheet does not contain all standalone 15 different empty slot illustrations; Accessory is a source-derived neutral ninth/right-side variant rather than invented art. Real item icons are dynamic.
- Original large stat card has decorative blank fields. Known D&D 5e values remain live, but missing character sheet data such as five ability scores/proficiencies cannot be fabricated. Some fine text is smaller than the final target reference, particularly stat headers and label overlays.
- This update does not redesign Items, Materials or Quests. The compiled user supplied artwork is the source of truth for Equipment, future modular independent assets may allow more precise scaling and hover states.
