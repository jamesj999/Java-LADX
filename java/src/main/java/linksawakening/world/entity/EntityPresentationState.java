package linksawakening.world.entity;

import linksawakening.entity.EntitySpriteSelection;

/** Owned PresentationState storage; preserves ROM slot indexing. */
final class EntityPresentationState {
    private static final EntitySpriteSelection OUTDOOR_EMPTY = emptySelection(EntityRoomLoader.RoomTable.OVERWORLD);
    private static final EntitySpriteSelection INDOOR_EMPTY = emptySelection(EntityRoomLoader.RoomTable.INDOORS_A);
    private EntitySpriteSelection emptySelection = OUTDOOR_EMPTY;
    private EntitySpriteSelection spriteSelection = OUTDOOR_EMPTY;

    private static EntitySpriteSelection emptySelection(EntityRoomLoader.RoomTable table) {
        return new EntitySpriteSelection(table, -1, -1, new int[4], false, new int[8][4]);
    }

    void initializeSelection(EntitySpriteSelection selection, boolean indoor) {
        emptySelection = indoor ? INDOOR_EMPTY : OUTDOOR_EMPTY;
        writeSpriteSelection(selection);
    }

    boolean hasSpriteSelection() {
        return spriteSelection != emptySelection;
    }

    /** Optional snapshot metadata: absence remains source-compatible for external consumers. */
    EntitySpriteSelection snapshotSelection() {
        return hasSpriteSelection() ? spriteSelection : null;
    }

    private int bgPaletteEffectAddress;
    private int paletteDataFlags;
    private int defaultMusicTrack = 0x05;
    private boolean activeMusic;
    private int pendingMusicTrack = -1;
    private int pendingMusicFadeOutCountdown;

    EntitySpriteSelection readSpriteSelection() {
        return spriteSelection;
    }

    EntitySpriteSelection writeSpriteSelection(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    value) {
        return spriteSelection = java.util.Objects.requireNonNullElse(value, emptySelection);
    }

    int readBgPaletteEffectAddress() {
        return bgPaletteEffectAddress;
    }

    int writeBgPaletteEffectAddress(int value) {
        return bgPaletteEffectAddress = value;
    }

    int readPaletteDataFlags() {
        return paletteDataFlags;
    }

    int writePaletteDataFlags(int value) {
        return paletteDataFlags = value;
    }

    int readDefaultMusicTrack() {
        return defaultMusicTrack;
    }

    int writeDefaultMusicTrack(int value) {
        return defaultMusicTrack = value;
    }

    boolean readActiveMusic() {
        return activeMusic;
    }

    boolean writeActiveMusic(boolean value) {
        return activeMusic = value;
    }

    int readPendingMusicTrack() {
        return pendingMusicTrack;
    }

    int writePendingMusicTrack(int value) {
        return pendingMusicTrack = value;
    }

    int readPendingMusicFadeOutCountdown() {
        return pendingMusicFadeOutCountdown;
    }

    int writePendingMusicFadeOutCountdown(int value) {
        return pendingMusicFadeOutCountdown = value;
    }
}
