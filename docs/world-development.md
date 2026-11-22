# Wildlands procedural world development

The main **Explore Wildlands** and **Continue** flows already launch `OutdoorActivity`, not the 5×5 `IsometricRoomActivity` test scene. The outdoor world generates terrain deterministically for any integer world coordinate using coordinate hashing, interpolated noise, biome, elevation, river and road rules. It uses a bounded 64×64 cache and draws only visible tiles. The joystick already supports continuous floating-point positions, normalized diagonal movement and axis-separated collision. The isometric room is a separate legacy demonstration with grid-based tap routing.

This branch adds persistence for the outdoor player's coordinates when entering new tiles, allowing exploration to continue after restarting. It does **not** claim that the complete envisioned world (towns, dungeons, encounters, quests, saves for all dynamic objects, etc.) is implemented. Those are future game systems and should be developed incrementally.

The Blender Petite Pale Elf model is a first procedural mesh proof of concept, not a finished game-ready model. Its animation atlas and `.blend` file are CI artifacts until visually reviewed and approved for integration.
