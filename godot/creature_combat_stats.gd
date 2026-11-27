extends RefCounted
## 2014 D&D 5e basic creature stats; imported GLBs do not contain gameplay stats.
## Gameplay uses these exact data fields; the Bestiary descriptions are not data.
const FEET_TO_METERS := 0.3048
const MONSTERS := {
    "Goblin Warrior": {
        "ac": 15, "hp": 7, "speed_ft": 30,
        "attack_bonus": 4, "damage_dice": 1, "damage_sides": 6,
        "damage_bonus": 2, "attack_type": "slashing", "attack_label": "Scimitar",
        "reach_ft": 5, "aggressive": true, "notice_m": 9.0,
        "attack_interval": 1.5, "xp": 50
    },
    "Twig Blight": {
        "ac": 13, "hp": 4, "speed_ft": 20,
        "attack_bonus": 3, "damage_dice": 1, "damage_sides": 4,
        "damage_bonus": 1, "attack_type": "piercing", "attack_label": "Claws",
        "reach_ft": 5, "aggressive": true, "notice_m": 7.0,
        "attack_interval": 1.5, "xp": 25
    },
    "Fire Beetle": {
        "ac": 13, "hp": 4, "speed_ft": 30,
        "attack_bonus": 1, "damage_dice": 1, "damage_sides": 6,
        "damage_bonus": -1, "attack_type": "slashing", "attack_label": "Bite",
        "reach_ft": 5, "aggressive": true, "notice_m": 6.0,
        "attack_interval": 1.5, "xp": 10
    },
    "Wolf": {
        "ac": 13, "hp": 11, "speed_ft": 40,
        "attack_bonus": 4, "damage_dice": 2, "damage_sides": 4,
        "damage_bonus": 2, "attack_type": "piercing", "attack_label": "Bite",
        "reach_ft": 5, "aggressive": true, "notice_m": 9.0,
        "attack_interval": 1.5, "xp": 50
    }
}
# Temporary test-character profile; character-class/equipment stats will replace it.
const TEST_PLAYER := {"ac": 10, "hp": 20, "attack_bonus": 4,
    "damage_dice": 1, "damage_sides": 6, "damage_bonus": 2, "reach_ft": 5}

static func get_monster(kind: String) -> Dictionary:
    return MONSTERS.get(kind, {}).duplicate()

static func attack_roll(rng: RandomNumberGenerator, bonus: int, ac: int) -> Dictionary:
    var natural := rng.randi_range(1, 20)
    return {"natural":natural, "total":natural+bonus,
        "hit":natural==20 or (natural!=1 and natural+bonus>=ac),
        "critical":natural==20}

static func d20_hit(rng: RandomNumberGenerator, bonus: int, ac: int) -> bool:
    return bool(attack_roll(rng,bonus,ac).hit)

static func damage_roll(rng: RandomNumberGenerator, profile: Dictionary,
        critical: bool = false) -> int:
    var damage := int(profile.get("damage_bonus", 0))
    # 2014 5e critical hit: double the damage dice, NOT the modifier.
    var count := int(profile.get("damage_dice", 1))*(2 if critical else 1)
    for die in count:
        damage += rng.randi_range(1, int(profile.get("damage_sides", 4)))
    return maxi(damage, 0)
