# Android DnD Roguelike

Native Android top-down fantasy roguelike prototype with GitHub Releases update support.

## v0.1.3 — deliberate six-second combat rhythm
- A level-one hero, guardian, arcanist, or goblin performs at most one Attack action per six seconds of active simulation time.
- Attack actions begin as soon as a valid target is in range and line of sight; no action is spent if the attack cannot execute.
- The next-action countdown is shown for each party member on the HUD.
- Attack-action code supports a future attacksPerAction statistic for class features such as Extra Attack. This does not grant extra actions.
- Tactical pause stops the countdown and combat simulation.
- Walking and enemy pursuit move more slowly, so battle positioning feels deliberate.
- This is a six-second **action cadence** prototype, not yet a full 5e turn/initiative system. Bonus actions, reactions, spellcasting, and class feature progression remain future tasks.

## v0.1.2 — grounded stylized-fantasy visuals
- Three visually distinct illustrated party members: sword-and-shield hero, armored guardian, and staff-wielding arcanist.
- Goblins with visible weapons, ears, faces, and alert states.
- Stone dungeon tiles, masonry obstacles, decorative barrels/crates, and torchlight.
- Combat hit bursts, active-character and targeted-enemy rings, improved HUD framing.
- The visual renderer is in FantasyArt.java, separate from gameplay and collision for future AI-assisted artwork replacement.
- Existing arena controls and first prototype gameplay are preserved.

**Art note:** This is an intermediate code-drawn illustrative art pass, not a final externally illustrated sprite pack. Decorations are visual only and do not yet alter physics. This is not yet an animated sprite atlas or modular paper-doll system.

## v0.1.1 — first playable prototype
Tap **Play prototype** from the app's main screen.

- A top-down dungeon-arena test map with solid obstacles.
- Three party members: Hero, Guardian, and Arcanist.
- Tap any party portrait to switch direct control; the others follow and auto-attack.
- Drag the bottom-left virtual joystick, or tap on the map to move.
- Tap an enemy to focus party attacks.
- Tap PAUSE/RESUME to freeze and continue the simulation.
- Enemies have basic circular awareness, line-of-sight obstruction, and delayed engagement.
- Enemy waves and party health allow testing of combat.
- Back button pauses the prototype.

**Prototype limitations:** This is a mechanics test, not a finished D&D game. It does not yet implement spellcasting, tactical queued commands, customizable AI tactics, procedural dungeons, character creation, inventory, long-term progression, or permanent saves. The awareness and combat simulation are preliminary.

## App updates
The installed starter remains the main menu. It checks stable GitHub Releases on startup and when pressing **Check for updates**. It downloads and verifies the new APK, and Android asks for installation approval.

The repository workflow builds and lints on pushes to main. If the existing SIGNING_KEYSTORE_BASE64 Actions secret is present, it also publishes a signed APK and update.json. The signing identity and applicationId must remain unchanged for in-place updates.

Current version.properties: versionCode=4, versionName=0.1.3. Increment both for the next release.

## Build
JDK 17, Android SDK 35, Gradle 8.11.1, Android Gradle Plugin 8.9.1.

Run: `gradle :app:assembleDebug :app:lintDebug`.

## Long-term direction
Real-time top-down RPG, three-character party with switchable direct control and customizable companion AI; D&D 5e-inspired statistics; a tactical pause system; multi-floor expeditions with hub progression; procedural rooms, fog of war, and destructible terrain. Design features will be delivered incrementally rather than represented as implemented in this first prototype.
