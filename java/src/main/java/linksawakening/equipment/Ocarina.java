package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;

import java.util.Objects;

/**
 * The edge-triggered Ocarina use path from {@code UseOcarina} in bank $02.
 * Song menu navigation is owned by the inventory layer; this item consumes
 * the selected song state and hands the playback countdown to the room.
 */
public final class Ocarina implements EquippedItem {

    public interface PlaybackTarget {
        boolean startOcarina(int countdown, int songFlags, int selectedSong);

        boolean ocarinaPlaying();

        /** Mirrors the pre-UseOcarina air/hookshot gate in the ROM. */
        default boolean canStartOcarina() {
            return true;
        }

        /** Returns Link's ROM body animation state, or {@code -1} when idle. */
        default int ocarinaAnimationState() {
            return -1;
        }
    }

    private final PlayerState playerState;
    private final GameplaySoundSink soundSink;
    private final PlaybackTarget target;

    public Ocarina(PlayerState playerState, GameplaySoundSink soundSink,
                   PlaybackTarget target) {
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public void onPress() {
        if (target.ocarinaPlaying() || !target.canStartOcarina()) {
            return;
        }

        int songFlags = playerState.ocarinaSongFlags() & 0x07;
        int selectedSong = playerState.selectedSongIndex() & 0xFF;
        Playback playback = Playback.forSelection(songFlags, selectedSong);
        if (target.startOcarina(playback.countdown(), songFlags, selectedSong)) {
            soundSink.play(playback.sound());
        }
    }

    @Override
    public boolean blocksMotion() {
        return target.ocarinaPlaying();
    }

    @Override
    public boolean locksFacing() {
        return target.ocarinaPlaying();
    }

    @Override
    public int overrideAnimationState(int direction, int walkFrame) {
        return target.ocarinaAnimationState();
    }

    private record Playback(int countdown, GameplaySoundEvent sound) {
        private static Playback forSelection(int songFlags, int selectedSong) {
            if (songFlags == 0) {
                return new Playback(0xD0, GameplaySoundEvent.OCARINA_NO_SONG);
            }
            return switch (selectedSong) {
                case 0 -> new Playback(0xDC, GameplaySoundEvent.OCARINA_BALLAD);
                case 1 -> new Playback(0xD0, GameplaySoundEvent.OCARINA_MAMBO);
                case 2 -> new Playback(0xBB, GameplaySoundEvent.OCARINA_FROG);
                default -> new Playback(0xD0, GameplaySoundEvent.OCARINA_NO_SONG);
            };
        }
    }
}
