Future procedural-world milestones: region seeds and named biome zones; points of interest and settlement placement; procedural dungeons; enemies and encounter generation; persistent world modifications and loot; streaming performance tests; save migrations; isometric presentation across the overworld. These features are not yet implemented.


## Low priority — deferred 2014 Athletics mechanics
- Swimming
- Jumping: basic action implemented; remaining Athletics and terrain interactions deferred.
- Grappling
- Climbing

Shoving is the current combat priority; use the shared attack resource. Prone recovery and animation remain provisional until movement economy and dedicated animation are implemented.


## Low priority — background feature follow-up
- Guild Artisan: implement monthly 5 GP guild dues when calendar and guild systems exist. No deductions until implemented.
- Background feature tags are derived from the selected background; actual feature interactions remain future work.

## Inventory and equipment — current UI milestone (2026-11-25)

### Implemented foundation
- Equipment screen with 17 visible slots, including eyes and wrists; item selection, compatible-slot equipping and unequipping.
- Illustrated inventory catalog, stack quantities, selected item details and carrying weight.
- Existing weapon, armor, shield and clothing assignments; ownership and handedness checks.

### Approved next work — planning only
- Keep four main tabs: Equipment, Items, Materials, Quest.
- Equipment: show equippable items; preserve current character and slot layout.
- Items: match supplied two-column reference, with All Items, Consumables, Tools, Supplies, Utility and Other filters. Other includes keepsakes and physical quest items; preserve protection for important items.
- Materials: match supplied reference, with All Materials, Raw Materials, Plants & Herbs, Animal Parts, Ore & Gems, Crafting Parts and Other filters. Do not add example materials merely to populate the UI.
- Quest: quest journal rather than a physical item inventory. Active/completed quests, objectives, progress, rewards and relevant item references; final layout pending.
- Add independent classification tags, scrollable lists, quantities, selected-item icon and description, weight/capacity footer, sorting by name/weight/quantity/value where data exists, and context-sensitive Use action disabled when unsupported.
- Implement actual item actions only when corresponding gameplay exists.

### Follow-up
- Accessory item definitions and slot compatibility, active melee/ranged weapon switching, proficiency penalty verification, equipment appearance, persistence and device testing.
- Publish implementation only after planning approval; increment version, build and verify APK and updater metadata.
