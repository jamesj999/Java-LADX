package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.app.GameSession;
import linksawakening.app.SessionTestAccess;
import linksawakening.entity.Link;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class LinkMotionCaptureTest {
    @Test
    void finalPositionIsCapturedBeforeRealLinkMotion() {
        var game = new GameSession();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new PhaseFixture(game);
            var order = new ArrayList<String>();
            var driver = new LinkMotionDriver() {
                @Override
                public void capture(Link l) {
                    order.add("capture");
                    LinkMotionDriver.DIRECT.capture(l);
                }

                @Override
                public void update(Link l) {
                    assertEquals(List.of("capture"), order);
                    order.add("update");
                    LinkMotionDriver.DIRECT.update(l);
                }
            };
            c.linkEquipment(driver).tick(false, false);
            assertEquals(List.of("capture", "update"), order);
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }
}
