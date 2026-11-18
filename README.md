# Android DnD Roguelike

Native Android top-down fantasy roguelike prototype with GitHub Releases update support.

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

Current version.properties: versionCode=2, versionName=0.1.1. Increment both for the next release.

## Build
JDK 17, Android SDK 35, Gradle 8.11.1, Android Gradle Plugin 8.9.1.

Run: `gradle :app:assembleDebug :app:lintDebug`.

## Long-term direction
Real-time top-down RPG, three-character party with switchable direct control and customizable companion AI; D&D 5e-inspired statistics; a tactical pause system; multi-floor expeditions with hub progression; procedural rooms, fog of war, and destructible terrain. Design features will be delivered incrementally rather than represented as implemented in this first prototype.
