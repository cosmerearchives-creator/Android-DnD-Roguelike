# Creator v0.3.204: source-frame scale correction

The original 1900x1900 transparent source-elements sheet and 1673x940 reference screenshot remain byte-identical and full resolution.

Cause: the previous crop named blank_tall_panel.png included a vertical post, inventory buttons, dropdowns, item-grid strips and other adjacent UI. Scaling it independently into four Body panels repeated unwanted art across the character creator. The old preview_stone.png crop was only a small corner, not a full pedestal.

Changes: Reproducible source-art extraction is in scripts/creator_clean_art.py and lossless nine-slice art construction in scripts/build_creator_nineslice.py. The Godot creator now references clean self-contained panels, buttons and stone pedestal. The sidebar and four right controls use NinePatchRects so panel corners retain their sizes instead of being distorted. Buttons use source-backed StyleBoxTextures, and Height, Frame and Weight use gold slider tracks with diamond handles. The original user-supplied model and its 66-bone rig are not changed; Next still saves appearance and enters the game.

Local verification: Godot 4.5.1 creator_workshop_smoke.gd and creator_v204_scale_smoke.gd passed (8 sidebar categories, 7 navigation controls, Body/Hair/Beard panels, six NinePatchRects and 1600x900 layout fitting). Android and Windows CI tests, visual screenshot comparison and on-device behavior are independent gates still requiring validation.

Known limits: This corrects the worst compositing and scale problems, but is not yet pixel-identical to the reference. Landscape artwork is still scaled into the existing gameplay-oriented viewport; no new skeleton, gender morph or body-marking asset is invented. The original raw files remain on disk at full resolution.
