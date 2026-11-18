package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.app.AllocationSessions;
import linksawakening.app.gameplay.PhaseFixture;
import linksawakening.equipment.Sword;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

/** Isolates the immutable entity frame inputs from the world entity tick. */
@RequiresRom
class EntityAllocationTest {
    private final TestReporter reporter;

    EntityAllocationTest(TestReporter reporter) {
        this.reporter = reporter;
    }

    private static volatile Object observed;

    @Test
    void frameInputsAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = new PhaseFixture(session);
            var sword = Sword.CollisionBox.inactive();
            var inputs = c.entityInputs();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> {
                session.frameClock().advance();
                observed = inputs.create(sword);
            });
            reporter.publishEntry("Entity inputs bytes/frame: " + bytes);
            assertTrue(bytes <= 50, "Entity frame input bytes: " + bytes);
        }
    }

    @Test
    void borrowedFrameInputsAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = new PhaseFixture(session);
            var sword = Sword.CollisionBox.inactive();
            var inputs = c.entityInputs();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> {
                session.frameClock().advance();
                observed = inputs.borrow(sword);
            });
            reporter.publishEntry("Borrowed entity inputs bytes/frame: " + bytes);
            assertTrue(bytes == 0, "Borrowed entity frame input bytes: " + bytes);
        }
    }

    @Test
    void resultApplicationWithoutWorldTickAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = new PhaseFixture(session);
            var phase = c.entityPhase(
                    c.roomSession(), c.link(), session.gameplaySounds(), c.transientVfxSystem(), c.musicDirector());
            var events = List.<EntityProjectileEvent>of();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> phase.applyResults(events));
            reporter.publishEntry("Entity results without world tick bytes/frame: " + bytes);
            assertTrue(bytes <= 350, "Entity result application bytes: " + bytes);
        }
    }
}
