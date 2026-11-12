package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.app.*;
import linksawakening.app.GameSession;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.equipment.Sword;
import linksawakening.testing.RequiresRom;
import linksawakening.world.entity.event.*;
import org.junit.jupiter.api.Test;

@RequiresRom
class EntityFrameSessionTest {
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
            var c = SessionTestAccess.context(game).runtime().flows().simulation();
            c.link().position().setPixelPosition(50, 60);
            c.link().position().captureRomFinalPosition();
            c.link().position().setPixelPosition(70, 80);
            var box = new Sword.CollisionBox(true, 1, 2, 3, 4);
            var inputs = new EntityFrameInputsFactory().create(c, box);
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
            var c = SessionTestAccess.context(game).runtime().flows().simulation();
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
            new EntityPhase(
                            new GameplayCombatEvents(
                                    c.roomSession(),
                                    c.link(),
                                    game.gameplaySounds(),
                                    c.transientVfxSystem(),
                                    c.musicDirector()),
                            routing)
                    .routeDialogs(c);
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
            var c = SessionTestAccess.context(game).runtime().flows().simulation();
            int hearts = c.playerState().vitals().maxHearts();
            linksawakening.world.room.RoomFrameTestAccess.heart(c.roomSession());
            var phase = new EntityPhase(
                    new GameplayCombatEvents(
                            c.roomSession(),
                            c.link(),
                            game.gameplaySounds(),
                            c.transientVfxSystem(),
                            c.musicDirector()),
                    EntityDialogRouting.DIRECT);
            phase.applyChestRewards(c);
            assertEquals(hearts + 1, c.playerState().vitals().maxHearts());
            assertEquals(0xFF, c.playerState().vitals().addHealthBuffer());
            phase.applyChestRewards(c);
            assertEquals(hearts + 1, c.playerState().vitals().maxHearts());
        } finally {
            SessionTestAccess.cleanup(game);
        }
    }

    @Test
    void cachedInputsRemainValueSnapshotsAcrossScriptedFrames() throws Exception {
        try (var game = AllocationSessions.active()) {
            var c = game.flows().simulation();
            var cached = new EntityFrameInputsFactory();
            var expected = new ArrayList<linksawakening.world.room.EntityFrameInputs>();
            var actual = new ArrayList<linksawakening.world.room.EntityFrameInputs>();
            var sword = Sword.CollisionBox.inactive();
            for (int frame = 0; frame < 240; frame++) {
                game.headlessButton("right", frame % 60 < 30);
                game.headlessButton("down", frame % 60 >= 30);
                SessionTestAccess.step(game);
                expected.add(new EntityFrameInputsFactory().create(c, sword));
                actual.add(cached.create(c, sword));
            }
            assertEquals(expected, actual, "Reused leaf values must never mutate earlier frames");
            assertTrue(actual.stream().map(f -> f.link().position()).distinct().count() > 1);
        }
    }
}
