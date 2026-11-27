# Forest bestiary roaming integration — v0.3.171

## Implemented
- The Emerald Blade Warden and Goblin Alts 2–4 have new 15-bone humanoid rigs with Idle, Walk, and Attack animations. Original unrigged GLBs remain intact in the repository.
- The Goblin Warrior already has a 15-bone humanoid rig and the same three clips. The Fire Beetle retains its imported animated skeleton and Walk clip.
- Normalized display heights: Warden 1.42 m, other goblins approximately 1.22 m; Fire Beetle 0.52 m. All feet are aligned to the ground.
- Initial forest spawns: each goblin variant and one Fire Beetle, then two random extra enemies. Maximum eight active roaming enemies.
- Enemies pick random wandering directions, rotate to face movement, switch idle/walk animations, and respawn as needed. They wander around the player without combat pursuit yet.
- Attack removes the closest enemy within the existing five-foot range. Defeated enemies are replaced by random spawns.
- Fire Beetle has a warm, shadowless point light to represent its illumination, subject to on-device performance review.
- Bestiary shows explicit Goblins and Fire Beetle selection buttons above the model preview, with model animation preview where supported.

## Tests
- Run `python scripts/verify_enemy_content.py` to verify all GLB skin and animation metadata, beetle Walk, and catalog references.
- The new `enemy_spawner.gd` and modified `bestiary.gd` pass the local Godot 4.5.1 headless script parser.
- The production Android Godot 4.4.1 build is the definitive compilation and packaging test.

## Review still required
- New skin weights are algorithmically inferred. Examine shoulder, elbow, knee, wrist, and prop deformation during walking and attacking on the phone.
- Check real-device frame rate with eight enemies visible, and consider reduced counts or model LODs if necessary.
- Validate enemy facing and fire-beetle scale in-game.
- AI currently wanders only; future enemy awareness, pathfinding, and 2014 5e combat remain separate work.
