package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OcarinaTest {

    @Test
    void startsTheRomBalladCountdownAndSound() {
        PlayerState player = new PlayerState();
        player.setOcarinaSongFlags(0x04);
        player.setSelectedSongIndex(0);
        RecordingTarget target = new RecordingTarget();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        Ocarina ocarina = new Ocarina(player, sounds::add, target);

        ocarina.onPress();

        assertEquals(0xDC, target.countdown);
        assertEquals(0x04, target.songFlags);
        assertEquals(0, target.selectedSong);
        assertEquals(List.of(GameplaySoundEvent.OCARINA_BALLAD), sounds);
        assertTrue(ocarina.blocksMotion());
        assertTrue(ocarina.locksFacing());
    }

    @Test
    void selectsTheRomMamboAndFrogDurations() {
        PlayerState player = new PlayerState();
        player.setOcarinaSongFlags(0x07);
        RecordingTarget target = new RecordingTarget();
        Ocarina ocarina = new Ocarina(player, event -> {}, target);

        player.setSelectedSongIndex(1);
        ocarina.onPress();
        assertEquals(0xD0, target.countdown);
        assertEquals(GameplaySoundEvent.OCARINA_MAMBO, target.sound);

        target.playing = false;
        player.setSelectedSongIndex(2);
        ocarina.onPress();
        assertEquals(0xBB, target.countdown);
        assertEquals(GameplaySoundEvent.OCARINA_FROG, target.sound);
    }

    @Test
    void playsTheRomNoSongToneAndDoesNotRestartWhileActive() {
        PlayerState player = new PlayerState();
        RecordingTarget target = new RecordingTarget();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        Ocarina ocarina = new Ocarina(player, sounds::add, target);

        ocarina.onPress();
        ocarina.onPress();

        assertEquals(0xD0, target.countdown);
        assertEquals(List.of(GameplaySoundEvent.OCARINA_NO_SONG), sounds);
        assertFalse(target.secondStartAttempt);
    }

    private static final class RecordingTarget implements Ocarina.PlaybackTarget {
        private int countdown;
        private int songFlags;
        private int selectedSong;
        private boolean playing;
        private boolean secondStartAttempt;
        private GameplaySoundEvent sound;

        @Override
        public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
            if (playing) {
                secondStartAttempt = true;
                return false;
            }
            this.countdown = countdown;
            this.songFlags = songFlags;
            this.selectedSong = selectedSong;
            this.playing = true;
            this.sound = switch (selectedSong) {
                case 1 -> GameplaySoundEvent.OCARINA_MAMBO;
                case 2 -> GameplaySoundEvent.OCARINA_FROG;
                default -> songFlags == 0
                    ? GameplaySoundEvent.OCARINA_NO_SONG
                    : GameplaySoundEvent.OCARINA_BALLAD;
            };
            return true;
        }

        @Override
        public boolean ocarinaPlaying() {
            return playing;
        }
    }
}
