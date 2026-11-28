# Character Creator visual polish — v0.3.205

## Approved scope
Refine the existing Appearance workshop without rebuilding or altering the supplied female humanoid skeleton. Target is the user's late-November 2026 1673×940 desktop screenshot and earlier workshop reference. Maintain the seven navigation entries, eight Appearance categories, Body controls, Hair/Beard attachment selection, live preview, zoom/rotation, Back and Next-to-game actions.

## Implemented
- Replaced scaled high-ornament button artwork on the top navigation and left Appearance categories with compact bronze/leather buttons sourced from the existing transparent UI sheet; active selection now has a visibly warm orange highlight.
- Introduced a separate small square bronze frame for rotation, zoom and slider arrow controls. Ornate gold scrollwork remains on Back, Next, the title and major panel borders.
- Built three visible gold slider rails using native Godot ColorRects rather than relying on a theme texture that was rendering invisibly under the HSlider. Their diamond handles are rendered at a compact native resolution; Height, Frame and Weight labels and captions clear neighboring borders.
- Replaced the cropped stone fragment (including its unwanted post) with the complete standalone circular/elliptical stone pedestal from the original transparent source. Repositioned it under the existing live female-base feet without touching the model, original maps or 66-bone rig.
- Captured actual OpenGL 1673×940 review image on Stormfather: `G:/ChatGPT Projects/Forests of Hell/Creator Review/creator_v205_review.png`. Visually compared against the user's screenshot, then refined caption and pedestal alignment.

## Art and skeleton integrity
The unchanged original source remains `godot/creator_ui/source_elements.png`, 1900×1900 RGBA, SHA-256 4263B46C00712ED5C7B987CF0C426E576410B1619467DF8EE0FC66CA42579623. The new source-derived assets are `creator_stone_oval.png` (173×87), `creator_button_compact.png` (420×100), `creator_button_compact_active.png` (420×100), `creator_arrow_compact.png` (130×110), and `creator_slider_diamond_compact.png` (38×44). Build reproducibly with `python scripts/refine_creator_v205_art.py`. No original texture, skeleton, animation, bone hierarchy or bone map changed.

## Tests and limits
Local Godot 4.5.1 runs passed: `creator_workshop_smoke.gd`, `creator_v204_scale_smoke.gd`, `character_appearance_smoke.gd` and new `creator_v205_ui_smoke.gd`. The old `creator_preview_smoke.gd` initially failed because it expected the deprecated menu-embedded creator; updated it to assert the actual dedicated workshop with model, hair scaling, zoom and cleanup, and it passed. Verified full OpenGL screenshot at target resolution.

Release-specific gates remain separate: signed Android APK, Windows ZIP, updater manifests and on-device inspection. The current workshop intentionally leaves Race/Class/Background etc. and unsupported morph/appearance categories unimplemented; no new skeletons or body mesh replacements were made. This is a visual fidelity iteration, not a complete implementation of every character creation option.

## v0.3.205 publication verification — 2026-11-28
GitHub Actions run 46163037849 completed SUCCESS, including production signing and stable release publication. Release v0.3.205 targets d5a5f4043a08780214c12c885a50cfda55f87ef2. Verified public Android APK (540573185 bytes), Windows x64 ZIP (544121874 bytes), and both live updater manifests at /releases/latest/download/{update.json,windows-update.json}: versionCode 226, versionName 0.3.205, matching release download URLs and SHA-256 digests. Android manifest SHA d5d78ce51abc587981918c7027f62631e94d3a1f3b6341ebd72ce34607d83acf; Windows manifest SHA bd64bd7de555672e09fa9a176f71138e7d184f6c528310ebaec7024ef83039ea. On-device visual/UI operation remains unverified.
