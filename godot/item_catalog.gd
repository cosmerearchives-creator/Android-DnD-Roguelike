extends RefCounted
## Exported from the legacy Equipment.java catalog.
const ITEMS = {
  "battleaxe": {
    "id": "battleaxe",
    "name": "Battleaxe",
    "weight": 4.0,
    "type": "Weapon",
    "icon": 6
  },
  "flail": {
    "id": "flail",
    "name": "Flail",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 32
  },
  "glaive": {
    "id": "glaive",
    "name": "Glaive",
    "weight": 6.0,
    "type": "Weapon",
    "icon": 33
  },
  "greataxe": {
    "id": "greataxe",
    "name": "Greataxe",
    "weight": 7.0,
    "type": "Weapon",
    "icon": 35
  },
  "greatsword": {
    "id": "greatsword",
    "name": "Greatsword",
    "weight": 6.0,
    "type": "Weapon",
    "icon": 37
  },
  "halberd": {
    "id": "halberd",
    "name": "Halberd",
    "weight": 6.0,
    "type": "Weapon",
    "icon": 40
  },
  "lance": {
    "id": "lance",
    "name": "Lance",
    "weight": 6.0,
    "type": "Weapon",
    "icon": 53
  },
  "longsword": {
    "id": "longsword",
    "name": "Longsword",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 59
  },
  "maul": {
    "id": "maul",
    "name": "Maul",
    "weight": 10.0,
    "type": "Weapon",
    "icon": 63
  },
  "morningstar": {
    "id": "morningstar",
    "name": "Morningstar",
    "weight": 4.0,
    "type": "Weapon",
    "icon": 66
  },
  "pike": {
    "id": "pike",
    "name": "Pike",
    "weight": 18.0,
    "type": "Weapon",
    "icon": 72
  },
  "rapier": {
    "id": "rapier",
    "name": "Rapier",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 82
  },
  "scimitar": {
    "id": "scimitar",
    "name": "Scimitar",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 88
  },
  "shortsword": {
    "id": "shortsword",
    "name": "Shortsword",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 91
  },
  "trident": {
    "id": "trident",
    "name": "Trident",
    "weight": 4.0,
    "type": "Weapon",
    "icon": 106
  },
  "war_pick": {
    "id": "war_pick",
    "name": "War Pick",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 110
  },
  "warhammer": {
    "id": "warhammer",
    "name": "Warhammer",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 111
  },
  "whip": {
    "id": "whip",
    "name": "Whip",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 114
  },
  "blowgun": {
    "id": "blowgun",
    "name": "Blowgun",
    "weight": 1.0,
    "type": "Weapon",
    "icon": 8
  },
  "hand_crossbow": {
    "id": "hand_crossbow",
    "name": "Hand Crossbow",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 43
  },
  "heavy_crossbow": {
    "id": "heavy_crossbow",
    "name": "Heavy Crossbow",
    "weight": 18.0,
    "type": "Weapon",
    "icon": 45
  },
  "longbow": {
    "id": "longbow",
    "name": "Longbow",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 58
  },
  "net": {
    "id": "net",
    "name": "Net",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 68
  },
  "club": {
    "id": "club",
    "name": "Club",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 20
  },
  "dagger": {
    "id": "dagger",
    "name": "Dagger",
    "weight": 1.0,
    "type": "Weapon",
    "icon": 25
  },
  "greatclub": {
    "id": "greatclub",
    "name": "Greatclub",
    "weight": 10.0,
    "type": "Weapon",
    "icon": 36
  },
  "handaxe": {
    "id": "handaxe",
    "name": "Handaxe",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 44
  },
  "javelin": {
    "id": "javelin",
    "name": "Javelin",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 51
  },
  "light_hammer": {
    "id": "light_hammer",
    "name": "Light Hammer",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 57
  },
  "mace": {
    "id": "mace",
    "name": "Mace",
    "weight": 4.0,
    "type": "Weapon",
    "icon": 60
  },
  "quarterstaff": {
    "id": "quarterstaff",
    "name": "Quarterstaff",
    "weight": 4.0,
    "type": "Weapon",
    "icon": 80
  },
  "sickle": {
    "id": "sickle",
    "name": "Sickle",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 93
  },
  "spear": {
    "id": "spear",
    "name": "Spear",
    "weight": 3.0,
    "type": "Weapon",
    "icon": 97
  },
  "light_crossbow": {
    "id": "light_crossbow",
    "name": "Light Crossbow",
    "weight": 5.0,
    "type": "Weapon",
    "icon": 56
  },
  "dart": {
    "id": "dart",
    "name": "Dart",
    "weight": 0.25,
    "type": "Weapon",
    "icon": 26
  },
  "shortbow": {
    "id": "shortbow",
    "name": "Shortbow",
    "weight": 2.0,
    "type": "Weapon",
    "icon": 90
  },
  "sling": {
    "id": "sling",
    "name": "Sling",
    "weight": 0.0,
    "type": "Weapon",
    "icon": 95
  },
  "padded": {
    "id": "padded",
    "name": "Padded Armor",
    "weight": 8.0,
    "type": "Armor",
    "icon": 70
  },
  "leather_armor": {
    "id": "leather_armor",
    "name": "Leather Armor",
    "weight": 10.0,
    "type": "Armor",
    "icon": 54
  },
  "studded_leather": {
    "id": "studded_leather",
    "name": "Studded Leather",
    "weight": 13.0,
    "type": "Armor",
    "icon": 99
  },
  "hide_armor": {
    "id": "hide_armor",
    "name": "Hide Armor",
    "weight": 12.0,
    "type": "Armor",
    "icon": 47
  },
  "chain_shirt": {
    "id": "chain_shirt",
    "name": "Chain Shirt",
    "weight": 20.0,
    "type": "Armor",
    "icon": 17
  },
  "scale_mail": {
    "id": "scale_mail",
    "name": "Scale Mail",
    "weight": 45.0,
    "type": "Armor",
    "icon": 87
  },
  "breastplate": {
    "id": "breastplate",
    "name": "Breastplate",
    "weight": 20.0,
    "type": "Armor",
    "icon": 9
  },
  "half_plate": {
    "id": "half_plate",
    "name": "Half Plate",
    "weight": 40.0,
    "type": "Armor",
    "icon": 41
  },
  "ring_mail": {
    "id": "ring_mail",
    "name": "Ring Mail",
    "weight": 40.0,
    "type": "Armor",
    "icon": 84
  },
  "chain_mail": {
    "id": "chain_mail",
    "name": "Chain Mail",
    "weight": 55.0,
    "type": "Armor",
    "icon": 16
  },
  "splint": {
    "id": "splint",
    "name": "Splint",
    "weight": 60.0,
    "type": "Armor",
    "icon": 98
  },
  "plate": {
    "id": "plate",
    "name": "Plate",
    "weight": 65.0,
    "type": "Armor",
    "icon": 74
  },
  "shield": {
    "id": "shield",
    "name": "Shield",
    "weight": 6.0,
    "type": "Shield",
    "icon": 89
  },
  "backpack": {
    "id": "backpack",
    "name": "Backpack",
    "weight": 5.0,
    "type": "Container",
    "icon": 5
  },
  "pouch": {
    "id": "pouch",
    "name": "Pouch",
    "weight": 1.0,
    "type": "Container",
    "icon": 77
  },
  "waterskin": {
    "id": "waterskin",
    "name": "Waterskin",
    "weight": 0.0,
    "type": "Container",
    "icon": 112
  },
  "chest": {
    "id": "chest",
    "name": "Chest",
    "weight": 25.0,
    "type": "Container",
    "icon": 19
  },
  "bedroll": {
    "id": "bedroll",
    "name": "Bedroll",
    "weight": 7.0,
    "type": "Supply",
    "icon": 7
  },
  "mess_kit": {
    "id": "mess_kit",
    "name": "Mess Kit",
    "weight": 1.0,
    "type": "Supply",
    "icon": 64
  },
  "tinderbox": {
    "id": "tinderbox",
    "name": "Tinderbox",
    "weight": 1.0,
    "type": "Tool",
    "icon": 101
  },
  "torch": {
    "id": "torch",
    "name": "Torch",
    "weight": 1.0,
    "type": "Supply",
    "icon": 103
  },
  "ration": {
    "id": "ration",
    "name": "Ration (1 day)",
    "weight": 2.0,
    "type": "Supply",
    "icon": 83
  },
  "rope_hemp": {
    "id": "rope_hemp",
    "name": "Hempen Rope (50 ft)",
    "weight": 10.0,
    "type": "Supply",
    "icon": 85
  },
  "rope_silk": {
    "id": "rope_silk",
    "name": "Silk Rope (50 ft)",
    "weight": 5.0,
    "type": "Supply",
    "icon": 86
  },
  "piton": {
    "id": "piton",
    "name": "Piton",
    "weight": 0.25,
    "type": "Tool",
    "icon": 73
  },
  "hammer": {
    "id": "hammer",
    "name": "Hammer",
    "weight": 3.0,
    "type": "Tool",
    "icon": 42
  },
  "crowbar": {
    "id": "crowbar",
    "name": "Crowbar",
    "weight": 5.0,
    "type": "Tool",
    "icon": 24
  },
  "oil": {
    "id": "oil",
    "name": "Oil Flask",
    "weight": 1.0,
    "type": "Supply",
    "icon": 69
  },
  "candle": {
    "id": "candle",
    "name": "Candle",
    "weight": 0.0,
    "type": "Supply",
    "icon": 13
  },
  "incense": {
    "id": "incense",
    "name": "Incense Stick",
    "weight": 0.0,
    "type": "Supply",
    "icon": 50
  },
  "prayer_book": {
    "id": "prayer_book",
    "name": "Prayer Book",
    "weight": 1.0,
    "type": "Other",
    "icon": 78
  },
  "prayer_wheel": {
    "id": "prayer_wheel",
    "name": "Prayer Wheel",
    "weight": 1.0,
    "type": "Other",
    "icon": 79
  },
  "holy_symbol": {
    "id": "holy_symbol",
    "name": "Holy Symbol",
    "weight": 1.0,
    "type": "Tool",
    "icon": 48
  },
  "vestments": {
    "id": "vestments",
    "name": "Vestments",
    "weight": 4.0,
    "type": "Other",
    "icon": 108
  },
  "common_clothes": {
    "id": "common_clothes",
    "name": "Common Clothes",
    "weight": 3.0,
    "type": "Other",
    "icon": 22
  },
  "travelers_clothes": {
    "id": "travelers_clothes",
    "name": "Traveler's Clothes",
    "weight": 4.0,
    "type": "Other",
    "icon": 105
  },
  "animal_trophy": {
    "id": "animal_trophy",
    "name": "Animal Trophy",
    "weight": 0.0,
    "type": "Other",
    "icon": 1
  },
  "rank_insignia": {
    "id": "rank_insignia",
    "name": "Insignia of Rank",
    "weight": 0.0,
    "type": "Other",
    "icon": 81
  },
  "enemy_trophy": {
    "id": "enemy_trophy",
    "name": "Fallen Enemy Trophy",
    "weight": 0.0,
    "type": "Other",
    "icon": 30
  },
  "guild_letter": {
    "id": "guild_letter",
    "name": "Guild Introduction Letter",
    "weight": 0.0,
    "type": "Other",
    "icon": 39
  },
  "map_case": {
    "id": "map_case",
    "name": "Wooden Map Case with Map",
    "weight": 1.0,
    "type": "Other",
    "icon": 61
  },
  "bullseye_lantern": {
    "id": "bullseye_lantern",
    "name": "Bullseye Lantern",
    "weight": 2.0,
    "type": "Supply",
    "icon": 11
  },
  "miners_pick": {
    "id": "miners_pick",
    "name": "Miner's Pick",
    "weight": 10.0,
    "type": "Tool",
    "icon": 65
  },
  "shovel": {
    "id": "shovel",
    "name": "Shovel",
    "weight": 5.0,
    "type": "Tool",
    "icon": 92
  },
  "two_person_tent": {
    "id": "two_person_tent",
    "name": "Two-person Tent",
    "weight": 20.0,
    "type": "Supply",
    "icon": 107
  },
  "excavation_trinket": {
    "id": "excavation_trinket",
    "name": "Excavation Trinket",
    "weight": 0.0,
    "type": "Other",
    "icon": 31
  },
  "navigator_tools": {
    "id": "navigator_tools",
    "name": "Navigator's Tools",
    "weight": 2.0,
    "type": "Tool",
    "icon": 67
  },
  "walking_staff": {
    "id": "walking_staff",
    "name": "Walking Staff",
    "weight": 4.0,
    "type": "Other",
    "icon": 109
  },
  "hunting_trap": {
    "id": "hunting_trap",
    "name": "Hunting Trap",
    "weight": 25.0,
    "type": "Tool",
    "icon": 49
  },
  "wilderness_map": {
    "id": "wilderness_map",
    "name": "Wilderness Map",
    "weight": 0.0,
    "type": "Other",
    "icon": 115
  },
  "travel_documents": {
    "id": "travel_documents",
    "name": "Travel Documents",
    "weight": 0.0,
    "type": "Other",
    "icon": 104
  },
  "sketchbook": {
    "id": "sketchbook",
    "name": "Sketchbook",
    "weight": 1.0,
    "type": "Other",
    "icon": 94
  },
  "charcoal": {
    "id": "charcoal",
    "name": "Charcoal",
    "weight": 0.0,
    "type": "Supply",
    "icon": 18
  },
  "architect_tools": {
    "id": "architect_tools",
    "name": "Architect's Tools",
    "weight": 5.0,
    "type": "Tool",
    "icon": 3
  },
  "herbalism_kit": {
    "id": "herbalism_kit",
    "name": "Herbalism Kit",
    "weight": 3.0,
    "type": "Tool",
    "icon": 46
  },
  "cartographer_tools": {
    "id": "cartographer_tools",
    "name": "Cartographer's Tools",
    "weight": 6.0,
    "type": "Tool",
    "icon": 15
  },
  "calligrapher_supplies": {
    "id": "calligrapher_supplies",
    "name": "Calligrapher's Supplies",
    "weight": 5.0,
    "type": "Tool",
    "icon": 12
  },
  "archaeological_kit": {
    "id": "archaeological_kit",
    "name": "Archaeological Kit",
    "weight": 5.0,
    "type": "Tool",
    "icon": 2
  },
  "dice_set": {
    "id": "dice_set",
    "name": "Dice Set",
    "weight": 0.0,
    "type": "Tool",
    "icon": 27
  },
  "playing_cards": {
    "id": "playing_cards",
    "name": "Playing Card Set",
    "weight": 0.0,
    "type": "Tool",
    "icon": 75
  },
  "dragonchess": {
    "id": "dragonchess",
    "name": "Dragonchess Set",
    "weight": 0.5,
    "type": "Tool",
    "icon": 28
  },
  "three_dragon_ante": {
    "id": "three_dragon_ante",
    "name": "Three-Dragon Ante Set",
    "weight": 0.0,
    "type": "Tool",
    "icon": 100
  },
  "alchemist_supplies": {
    "id": "alchemist_supplies",
    "name": "Alchemist's Supplies",
    "weight": 8,
    "type": "Tool",
    "icon": 0
  },
  "brewer_supplies": {
    "id": "brewer_supplies",
    "name": "Brewer's Supplies",
    "weight": 9,
    "type": "Tool",
    "icon": 10
  },
  "calligrapher_supplies_artisan": {
    "id": "calligrapher_supplies_artisan",
    "name": "Calligrapher's Supplies",
    "weight": 5,
    "type": "Tool",
    "icon": 12
  },
  "carpenter_tools": {
    "id": "carpenter_tools",
    "name": "Carpenter's Tools",
    "weight": 6,
    "type": "Tool",
    "icon": 14
  },
  "cartographer_tools_artisan": {
    "id": "cartographer_tools_artisan",
    "name": "Cartographer's Tools",
    "weight": 6,
    "type": "Tool",
    "icon": 15
  },
  "cobbler_tools": {
    "id": "cobbler_tools",
    "name": "Cobbler's Tools",
    "weight": 5,
    "type": "Tool",
    "icon": 21
  },
  "cook_utensils": {
    "id": "cook_utensils",
    "name": "Cook's Utensils",
    "weight": 8,
    "type": "Tool",
    "icon": 23
  },
  "glassblower_tools": {
    "id": "glassblower_tools",
    "name": "Glassblower's Tools",
    "weight": 5,
    "type": "Tool",
    "icon": 34
  },
  "jeweler_tools": {
    "id": "jeweler_tools",
    "name": "Jeweler's Tools",
    "weight": 2,
    "type": "Tool",
    "icon": 52
  },
  "leatherworker_tools": {
    "id": "leatherworker_tools",
    "name": "Leatherworker's Tools",
    "weight": 5,
    "type": "Tool",
    "icon": 55
  },
  "mason_tools": {
    "id": "mason_tools",
    "name": "Mason's Tools",
    "weight": 8,
    "type": "Tool",
    "icon": 62
  },
  "painter_supplies": {
    "id": "painter_supplies",
    "name": "Painter's Supplies",
    "weight": 5,
    "type": "Tool",
    "icon": 71
  },
  "potter_tools": {
    "id": "potter_tools",
    "name": "Potter's Tools",
    "weight": 3,
    "type": "Tool",
    "icon": 76
  },
  "smith_tools": {
    "id": "smith_tools",
    "name": "Smith's Tools",
    "weight": 8,
    "type": "Tool",
    "icon": 96
  },
  "tinker_tools": {
    "id": "tinker_tools",
    "name": "Tinker's Tools",
    "weight": 10,
    "type": "Tool",
    "icon": 102
  },
  "weaver_tools": {
    "id": "weaver_tools",
    "name": "Weaver's Tools",
    "weight": 5,
    "type": "Tool",
    "icon": 113
  },
  "woodcarver_tools": {
    "id": "woodcarver_tools",
    "name": "Woodcarver's Tools",
    "weight": 5,
    "type": "Tool",
    "icon": 116
  }
}
