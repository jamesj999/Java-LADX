package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import linksawakening.app.AllocationSessions;
import linksawakening.entity.Link;
import linksawakening.gpu.Tile;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class LinkBodyUploadTest {
    @Test
    void hudAndBackgroundTransfersHoldBothBodyColumnsWhileTheirPositionIsLive() throws Exception {
        try (var session = AllocationSessions.active()) {
            var world = new PhaseFixture(session);
            var phase = world.tiles();
            var link = world.link();
            var clock = session.state().clock();
            world.gpu().loadAnimatedTilesGroup(world.romData(), 0);
            clock.seed(1);
            phase.tick();
            var walking = body(link);
            int tick = world.gpu().animatedTilesFrameCount();

            link.animation().showHeldItemPose();
            link.position().setPixelPosition(36, 40);
            clock.seed(4);
            phase.tick();
            assertEquals(tick, world.gpu().animatedTilesFrameCount());
            assertEquals(walking, body(link));
            var coordinates = new ArrayList<Integer>();
            link.animation().submitOam((tile, x, y, fx, fy, palette, behind) -> coordinates.add(x), 0, 0);
            assertEquals(List.of(36, 36, 44, 44), coordinates.subList(0, 4));

            world.gpu().loadAnimatedTilesGroup(world.romData(), 2);
            while ((world.gpu().animatedTilesFrameCount() & 15) != 15) {
                world.gpu().tickAnimatedTiles(world.romData());
            }
            clock.seed(5);
            phase.tick();
            assertEquals(0, world.gpu().animatedTilesFrameCount() & 15);
            assertEquals(walking, body(link));
            clock.seed(6);
            phase.tick();
            assertNotEquals(walking, body(link));
        }
    }

    private static List<BodyTile> body(Link link) {
        var body = new ArrayList<BodyTile>();
        link.animation().submitOam((tile, x, y, fx, fy, palette, behind) -> body.add(new BodyTile(tile, fx, fy)), 0, 0);
        return List.copyOf(body.subList(0, 4));
    }

    private record BodyTile(Tile tile, boolean flipX, boolean flipY) {}
}
