# Godot inventory restoration — v0.3.172

**Reference:** Approved Wildlands portrait inventory screenshot. Preserve the teal-and-gold fantasy presentation and the existing Godot gameplay HUD.

## Restored
- Inventory opens from the existing gameplay **Inventory** button and closes without changing scenes.
- Original 2014 D&D Equipment catalog exported to native Godot source: 116 item definitions.
- Original Wildlands inventory art atlas: 119 IDs, 128-pixel cells, 10 columns. The reusable PNG is 1280×1536; a source mapping JSON is retained.
- Equipment view includes the **currently selected playable model** in a rotatable 3D preview, a gold oval pedestal, and 17 equipment slots. Leather armor can be previewed on Female Base when actually equipped.
- Equipment, Items, Materials, and Quest tabs, a scrollable six-column inventory, selected-item details, item counts, alphabetical sorting, equipment slot selection, and compatible equip/unequip actions.
- Saved inventory and slot assignments in `user://inventory_v1.json`. Initial loadout follows the legacy Fighter demo, not the old Android save file.
- Carrying weight and capacity use item weights and 15 × Strength (starting Strength 10), including the filled waterskin.
- Teal panels and double-gold trimmed, clipped-corner buttons reproduce the design.
- Input to the gameplay joystick/actions is suppressed while the inventory is open and reenabled on close.

## Explicit limitations
- The old Java game's real inventory save has **not yet** been migrated into Godot. The starting items are intentionally based on its Fighter test loadout.
- Only compatible equipment types already represented in the old catalog can be equipped. The UI shows all 17 slots, but many require future wearable item implementation.
- Only the Female Base leather outfit currently has a corresponding rigged 3D armor preview. Equipping other armor updates the inventory data and slot icon, not yet the model geometry.
- Gold, HP, combat statistics, and quest progress are not yet integrated into the new Godot character/gameplay system. UI stats show placeholders rather than fabricated values.
- Materials and Quest views may be empty because the starter loadout contains none.
- Equipment changes to the Female Base leather armor are connected to the current world preview; all other combat modifiers await the gameplay migration.

## Tests
Run `godot --headless --path godot --script res://tests/inventory_smoke.gd` after the editor has imported the assets.
The smoke test confirms 4 tabs, 17 slots, 116+ catalog items, icon loading, starter equipment, and no portrait overflow at both 720×1280 and 720×1536.
Regenerate the icons and catalog with `python scripts/export_godot_inventory.py`.
