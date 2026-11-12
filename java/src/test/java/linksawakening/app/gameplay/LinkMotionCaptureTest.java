package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.app.*;
import linksawakening.app.GameSession;
import linksawakening.entity.Link;
import linksawakening.testing.RequiresRom;
import linksawakening.world.entity.event.*;
import org.junit.jupiter.api.Test;

@RequiresRom
class LinkMotionCaptureTest {
    @Test
    void finalPositionIsCapturedBeforeRealLinkMotion() {
        var game = new GameSession();
        try (var rom = linksawakening.testing.TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = SessionTestAccess.context(game).runtime().flows().simulation();
            var order = new ArrayList<String>();
            var driver = new LinkMotionDriver() {
                public void capture(Link l) {
                    order.add("capture");
                    LinkMotionDriver.DIRECT.capture(l);
                }

                public void update(Link l) {
                    assertEquals(List.of("capture"), order);
                    order.add("update");
                    LinkMotionDriver.DIRECT.update(l);
                }
            };
            new LinkEquipmentPhase(driver).tick(c, false, false);
            assertEquals(List.of("capture", "update"), order);
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }
}
