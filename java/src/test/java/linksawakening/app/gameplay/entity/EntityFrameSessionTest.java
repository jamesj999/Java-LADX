package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.app.AllocationSessions;
import linksawakening.app.GameSession;
import linksawakening.app.SessionTestAccess;
import linksawakening.app.gameplay.PhaseFixture;
import linksawakening.equipment.Sword;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.world.room.EntityFrameInputs;
import linksawakening.world.room.RoomFrameTestAccess;
import linksawakening.world.room.SwordBox;
import org.junit.jupiter.api.Test;

@RequiresRom
class EntityFrameSessionTest {
    @Test
    void frameInputsCaptureTheCurrentUiSwordAndFinalPosition() {
        var game = new GameSession();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new PhaseFixture(game);
            c.link().position().setPixelPosition(50, 60);
            c.link().position().captureRomFinalPosition();
            c.link().position().setPixelPosition(70, 80);
            var box = new Sword.CollisionBox(true, 1, 2, 3, 4);
            var inputs = c.entityInputs().create(box);
            assertEquals(58, inputs.link().finalX());
            assertEquals(78, inputs.link().position().x());
            assertEquals(c.dialogController().isActive(), inputs.ui().dialogActive());
            assertEquals(c.musicPlayer().isMusicPlaying(), inputs.ui().musicActive());
            assertEquals(new SwordBox(true, 1, 2, 3, 4), inputs.sword());
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void pendingMusicPlaysBeforeDialogsAndInstrumentTransition() {
        var game = new GameSession();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new PhaseFixture(game);
            RoomFrameTestAccess.music(c.roomSession(), 0x18);
            var calls = new ArrayList<String>();
            var routing = new EntityDialogRouting() {
                @Override
                public void music(int track) {
                    assertEquals(0x18, track);
                    calls.add("music");
                }

                @Override
                public void dialogs() {
                    calls.add("dialogs");
                }

                @Override
                public void manbo() {
                    calls.add("manbo");
                }

                @Override
                public void instrument() {
                    calls.add("instrument");
                }
            };
            c.entityPhase(
                            new GameplayCombatEvents(
                                    c.roomSession(),
                                    c.link(),
                                    game.gameplaySounds(),
                                    c.transientVfxSystem(),
                                    c.musicDirector()),
                            routing)
                    .routeDialogs();
            assertEquals(List.of("music", "dialogs", "manbo", "instrument"), calls);
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void heartContainerRewardAppliesPlayerStateAndIsDrainedOnce() {
        var game = new GameSession();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        game.initializeHeadlessNewGame();
        try {
            var c = new PhaseFixture(game);
            int hearts = c.playerState().vitals().maxHearts();
            RoomFrameTestAccess.heart(c.roomSession());
            var phase = c.entityPhase(
                    new GameplayCombatEvents(
                            c.roomSession(),
                            c.link(),
                            game.gameplaySounds(),
                            c.transientVfxSystem(),
                            c.musicDirector()),
                    c.routing());
            phase.applyChestRewards();
            assertEquals(hearts + 1, c.playerState().vitals().maxHearts());
            assertEquals(0xFF, c.playerState().vitals().addHealthBuffer());
            phase.applyChestRewards();
            assertEquals(hearts + 1, c.playerState().vitals().maxHearts());
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void cachedInputsRemainValueSnapshotsAcrossScriptedFrames() throws Exception {
        try (var game = AllocationSessions.active()) {
            var c = new PhaseFixture(game);
            var cached = c.entityInputs();
            var expected = new ArrayList<EntityFrameInputs>();
            var actual = new ArrayList<EntityFrameInputs>();
            var sword = Sword.CollisionBox.inactive();
            for (int frame = 0; frame < 240; frame++) {
                game.headlessButton("right", frame % 60 < 30);
                game.headlessButton("down", frame % 60 >= 30);
                SessionTestAccess.step(game);
                expected.add(c.entityInputs().create(sword));
                actual.add(cached.create(sword));
            }
            assertEquals(expected, actual, "Reused leaf values must never mutate earlier frames");
            assertTrue(actual.stream().map(f -> f.link().position()).distinct().count() > 1);
        }
    }

    @Test
    void borrowedCaptureReusesRegistersWithoutMutatingOwnedHistory() throws Exception {
        try (var game = AllocationSessions.active()) {
            var c = new PhaseFixture(game);
            var factory = c.entityInputs();
            var sword = Sword.CollisionBox.inactive();
            var owned = factory.create(sword);
            var borrowed = factory.borrow(sword);
            int originalX = owned.link().position().x();
            int originalFrame = owned.frameCounter();
            c.link()
                    .position()
                    .setPixelPosition(
                            c.link().position().pixelX() + 7,
                            c.link().position().pixelY());
            game.frameClock().advance();
            org.junit.jupiter.api.Assertions.assertSame(borrowed, factory.borrow(sword));
            assertEquals(originalX + 7, borrowed.link().position().x());
            assertEquals(c.frameCounter(), borrowed.frameCounter());
            assertEquals(originalX, owned.link().position().x());
            assertEquals(originalFrame, owned.frameCounter());
        }
    }
}
