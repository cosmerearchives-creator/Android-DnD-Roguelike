# Android DnD Roguelike

## v0.2.9 — solo elf visual/animation test and pause navigation
- Outdoor Wildlands visual prototype **shows only the main character**. The Guardian/Arcanist were hidden in this testing scene; party/combat systems and the classic Arena remain unchanged.
- Upgrades the elf to an **eight-frame walking atlas for each of four directions** (32 transparent frames), with stronger leg and opposing arm motion, a synchronized footfall shadow, and immediate facing changes without any turning animation. This is still an illustration-derived gait prototype, not a complete hand-animated skeletal walk.
- First illustrated-terrain and HUD polish: warmer forest greens, more varied foliage, detailed fir trees, stone surfaces, river highlights, dark navy panels, muted gold edging and elven-style controls. Keeps camera at the approved 1.8×.
- Pausing now exposes **Resume**, **Return to Main Menu**, and **Quit Game**. Android Back toggles pause. Return to Main Menu closes the exploration activity and restores the existing main-menu flow; Quit Game finishes the app task.
- CI checks that the 2880×1920 animated atlas has all 32 alpha-bearing frames before running the Android build.

## v0.2.8 — basic directional walking and idle motion
- Keeps the approved B-style Fantasy Adventurer female High Elf art, four isometric directions, and 1.8× camera.
- Adds `app/src/main/assets/elf_fighter_walk.webp`: **four frames in each of four directions**, rendered from the approved art with modest leg/torso deformation rather than independently redrawn poses. All 16 frame cells use consistent dimensions, translucency, and alignment.
- The sprite renderer chooses a direction *immediately* as movement changes, with no turning animation, blend, or tween.
- Walking advances only when the player actually changes position; when stopped, blocked, or paused, the character returns directly to an idle frame. Idle breathing is subtle and does not require additional artwork.
- Preserves combat prototype, gear, companions, procedural terrain, camera, and depth ordering. **Limit:** this is a first procedural gait treatment. It is not yet a hand-illustrated anatomically accurate walk cycle, hair/cloth physics, or full attack animation.
- CI validates both the idle and walking sprite atlases and builds/lints before any versioned release.

## v0.2.7 — Fantasy Adventurer armor selection (B)
- Replaces the gameplay female High Elf Fighter artwork with the approved **B / Fantasy Adventurer** armor reference: more feminine sculpted medium armor, exposed upper arms, lighter thigh coverage, ornate silver-and-gold elven detail and blue tabard, while preserving sword and shield.
- Keeps youthful adult features, distinctly pointed ears and long golden-blonde hair partially tied back.
- Four isometric-facing illustrated frames (front, right, back, left) are now a **1440×480 transparent WebP atlas**, 360×480 source pixels per frame. Git binary SHA verified against the prepared asset before committing.
- Preserves existing 1.8× camera, movement, combat test arena, companions, terrain and depth sorting. This is a static visual benchmark, not modular equipment customization or true walk-cycle animation.

## v0.2.6 — perspective-correct female High Elf art test
- Replaces the old front/side studio-style sprites with four newly illustrated **isometric-angle** views of the same young-adult female elf fighter (front, right, back, left), with golden-blonde partially tied hair, pointed ears, semi-practical silver armor, sword, and shield.
- Packages a fully verified **960×320 RGBA WebP** atlas (240×320 source pixels per facing). The original asset bytes were transferred directly to GitHub and verified against its Git blob SHA; no extra downsampling for upload.
- Updates the sprite facing indices to the new image order. The camera stays at the approved 1.8× zoom.
- The Wildlands renderer now draws the player and placeholder companions at their terrain depth positions rather than as a final foreground overlay. Nearby trees, props, and foreground elevation can occlude characters naturally. A subtle shadow anchors the player's feet.
- Preserves the existing dungeon arena, exploration, character data, and updater. The new atlas is a fixed visual test: no fully modular equipped gear or genuine walking animations yet.

## v0.2.5 — closer illustrated elf visual pass (pending release verification)
- The camera is now 1.8× (was 1.6×) as approved. The player's illustrated sprite is slightly wider and taller for better face, hair and armor readability; companion/environment artwork and all gameplay systems are unchanged.
- Replaced the previous 320×105 four-view WebP asset (80×105 per view) with a 640×210 four-view asset (160×210 per view), maintaining alpha transparency. This uses more source pixels but is still a compressed static prototype rather than a full-resolution production atlas.
- The renderer uses bitmap filtering and continues to select the same four directions from movement.
- CI now decodes each image to verify all four illustrated frames before Android builds; this prevents a silent fallback to the geometric model from a broken atlas.
- Equipment colors and forms are still baked into this single prototype sprite; interchangeable equipment layers and walk animations are not implemented in this pass.

## v0.2.3 — illustrated female High Elf sprite integration
- The four-facing **female High Elf Fighter sprite atlas** is now stored in `app/src/main/assets/elf_fighter_atlas.webp`. It is a transparent, downsampled WebP test asset (320 × 105, four horizontal 80 × 105 views), not a final production-quality texture.
- The outdoor renderer loads the WebP atlas and switches front/right/back/left faces based on movement direction. The older vector hero remains a fallback only when the atlas cannot be read.
- Female Fighter test art: youthful adult features, visible pointed ears, golden-blonde partially tied-back hair, medium-weight semi-practical plate and cloth, sword and shield.
- Four static views with slight movement bob only, **not** finished gait animation. Atlas is one fixed equipment look and does **not yet** match arbitrary character creator customizations.
- Prior combat arena and exploration mechanics remain intact. No additional camera zoom was imposed.

## v0.2.2 — illustrated character and clearing prototype
- The outdoor player is drawn with a new independent layered `IllustratedFighter` renderer (hair, pointed ears, face, tunic, armor, gold trim, boots, blade and optional shield) and uses saved character-creator selections. Four facing orientations and a basic animated gait are supported.
- Outdoor scenery now has layered stone pavers, grass clusters, flowers and a weathered broken arch at ruin landmarks.
- Keeps the closer 1.6x isometric camera, the existing procedural exploration scene and the original combat arena.
- **Art caveat:** this is an original vector-rendered *intermediate* art test, **not yet** the painted, high-resolution sprite artwork shown in the earlier concept mockup. Detailed authored character/environment texture sets and polished directional animation remain next-stage deliverables.
- This version does not change combat mechanics or procedural persistence.

## v0.2.0 — first isometric outdoor exploration vertical slice
- Added **Explore Wildlands** on the main menu, and completed character creation now leads into the outdoor scene rather than the classic test arena.
- New `OutdoorActivity` renders a three-quarter isometric view with a camera following the player and deterministic, coordinate-based outdoor terrain generation.
- Initial generated terrain contains interpolated climate/moisture biomes, tiered elevation with basic movement checks, a meandering road and river, an illustrative bridge, vegetation, rock obstacles and handcrafted ruin landmarks.
- Joystick and tap-to-move are retained; joystick cancels a previously targeted destination. The scene shows the Fighter's name with two placeholder party members and a live location/terrain HUD.
- **ARENA** opens the unchanged combat prototype. The new outdoor vertical slice is for exploration; it does not yet have integrated outdoor encounters, finished art assets, robust long-distance pathfinding, proper elevation connectors, map/fog UI, tile streaming/cache, saves for procedural world state, or generated varied expedition seeds.
- The draw code generates world tiles deterministically by coordinate and culls those outside the current camera view. This is not yet a production chunk-streaming system.

## v0.1.8 — visual character forge
- Replaced the raw vertical field list with a dark-fantasy Character Forge layout: clear step navigation, grouped choice cards, progress controls, live stats footer, and portrait/landscape adaptations.
- Added an illustrated character preview with distinct full-body and top-down modes, four directional views, and idle/walk/attack preview selectors.
- Preview responds to selected skin, hair, clothing color, armor, primary weapon and shield; the preview is original illustrative artwork, not yet a shared animation asset with the actual arena.
- Keeps existing creation options, draft persistence and gameplay entry. Advanced face/body editors and full matching gameplay sprites are still future work.

## v0.1.6 — first character creator (initial playable milestone)
- Added a native Android High Elf Fighter creation wizard with identity and basic appearance choices, 4d6-drop-lowest score sets (up to three rerolls), manual ability assignment, background, Fighter skills, Fighting Style, racial cantrip selection, and starting equipment choices.
- Added character review and local draft/completed-character persistence. The completed character replaces the default Hero in the arena, with selected HP, basic ability-derived weapon calculations, AC, personal currency, and equipment.
- Equipment choices support Fighter's armor, martial weapons, crossbow or handaxes, and Explorer's/Dungeoneer's packs, along with starter background equipment.
- **Current limits:** this is the first playable creator, not the complete agreed design. Advanced visual customization and preview, personality writing, full background features, cantrip casting, Fighter class abilities, item interactions, and gameplay progression are not all implemented. A full gameplay save beyond the completed creation choices is also pending.
- Release publication is now gated to changes in `version.properties` (or explicit manual dispatch); other pushes still run build/lint without repeatedly attempting to publish the same version.

## v0.1.4 — starting equipment foundation (in progress)
- Added a 2014-based equipment catalog covering Fighter weapon options, armor, shields, adventuring supplies, tools, containers, and key background objects.
- Added a character inventory model with item weights, container capacity, starting pack lists, gear slots, load limits, and weightless personal currency.
- The Hero prototype uses an initial chain-mail, longsword, shield, handaxe, and Explorer's Pack demonstration loadout.
- Added a BAG interface to inspect items, equip weapons and armor, change Versatile grips, eat rations, and drink carried water.
- The prototype's equipped weapon now drives basic damage dice and attack checks; equipped armor/shields drive AC and carried weight influences movement.
- **Not yet complete:** character creator equipment choices, all weapon special properties, persistent saves, full container management UI, dropped/recoverable thrown items, timers/lighting for torches, background-specific contextual interactions, and several consumable mechanics. Existing combat is still a prototype.
- Build/release status should be checked in GitHub Actions; this README does not imply an APK has passed CI.

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
