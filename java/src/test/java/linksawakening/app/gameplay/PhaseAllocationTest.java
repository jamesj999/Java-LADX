package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.app.AllocationSessions;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

/** Measures retained phases separately; entity world ticks are measured by SessionAllocationTest. */
@RequiresRom
class PhaseAllocationTest {
    private final TestReporter reporter;

    PhaseAllocationTest(TestReporter reporter) {
        this.reporter = reporter;
    }

    private static volatile Object observed;

    @Test
    void retainedGameplayPhasesReportPerStepAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = new PhaseFixture(session);
            var story = c.dialogStory();
            var link = c.linkEquipment();
            var combat = c.combat();
            var tiles = c.tiles();
            report("dialog/story", () -> story.tick());
            report("link/equipment", () -> link.tick(false, false));
            report("combat", () -> observed = combat.tick(false));
            report("animated tiles", () -> tiles.tick(false));
            report("music", () -> session.flows().music().tick());
            report("input", () -> session.flows().inputRouter().tryPresentFileSave());
            var screen = (GameplayScreen) session.state().screens().currentScreen;
            report("queued input", () -> {
                session.headlessButton("right", true);
                session.headlessButton("right", false);
                screen.clearPendingInput();
            });
        }
    }

    private void report(String name, Runnable action) {
        double bytes = AllocationProbe.bytesPerIteration(600, 120, action);
        reporter.publishEntry(name + " bytes/step: " + bytes);
        assertTrue(bytes >= 0);
    }
}
