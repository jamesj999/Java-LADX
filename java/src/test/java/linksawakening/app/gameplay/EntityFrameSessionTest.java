package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.app.*;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.testing.RequiresRom;
import linksawakening.world.entity.event.*;
import org.junit.jupiter.api.Test;

@RequiresRom
class EntityFrameSessionTest {
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
            var c = new GameplaySimulation(SessionTestAccess.context(game));
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

    @Test
    void frameInputsCaptureTheCurrentUiSwordAndFinalPosition() {
        var game = new GameSession();
        try (var rom = linksawakening.testing.TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new GameplaySimulation(SessionTestAccess.context(game));
            c.link().setPixelPosition(50, 60);
            c.link().captureRomFinalPosition();
            c.link().setPixelPosition(70, 80);
            var box = new Sword.CollisionBox(true, 1, 2, 3, 4);
            var inputs = EntityFrameInputsFactory.create(c, box);
            assertEquals(58, inputs.link().finalX());
            assertEquals(78, inputs.link().position().x());
            assertEquals(c.dialogController().isActive(), inputs.ui().dialogActive());
            assertEquals(c.musicPlayer().isMusicPlaying(), inputs.ui().musicActive());
            assertEquals(new linksawakening.world.room.SwordBox(true, 1, 2, 3, 4), inputs.sword());
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void pendingMusicPlaysBeforeDialogsAndInstrumentTransition() {
        var game = new GameSession();
        try (var rom = linksawakening.testing.TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new GameplaySimulation(SessionTestAccess.context(game));
            linksawakening.world.room.RoomFrameTestAccess.music(c.roomSession(), 0x18);
            var calls = new ArrayList<String>();
            var routing = new EntityDialogRouting() {
                public void music(GameplaySimulation c, int track) {
                    assertEquals(0x18, track);
                    calls.add("music");
                }

                public void dialogs(GameplaySimulation c) {
                    calls.add("dialogs");
                }

                public void manbo(GameplaySimulation c) {
                    calls.add("manbo");
                }

                public void instrument(GameplaySimulation c) {
                    calls.add("instrument");
                }
            };
            new EntityPhase(routing).routeDialogs(c);
            assertEquals(List.of("music", "dialogs", "manbo", "instrument"), calls);
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void heartContainerRewardAppliesPlayerStateAndIsDrainedOnce() {
        var game = new GameSession();
        try (var rom = linksawakening.testing.TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new GameplaySimulation(SessionTestAccess.context(game));
            int hearts = c.playerState().maxHearts();
            linksawakening.world.room.RoomFrameTestAccess.heart(c.roomSession());
            var phase = new EntityPhase();
            phase.applyChestRewards(c);
            assertEquals(hearts + 1, c.playerState().maxHearts());
            assertEquals(0xFF, c.playerState().vitals().addHealthBuffer());
            phase.applyChestRewards(c);
            assertEquals(hearts + 1, c.playerState().maxHearts());
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }
}
