package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BossIntroMotionTest {

    @Test
    void bossIntroWaitsForTheRomDelayThenStartsMusicAndDialogOnce() {
        BossIntroMotion motion = new BossIntroMotion();

        for (int frame = 0; frame < 0x20; frame++) {
            BossIntroMotion.Update delayed = motion.advance(0x00, 0x00, 0x00, 0x04);
            assertEquals(-1, delayed.musicTrack());
            assertEquals(-1, delayed.dialogLowId());
        }

        BossIntroMotion.Update started = motion.advance(0x00, 0x00, 0x00, 0x04);
        assertEquals(0x19, started.musicTrack());
        assertEquals(0, started.dialogTableId());
        assertEquals(0xB0, started.dialogLowId());

        BossIntroMotion.Update repeated = motion.advance(0x00, 0x00, 0x00, 0x04);
        assertEquals(-1, repeated.musicTrack());
        assertEquals(-1, repeated.dialogLowId());
    }

    @Test
    void bossIntroKeepsMusicButSuppressesDialogBeforeTheInteractiveTransition() {
        BossIntroMotion motion = new BossIntroMotion();
        for (int frame = 0; frame < 0x20; frame++) {
            motion.advance(0x00, 0x00, 0x00, 0x04);
        }

        BossIntroMotion.Update started = motion.advance(0x00, 0x00, 0x00, 0x03);
        assertEquals(0x19, started.musicTrack());
        assertEquals(-1, started.dialogLowId());
    }
}
