# Android DnD Roguelike

## v0.3.12 — refine the Wildlands character pose
- Gives the side profile a visible neck between the jaw and collar and matches the exposed base skin closer to the portrait.
- Rests the arms closer to the torso, with a slight idle sway and an alternating swing that follows the walk cycle.

## v0.3.11 — align the Wildlands rig seams
- Registers limb artwork at the joints in each facing. The base and armor use the same transforms.
- Trims duplicate shoulder, cuff, and knee artwork from the base layers and places both boots at the rig's ground contacts.

## v0.3.10 — single Wildlands character rig
- Removes the Original comparison mode and its full-body and walking atlases from the app. The Base Only / Armor On button sits on the right side of the HUD.
- Uses a dedicated face and hair atlas. Front, side, and back artwork attaches at the rig's collar, and the left-facing view mirrors the side profile.

## v0.3.9 — unify original portrait and rig world-space scaling
- Investigated the apparent 1.6× versus 1.8× zoom difference seen when switching between ORIGINAL and RIG TEST. Both renderers share the same 1.8× camera and receive the same .86f character scale; the toggle is an exclusive draw branch and does **not** paint two models at once.
- Found the actual regression: `RiggedElf.drawOriginalPortrait` scales the original image's 27–73% horizontal / 0–28% vertical head crop to a separately hand-sized rectangle (64 × 52 local units front), while `IllustratedFighter` maps the full 360×480 source to 125 × 166 local units. The portrait therefore looked about **12% larger** than the source body—similar to a 1.8 / 1.6 zoom discrepancy.
- Introduces a shared pure-Java `SpriteProjection`, used by both render paths, so crops of the source sprite retain the same pixel-to-world scale and foot anchoring. Removes the portrait's custom direction-dependent stretch. Adds a CI regression test guarding full-frame and head-crop dimensions.
- The original comparison mode remains unchanged; the modular rig, base/armor toggles, neck bridge, gait and environment are preserved. This corrects projection only. **The rig still borrows facial artwork from the approved original image until a standalone head atlas is authored**; it is not a completed independent head asset.

## v0.3.8 — restore anatomically connected neck in base and armored rig
- Four on-device BASE ONLY screenshots from v0.3.7 confirmed the masked portrait no longer contains the old shoulders, but now the High Elf's head looks detached from her torso in front and side facings because the crop also removed the painted collar/neck.
- Adds a **slender, direction-aware skin neck bridge** beneath the existing head/hair and independently illustrated torso. It overlaps the portrait's chin and the base garment neckline to eliminate terrain-colored gaps without reviving the original armored chest/shoulders.
- Keeps both rig body layers unchanged and retains the exact four-facing portrait, torso, gait IK, foot planting, animations, HUD, and world rendering. The rear view uses a subtly shaded neck beneath the hair.
- This is a local art correction; verify the visual result on the phone before subsequent proportion or animation changes.

## v0.3.7 — isolate the painted portrait from the rigged base body
- Phone screenshots of v0.3.6 revealed a remaining *second upper body* behind the real base torso. The approved original bitmap's supposed head crop included its original metal shoulders, chest and arm; changing painter order did not remove those pixels.
- The rig now clips that inherited illustration to separate head/hair silhouettes for front, rear and true side facings. The torso, shoulders and arms come only from the detachable illustrated rig layers, with no full-body fragments double-rendering.
- The lower leg is painted behind the thigh at its knee connection, and the skin bridge is smaller to reduce stark knee rings. Both changes use the existing base and armor atlas parts; no gait math, movement rate or facing changes.
- Keep checking actual device screenshots in BASE ONLY (front, side, back, moving), then ARMOR ON. The portrait clipping silhouettes are a targeted fix and still need actual phone review before treating the art as final.

## v0.3.6 — targeted base-body seam correction
- Fixes visible disconnected knees/elbows in BASE ONLY: per-part atlas cells have transparent padding, so painting the full source cell leaves illustrated limbs short of their rig endpoints.
- Measures the painted alpha bounds once at startup and uses a **shared crop for base and armor**, maintaining their alignment. Adds modest bone-end overlap and skin bridges behind both joint sections.
- Draws the approved head/hair portrait behind the new torso and arms, preventing baked-in shoulder/chest imagery from covering the independent base-body art.
- Four-facing stance, gait timing, equipment toggle, terrain, and gameplay are unchanged. Device screenshot/video testing is still needed, and final hair/armor illustration refinements remain future work.

## v0.3.5 — articulated base body and removable illustrated armor
- Introduces **two real illustrated transparent WebP layer atlases**, each 768×960 with **20 independent four-facing body-part images**: torso, upper arm, forearm/hand, thigh, and lower leg/boot. `elf_base_layers.webp` depicts an adult High Elf's fitted navy underclothing, leather footwear and skin. `elf_armor_layers.webp` contains matching fantasy silver/gold armor pieces and blue fabric. These are artwork elements, not regenerated full-body walking poses.
- Adds `ElfLayerSprites.java`, which attaches each illustrated body part to **the existing gait IK hip/knee/ankle and shoulder/elbow/hand positions**. The base layer is always drawn first; when enabled, each armor part draws over the **same** attachment transforms. Armor visibility does not change movement, proportions, stride length or facing.
- **RIG TEST** now defaults to **BASE ONLY**. A separate top-bar toggle changes between `BASE ONLY` and `ARMOR ON`, saved across launches. The existing `RIG TEST / ORIGINAL` comparison control still works; the original approved full-body elf artwork remains untouched.
- The normal path **no longer draws the diagnostic block-shaped thigh, shin and shoulder parts**; those primitives remain as fallback if assets fail to decode.
- Wildlands redraw now uses `postInvalidateOnAnimation()` instead of `postInvalidateDelayed(32)`. This requests a new frame on each display vsync, potentially ~60 FPS on a 60 Hz phone, but **real frame rate remains limited by device performance**. Gait phase continues to come from actual distance traveled, not frame count.
- The character's approved blonde elf portrait and four snap facings are preserved. The first-pass base and armor parts use distinct illustrated imagery and can still need alignment and occlusion refinements, particularly when joints flex or the side view overlaps. Existing combat, world, test-area isolation, pause navigation and APK auto-update are unchanged.
- CI decodes each atlas and verifies transparent background and all 20 populated cells.

## v0.3.4 — illustrated rig refinement and real side-facing anatomy
- Based on player screenshots of the v0.3.3 rig: the head turned sideways, but the shoulders, breastplate and arms still faced front.
- Adds `RigViewProfile.java`: four discrete facings with an actual **narrow profile torso**, much smaller screen-space shoulder separation, and side-view hand/elbow offsets. Left and right are mirrored immediately; **no turning interpolation** is added.
- Reorders side-view drawing by depth: the sword arm and sword sit **behind** the torso, while the shield arm and shield sit in front. Front and back continue to show both arms.
- **Upgrades the visual model** using selected pixels from the existing approved female High Elf Fighter illustration for the torso/breastplate, shin and thigh plates and sleeves, clipped and attached to the independently moving bones. Replaces conspicuous round shoulder joints with more organic silver-and-gold elven pauldrons; narrows side cloth and waist. The original image asset and the `ORIGINAL` comparison mode remain intact.
- Keeps the v0.3.3 upright hips, front/back depth-projected knees, distance-based planted foot gait, terrain, solo test, controls and pause actions unchanged.
- Adds pure-Java `RigViewProfileTest.java` regression checks that prevent side-view shoulders/torso from accidentally reverting to a broad frontal silhouette.
- **Limitation:** these are *runtime crops and shape masks* from our approved static illustration, not separately authored animation-quality armor layers. Some clipping, stretching or texture seam artifacts may remain, and the updated appearance still requires evaluation on an actual phone. Future asset work should create independent transparent limb, torso and gear layers once the proportions pass inspection.

## v0.3.3 — upright gait and front/back stepping correction
- Fixes the **crab-leg appearance** observed in the v0.3.2 screenshots: front and back legs no longer bend to the left/right of their hips. A new `RigLegPose.java` solves the two-bone gait in the physical **forward/depth plane** and *then* projects that movement into screen Y when facing front/back, or screen X when side-facing.
- Raises the pelvis/hip anchor from **-58** to **-74** local pixels to restore a straighter stance and longer-looking legs. Resting knees interpolate to a straight aligned position instead of remaining visibly crouched.
- Keeps the distance-controlled alternating stance/swing phases, feet lifting off the ground, modest torso bounce, full four-direction instant facing, and separate limb components.
- Narrows front/back boots and keeps knees and boots underneath their respective hips. Side-view strides remain visibly forward/back rather than crab-stepping.
- Adds `RigLegPoseTest.java` unit/regression checks for upright stance, knee placement, narrow front/back projections and real depth movement. CI runs these alongside the existing gait tests and Android build/lint.
- The **RIG TEST / ORIGINAL** toggle remains. Original approved elf art, face/hair, terrain, movement, UI, Arena, pause options and APK updater are preserved. The vector limb armor is **still a mechanics-only prototype**, not final illustrated armor. Phone video testing is still needed to verify how the motion looks.

## v0.3.2 — articulated walking laboratory

This is a **mechanics-first rigging prototype**, not another attempt to disguise the same static sprite as a walk cycle.

- Added `GaitModel.java`: a pure-Java distance-driven, two-foot gait. Each foot has an explicit **grounded stance phase** (64% of its cycle) and an **airborne swing phase** with sinusoidal lift. The opposite leg is half a cycle out of phase. Gait progression uses actual traveled world distance, so blocked movement does not animate footsteps.
- Added `RiggedElf.java`: distinct hips, **two-bone IK knees**, thighs, shins, independent moving boots, arms, hands, equipment grips, torso bob and subtle counter-sway. This is deliberately a **temporary styled-vector rig** so we can prove the movement before creating detailed separated armor layers.
- The rig reuses the approved female High Elf Fighter artwork **for her face and hair** from the existing four-facing atlas. The **original full-body art and eight-frame walk atlas remain packaged and untouched**.
- Added a visible **RIG TEST / ORIGINAL** toggle on the Wildlands HUD (top right), saved between launches. **RIG TEST is the default**. Toggle to ORIGINAL at any time to compare against the previous sprite-based walk without affecting gameplay.
- Direction switching is instantaneous; no turn animation. Existing camera, character creation, world, controls, solo test scene, attack preview, pause menu, Main Menu, Quit Game, and APK auto-updater remain unchanged.
- `GaitModelTest.java` executes in GitHub Actions using plain `javac/java` and checks distance/speed, alternating planted feet, lift boundaries and cycle continuity.

**Next art milestone:** Replace the rig's provisional vector breastplate, arm, thigh, shin and boot parts with **separately illustrated transparent layers** matching the approved B/Fantasy Adventurer outfit. This release is an animation *mechanics* test: it should show recognizably bending knees and alternating planted feet, but it intentionally does **not** claim final-quality skeletal character art. We have not seen a device gameplay video of this version.

## v0.3.1 — stride-synced walking, attack animation and richer Wildlands
- Ties the eight-frame walk atlas to distance traveled: one complete gait cycle per 1.2 world tiles, with the footfall phase reset when movement starts. This removes the slow-cycle mismatch that made the elf glide across the ground.
- Adds a short sword-swing animation preview in Wildlands using a directional lunge, layered slash trail and sparks. The preview does not apply damage or change exploration rules; combat remains in Arena.
- Replaces the ground atlas with a 4×4 high-detail painted terrain set for meadow, leaf litter, earth, stone, rock, creek and bridge surfaces; enlarges and increases the density of painted trees, rocks and shrubs while keeping paths and the starting clearing open.
- Preserves the character art, camera, terrain atlas, procedural map, HUD, and all existing systems.

## v0.3.0 — painted terrain / movement and UI pass
- Adds **real illustrated bitmap materials** rather than relying on flat procedural terrain: a 1024×1024 compressed WebP atlas of **16 original painted ground textures**, including meadow, fern/leaf litter, dry grass, two dirt trails, stone cobbles, mossy ruins, rock/cliff, creek, timber bridge, flowers and gravel. Runtime draws these clipped into isometric tiles; 16 prepared transparent diamond bitmaps avoid costly per-frame clipping.
- Adds a second **1024×256 alpha WebP atlas of four foliage/rock props**: pine, rounded woodland tree, boulder and bush. These are composited from the original painted textures; remaining special ruin landmarks still use prototype vector geometry.
- Synchronizes the existing elf's eight-frame directional walk playback with **distance actually moved in world space** rather than wall-clock time; stride and subtle lift accompany alternating footfalls. Adds a short idle-to-walk blend. Facing still switches instantly with no turning sequence. **The eight-frame atlas is still illustration-derived, not separately hand-drawn foot-plant poses. Video-based correction remains unverified because no gameplay recording was attached.**
- Keeps the Wildlands exploration scene **player-only**, preserving character design, sprite size, and approved 1.8× camera.
- Retains the navy-and-gold HUD and touchscreen controls. Rescales the paused overlay to fit landscape displays so **Resume**, **Return to Main Menu**, and **Quit Game** are all accessible.
- Preserves character creation, the separate classic Arena, existing game logic, app signing, and GitHub APK updater.
- Limitations: illustrated material transitions and prop variety are first-pass, not a complete tileable edge/autotile set; animation gait needs device video feedback and may retain some sliding.

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
