package linksawakening.ui;

import java.util.Objects;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.OcarinaSong;
import linksawakening.state.PlayerState;

/**
 * State machine for the Ocarina song popup in {@code func_020_6111}.
 *
 * <p>The game stores the selection as a zero-based index, but the popup's
 * three entries are enabled by masks {@code $04}, {@code $02}, and
 * {@code $01}.  Horizontal movement therefore keeps wrapping until it lands
 * on a song whose bit is present in {@link PlayerState#ocarinaSongFlags()}.</p>
 */
// Selection direction is a signed menu step, not a cardinal facing.
public final class OcarinaSongMenu {

    public static final int SONG_COUNT = 3;
    public static final int OPEN_CLOSE_FRAMES = 0x10;

    private final PlayerState playerState;
    private final GameplaySoundSink soundSink;

    private boolean open;
    private int openingFrames;
    private int closingFrames;
    private int animationFrame;
    private boolean closeInventoryAfterClosed;
    private boolean closeInventoryRequestReady;

    public OcarinaSongMenu(PlayerState playerState, GameplaySoundSink soundSink) {
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
    }

    /** Requests the ROM's 16-frame opening animation. */
    public boolean requestOpen() {
        if (playerState.progression().ocarinaSongFlags() == 0 || isVisible()) {
            return false;
        }
        open = true;
        openingFrames = OPEN_CLOSE_FRAMES;
        closingFrames = 0;
        animationFrame = 3;
        return true;
    }

    /** Starts the ROM's 16-frame closing animation. */
    public void requestClose(boolean closeInventoryAfterClosed) {
        this.closeInventoryAfterClosed |= closeInventoryAfterClosed;
        if (!isVisible() || closingFrames != 0) {
            return;
        }
        open = false;
        openingFrames = 0;
        closingFrames = OPEN_CLOSE_FRAMES;
        animationFrame = 0;
    }

    /**
     * Advances one frame. Returns true on the frame the popup has finished
     * closing, matching the point where the original restores shared tiles.
     */
    public boolean tick() {
        if (closingFrames != 0) {
            closingFrames--;
            animationFrame = Math.min(3, (OPEN_CLOSE_FRAMES - closingFrames) * 4 / OPEN_CLOSE_FRAMES);
            if (closingFrames == 0) {
                closeInventoryRequestReady = closeInventoryAfterClosed;
                closeInventoryAfterClosed = false;
                animationFrame = 0;
                return true;
            }
            return false;
        }

        if (openingFrames != 0) {
            openingFrames--;
            animationFrame = Math.min(3, openingFrames * 4 / OPEN_CLOSE_FRAMES);
        }
        return false;
    }

    /** Returns whether the popup has been requested and is not fully closed. */
    public boolean isVisible() {
        return open || openingFrames != 0 || closingFrames != 0;
    }

    /** Mirrors {@code wOcarinaMenuOpen}; closing clears this before animation ends. */
    public boolean isOpen() {
        return open;
    }

    public boolean isReady() {
        return open && openingFrames == 0 && closingFrames == 0;
    }

    public int animationFrame() {
        return animationFrame;
    }

    public int selectedSongIndex() {
        return playerState.progression().selectedSongIndex();
    }

    public boolean consumeCloseInventoryRequest() {
        boolean request = closeInventoryRequestReady;
        closeInventoryRequestReady = false;
        return request;
    }

    /** Moves to the next available song in the requested direction. */
    public boolean moveSelection(int direction) {
        if (!isReady() || direction == 0) {
            return false;
        }
        int step = direction < 0 ? -1 : 1;
        int candidate = playerState.progression().selectedSongIndex();
        for (int attempts = 0; attempts < SONG_COUNT; attempts++) {
            candidate = Math.floorMod(candidate + step, SONG_COUNT);
            if (isSongAvailable(candidate)) {
                playerState.progression().setSelectedSongIndex(candidate);
                soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
                return true;
            }
        }
        return false;
    }

    public boolean isSongAvailable(int songIndex) {
        if (songIndex < 0 || songIndex >= SONG_COUNT) {
            return false;
        }
        return OcarinaSong.fromSelectionIndex(songIndex)
                .isSet(playerState.progression().ocarinaSongFlags());
    }

    /** @deprecated Use {@link OcarinaSong#fromSelectionIndex(int)}. */
    @Deprecated(forRemoval = true)
    public static int songFlagForIndex(int songIndex) {
        if (songIndex < 0 || songIndex >= SONG_COUNT) {
            throw new IllegalArgumentException("Song index out of range: " + songIndex);
        }
        return OcarinaSong.fromSelectionIndex(songIndex).romValue();
    }
}
