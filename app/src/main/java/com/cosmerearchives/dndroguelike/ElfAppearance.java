package com.cosmerearchives.dndroguelike;

/** Playable High Elf appearances, in the Wildlands model button's order. */
enum ElfAppearance {
    CLASSIC("classic", "CLASSIC ELF", 72, 0),
    BRONZE("bronze", "TALL BRONZE", 78, 0xFF9B6549),
    COMPACT("compact", "COMPACT FAIR", 67, 0xFFCC9072),
    SILVER("silver", "SILVER CURLS", 74, 0xFF714B38),
    BRAIDED("braided", "AUBURN BRAID", 76, 0xFFB17D54),
    PETITE("petite", "PETITE PALE", 63, 0xFFDFB39B),
    COILED("coiled", "COILED PUFF", 70, 0xFF925538),
    BOB("bob", "TAWNY BOB", 66, 0xFFB0815F),
    PETITE_ALT("petite_alt", "PETITE ALT", 63, 0xFFDDB09A);

    private static final ElfAppearance[] ROSTER = values();
    final String id;
    final String label;
    final float heightScale;
    final int fistSkin;

    ElfAppearance(String id, String label, int heightInches, int fistSkin) {
        this.id = id;
        this.label = label;
        this.heightScale = heightInches / 72f;
        this.fistSkin = fistSkin;
    }

    ElfAppearance next() {
        return ROSTER[(ordinal() + 1) % ROSTER.length];
    }

    static ElfAppearance fromId(String id) {
        for (ElfAppearance appearance : ROSTER) {
            if (appearance.id.equals(id)) return appearance;
        }
        return CLASSIC;
    }
}
