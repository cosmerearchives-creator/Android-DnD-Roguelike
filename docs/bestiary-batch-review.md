# Bestiary batch import — pending visual approval

Imported source candidates:
- Goblin Alt 2.glb (SHA-256 CF00EFB28A56E347BF49EDFEC2DDAE79D91AB320B7BC88D5425DDE052C7A753A): 194,132 -> 26,000 triangles in game copy.
- Goblin Alt 3.glb (SHA-256 AE04C80C21E69CD200CF8049A6C5CE73B6C99BEED59E52005DE4BA31BBDA7681): 1,025,214 -> 33,000 triangles in game copy.
- Goblin Alt 4.glb (SHA-256 C8AE8245E52C96ECAC80F130A92E598E01D3E24B3D1F13C25B62E9EBD3252921): 2,929,666 -> 40,000 triangles in game copy.
- Meshy_AI_Cybersecurity_Beetle_icon_as_a_geometric_20261127_animated_Walking.glb (SHA-256 224D18A4A07F36D21C8D1B75BAE92D20D77E4B5326FA90A3C9CCC9D0163BD3C0): ~575,257 -> ~35,080 triangles in game copy.

The game-ready derivatives are in `godot/assets/`, while original source assets move to `Beastiary Review`. Originals are NOT approved and must not be moved to `Beastiary Complete` until explicitly approved.

Three new goblin variants share the existing Goblin Bestiary entry and the existing Goblin statistics, and are eligible as alternate spawn visuals. They were unrigged in source, so the alternates remain static; no skeletal animations were invented. The existing Goblin Warrior and Emerald Blade Warden remain unchanged.

Fire Beetle is a separate Bestiary entry with the 2014 5e Giant Fire Beetle stat block (CR 0, AC 13, HP 4, bite +1, 1d6-1 slashing, illumination 10 ft. bright + 10 ft. dim). The imported source includes a 27-bone armature and walking animation. Illumination is currently listed as an ability, not implemented in world gameplay.

**Quality caveat:** This first mobile integration significantly decimated geometry to prevent very high-poly source models from exhausting Android performance. We have not measured real-device performance or compared close-up rendered quality with originals. The user must visually approve before completion. Source models were not modified.

**Asset harvesting:** Sources have single primary meshes; goblin equipment may be fused into their geometry. No independently reusable equipment has been extracted yet. Additional component extraction and source licensing review remain separate tasks.

**Validation:** Blender 5.3 exports completed; isolated Godot 4.4.1 imported all four, instantiated each model, confirmed beetle animator, and switched Bestiary entries. On-device review and FPS testing remain pending.

Reproducible export: `blender -b -t 2 --python scripts/prepare_bestiary_batch.py`.
