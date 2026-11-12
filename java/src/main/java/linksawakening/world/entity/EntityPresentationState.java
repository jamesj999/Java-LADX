package linksawakening.world.entity;

import linksawakening.entity.EntitySpriteSelection;

/** Owned PresentationState storage; preserves ROM slot indexing. */
final class EntityPresentationState {
    private EntitySpriteSelection spriteSelection;
    private int bgPaletteEffectAddress;
    private int paletteDataFlags;
    private int defaultMusicTrack = 0x05;
    private boolean activeMusic;
    private int pendingMusicTrack = -1;
    private int pendingMusicFadeOutCountdown;

    EntitySpriteSelection readSpriteSelection() {
        return spriteSelection;
    }

    EntitySpriteSelection writeSpriteSelection(EntitySpriteSelection value) {
        return spriteSelection = value;
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
