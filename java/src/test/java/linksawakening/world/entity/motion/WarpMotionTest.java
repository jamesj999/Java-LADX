package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBuilder;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class WarpMotionTest {

    @Test
    void dungeonWarpUsesRomPairAndRequiresClearedMiniboss() throws Exception {
        WarpMotion motion = new WarpMotion(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntity entity = entity(0x48, 0x40);

        WarpMotion.Update gated = motion.advance(new WarpFrame(entity, 0, 0, 0x01, 0x28, false, 0x48, 0x40, 0));
        assertEquals(0, gated.state());
        assertEquals(-1, gated.musicTrack());

        WarpMotion.Update intro = motion.advance(
                new WarpFrame(entity, gated.state(), gated.countdown(), 0x01, 0x28, true, 0x00, 0x00, 0));
        assertEquals(1, intro.state());
        assertEquals(0x1B, intro.musicTrack());

        WarpMotion.Update close = motion.advance(
                new WarpFrame(entity, intro.state(), intro.countdown(), 0x01, 0x28, true, 0x48, 0x40, 0));
        assertEquals(1, close.state());

        WarpMotion.Update away = motion.advance(
                new WarpFrame(entity, close.state(), close.countdown(), 0x01, 0x28, true, 0x20, 0x20, 0));
        assertEquals(2, away.state());

        WarpMotion.Update airborne =
                motion.advance(new WarpFrame(entity, away.state(), away.countdown(), 0x01, 0x28, true, 0x48, 0x40, 1));
        assertEquals(2, airborne.state());

        WarpMotion.Update armed =
                motion.advance(new WarpFrame(entity, away.state(), away.countdown(), 0x01, 0x28, true, 0x48, 0x40, 0));
        assertEquals(3, armed.state());
        assertEquals(0x50, armed.countdown());
        assertEquals(0x20, armed.immunityCountdown());
        assertEquals(0x1C, armed.jingleId());
        assertTrue(armed.motionBlocked());

        WarpMotion.Update countdown = armed;
        for (int frame = 0; frame < 0x4F; frame++) {
            countdown = motion.advance(
                    new WarpFrame(entity, countdown.state(), countdown.countdown(), 0x01, 0x28, true, 0x48, 0x40, 0));
            assertTrue(countdown.motionBlocked());
            assertFalse(countdown.destinationReady());
        }
        WarpMotion.Update destination = motion.advance(
                new WarpFrame(entity, countdown.state(), countdown.countdown(), 0x01, 0x28, true, 0x48, 0x40, 0));
        assertTrue(destination.destinationReady());
        assertEquals(
                new linksawakening.world.entity.event.WarpDestination(0x01, 0x36, 0x50, 0x48),
                destination.destination());
    }

    @Test
    void dungeonWarpSelectsTheOtherRomPairEntryForAnotherDungeon() throws Exception {
        WarpMotion motion = new WarpMotion(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        RoomEntity entity = entity(0x48, 0x40);
        WarpMotion.Update armed = motion.advance(new WarpFrame(entity, 2, 0, 0x00, 0x17, true, 0x48, 0x40, 0));
        assertEquals(3, armed.state());
        WarpMotion.Update destination =
                motion.advance(new WarpFrame(entity, armed.state(), 1, 0x00, 0x17, true, 0x48, 0x40, 0));
        assertEquals(
                new linksawakening.world.entity.event.WarpDestination(0x00, 0x11, 0x50, 0x48),
                destination.destination());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(WarpMotion.ENTITY_TYPE)
                .at(x, y)
                .spriteDefinition(EntitySpriteDefinition.unsupported(WarpMotion.ENTITY_TYPE))
                .build();
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
