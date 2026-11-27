# Forests of Hell — Combat foundation v0.3.184

## Shipped behavior

- **Lock** at the top of the game HUD toggles automatic target acquisition, choosing the nearest living creature within **12 meters** and holding that creature until it dies or leaves the break range (18 meters). The creature receives a visible target marker with HP. If enabled with no creature in range, lock-on acquires the next eligible nearby creature.
- **A** and the large **Attack Mode** control both toggle automatic melee attacks. Their visual active states remain synchronized. Auto attacks use a **1-second cooldown**, only when a living enemy is within **5 feet**. Lock-on is optional; without lock-on, attacks target the nearest creature in melee reach.
- Manual **Attack** respects the same cooldown, prefers the locked target, faces the target at attack time, and reports misses, hits and enemy HP. While locked, the player also faces the target when moving.
- A test player has **20 HP, AC 10, +4 to hit, 1d6+2 damage**. These values are **explicitly provisional** until class, ability scores, equipment and the character sheet drive the combat profile.
- The HUD displays player HP/AC, auto/lock status and brief combat notifications. Enemy hits reduce HP, with a defeat notification at zero. In this test build, returning to the main menu and continuing starts a fresh test character health pool.
- Creature combat data is centralized in `godot/creature_combat_stats.gd` and used by actual attacks, movement and hit points. Source values match the Bestiary's 2014 D&D 5e AC/HP/attack bonus/damage for **Goblin Warrior, Twig Blight, Giant Fire Beetle (called Fire Beetle in game), and Wolf**.
- D20 natural 1 is an automatic miss; natural 20 automatically hits and doubles rolled damage dice but not the damage modifier. Creature damage cannot go below zero. Armor class is checked before damage is applied.
- Forest Goblins and Twig Blights chase within their notice range, turn toward the player, and attack from their 5-foot reach. Fire Beetles also attack for encounter testing. Their original 27-bone asset has no bite clip, so the attack has a small temporary visual lunge while its native `Walk` remains preserved.
- Fixed the in-world **Fire Beetle** direction with a 180-degree visual-axis correction. The Goblin Warrior's previously corrected forward-axis transform remains intact. The wolf is still available in Bestiary/Quadruped Animation Lab and does **not** yet spawn in the world.
- Creature combat movement uses each 2014 speed in feet per turn divided by six seconds, with a small local movement-speed variation. No changes to the original 3D source assets, skeletons or shared animations.

## Deferred full 2014 5e mechanics

This is a **combat foundation**, not a declaration of complete 2014 5e rules coverage. Creature traits like Goblin Nimble Escape, Twig Blight False Appearance, wolf Pack Tactics and wolf bite knockdown, ability-score saving throws, equipment-based defenses, player character progression, XP awards, stealth/perception and initiative/turn economy require further implementation. Enemy detection and continuous 1.5-second attacks are temporary real-time testing rules, not six-second D&D rounds.

## Verification

The Godot integration smoke test now covers all four creatures' AC/HP/attack profiles, strict humanoid and wolf rig validation, the full Bestiary, one-second manual/auto attack cooldown, lock state and target marker, player HP changes, enemy aggression and actual dice-driven enemy damage, and full-direction beetle/goblin facing. GitHub Actions also runs the integration smoke and both skeleton import gates before signing/publishing the APK.
