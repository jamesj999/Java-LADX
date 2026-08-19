package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WarpMotionTest {

    @Test
    void dungeonWarpUsesRomPairAndRequiresClearedMiniboss() throws Exception {
        WarpMotion motion = new WarpMotion(loadRom());
        RoomEntity entity = entity(0x48, 0x40);

        WarpMotion.Update gated = motion.advance(entity, 0, 0, 0x01, 0x28,
            false, 0x48, 0x40, 0);
        assertEquals(0, gated.state());
        assertEquals(-1, gated.musicTrack());

        WarpMotion.Update intro = motion.advance(entity, gated.state(), gated.countdown(),
            0x01, 0x28, true, 0x00, 0x00, 0);
        assertEquals(1, intro.state());
        assertEquals(0x1B, intro.musicTrack());

        WarpMotion.Update close = motion.advance(entity, intro.state(), intro.countdown(),
            0x01, 0x28, true, 0x48, 0x40, 0);
        assertEquals(1, close.state());

        WarpMotion.Update away = motion.advance(entity, close.state(), close.countdown(),
            0x01, 0x28, true, 0x20, 0x20, 0);
        assertEquals(2, away.state());

        WarpMotion.Update airborne = motion.advance(entity, away.state(), away.countdown(),
            0x01, 0x28, true, 0x48, 0x40, 1);
        assertEquals(2, airborne.state());

        WarpMotion.Update armed = motion.advance(entity, away.state(), away.countdown(),
            0x01, 0x28, true, 0x48, 0x40, 0);
        assertEquals(3, armed.state());
        assertEquals(0x50, armed.countdown());
        assertEquals(0x20, armed.immunityCountdown());
        assertEquals(0x1C, armed.jingleId());
        assertTrue(armed.motionBlocked());

        WarpMotion.Update countdown = armed;
        for (int frame = 0; frame < 0x4F; frame++) {
            countdown = motion.advance(entity, countdown.state(), countdown.countdown(),
                0x01, 0x28, true, 0x48, 0x40, 0);
            assertTrue(countdown.motionBlocked());
            assertFalse(countdown.destinationReady());
        }
        WarpMotion.Update destination = motion.advance(entity, countdown.state(),
            countdown.countdown(), 0x01, 0x28, true, 0x48, 0x40, 0);
        assertTrue(destination.destinationReady());
        assertEquals(new WarpMotion.Destination(0x01, 0x36, 0x50, 0x48),
            destination.destination());
    }

    @Test
    void dungeonWarpSelectsTheOtherRomPairEntryForAnotherDungeon() throws Exception {
        WarpMotion motion = new WarpMotion(loadRom());
        RoomEntity entity = entity(0x48, 0x40);
        WarpMotion.Update armed = motion.advance(entity, 2, 0, 0x00, 0x17,
            true, 0x48, 0x40, 0);
        assertEquals(3, armed.state());
        WarpMotion.Update destination = motion.advance(entity, armed.state(), 1,
            0x00, 0x17, true, 0x48, 0x40, 0);
        assertEquals(new WarpMotion.Destination(0x00, 0x11, 0x50, 0x48),
            destination.destination());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, WarpMotion.ENTITY_TYPE, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(WarpMotion.ENTITY_TYPE), 0, 0, 0, 0);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = WarpMotionTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
