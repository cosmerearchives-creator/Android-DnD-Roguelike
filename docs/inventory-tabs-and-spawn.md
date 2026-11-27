# Inventory tabs and forest spawning — v0.3.174

## Screens
- Items, Materials and Quest follow the provided Wildlands target images: left filter sidebar, item/quest rows with icon/name/quantity, separate detail pane and bottom inventory/action footer.
- Six Items filters, seven Materials filters and five Quest filters.
- Selection, sorting and dynamic filters are supported; Use remains disabled until item consumption is implemented.
- Track toggles and saves quest tracking selection, but does not yet add navigation markers to the forest.
- The actual goblin quest objective is shown and counts goblins defeated, up to 10. Progress persists to `user://quest_progress.cfg`. No artificial story quest or rewards are fabricated.
- Materials are empty until the game supplies tagged material inventory entries. The legacy item catalog contains no material-class entries, and we do not silently add sample inventory.
- Equipment tab remains as previously approved, with the isolated model viewer.

## Spawn behavior
- Initial forest population: one random enemy, rather than all goblin variants at the origin.
- Subsequent spawns: one attempt roughly every 11 seconds when fewer than eight are active.
- Each spawn is 14–30m from the player's current position, at least 7.5m from any existing enemy and within the playable forest.
- All existing goblin variants remain in the random pool, with the Fire Beetle at a 23% selection chance.
- Wandering AI remains active; combat awareness and encounter balancing are future work.

## Tests
- `godot --headless --path godot --script res://tests/inventory_smoke.gd`
- `godot --headless --path godot --script res://tests/inventory_tabs_smoke.gd`
- `godot --headless --path godot --script res://tests/enemy_spawn_smoke.gd`
- Physical-device visual and performance checks are still required.
