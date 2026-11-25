# Wildlands procedural world development

The main **Explore Wildlands** and **Continue** flows already launch `OutdoorActivity`, not the 5×5 `IsometricRoomActivity` test scene. The outdoor world generates terrain deterministically for any integer world coordinate using coordinate hashing, interpolated noise, biome, elevation, river and road rules. It uses a bounded 64×64 cache and draws only visible tiles. The joystick already supports continuous floating-point positions, normalized diagonal movement and axis-separated collision. The isometric room is a separate legacy demonstration with grid-based tap routing.

This branch adds persistence for the outdoor player's coordinates when entering new tiles, allowing exploration to continue after restarting. It does **not** claim that the complete envisioned world (towns, dungeons, encounters, quests, saves for all dynamic objects, etc.) is implemented. Those are future game systems and should be developed incrementally.

The Blender Petite Pale Elf model is a first procedural mesh proof of concept, not a finished game-ready model. Its animation atlas and `.blend` file are CI artifacts until visually reviewed and approved for integration.

## World clock and outdoor lighting

The hub, tavern, and wilderness in the playable world share the clock saved under
`wildlands_world`. A new character or a legacy save without clock data starts
on Day 1 at 8:00 AM. Editing an avatar retains the current time. The clock follows
monotonic elapsed time during focused gameplay, including when standing still.
The pause control, activity lifecycle, and modal dialogs stop the clock. Periodic
checkpoints and lifecycle saves preserve time without counting time offline.

| World time | Outdoor environment in clear weather |
| --- | --- |
| 12:00 AM–4:00 AM | Darkness |
| 4:00 AM–6:00 AM | Dim light |
| 6:00 AM–8:00 AM | Normal light |
| 8:00 AM–4:00 PM | Normal light with direct sunlight |
| 4:00 PM–6:00 PM | Normal light |
| 6:00 PM–8:00 PM | Dim light |
| 8:00 PM–12:00 AM | Darkness |

Canopy shade follows a ground ellipse sized and offset from the rendered crown.
The crown width is measured from each tree sprite, and its depth is estimated
from the isometric projection because these assets lack top-down canopy masks.
Trunk line-of-sight remains independent. Bush footprints supply natural
obscuration. The tavern roof blocks sunlight and the interior retains dim light.

The bed is in the far corner of the hub tavern. Use Interact near the bed,
choose an hour, then confirm. Selecting the current hour waits until its next
occurrence tomorrow. The preview shows elapsed time rounded up to the displayed
second, and the clock advances to the exact hour. The same clock can be advanced
by future rest actions. Waiting changes the time only and saves it immediately.

The lighting pipeline combines the schedule, saved weather, environmental
cover, overlapping light sources, and magical darkness. Clear is the only
available weather. Outdoor exposure and light intensity are resolved separately.
Geometry can block light rays. Changes in the lighting revision invalidate
current visibility and restart the distant terrain survey while retaining
previous discovery.

### Verification and future work

- Automated coverage includes clock rate and lifecycle gate logic, restored
  time, waiting and cancellation math, each lighting boundary, canopy geometry,
  light-source overlap and obstruction, ordinary and superior darkvision,
  magical darkness, sight Perception modifiers, and existing awareness and fog
  exploration behavior.
- Device verification remains separate from those checks. Lifecycle callbacks,
  the bed UI and hub path, art appearance and performance, save migration,
  character creation, controller input, and installer behavior need a device
  playthrough.
- Deferred weather work includes additional conditions (partly cloudy,
  overcast, rain, thunderstorms, fog and snow), weather generation and
  transitions, and weather-related natural obscuration.
- Deferred gameplay includes deployable lighting equipment and spells,
  Darkness spell placement, short rests, long rests, Elven Trance,
  and time-dependent NPC schedules and world events.
