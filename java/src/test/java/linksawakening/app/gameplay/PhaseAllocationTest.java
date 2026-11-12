package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.AllocationSessions;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

/** Measures retained phases separately; entity world ticks are measured by SessionAllocationTest. */
@RequiresRom
class PhaseAllocationTest {
    private static volatile Object observed;

    @Test
    void phaseAllocations() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = session.flows().simulation();
            var story = new DialogStoryPhase();
            var link = new LinkEquipmentPhase();
            var combat = new CombatPhase();
            var tiles = new AnimatedTilesPhase();
            report("dialog/story", () -> story.tick(c));
            report("link/equipment", () -> link.tick(c, false, false));
            report("combat", () -> observed = combat.tick(c, false));
            report("animated tiles", () -> tiles.tick(c, false));
            report("music", () -> session.flows().music().tick());
            report("input", () -> session.flows().inputRouter().tryPresentFileSave());
            var screen =
                    (linksawakening.app.screen.GameplayScreen) session.state().screens().currentScreen;
            report("queued input", () -> {
                session.headlessButton("right", true);
                session.headlessButton("right", false);
                screen.clearPendingInput();
            });
        }
    }

    private static void report(String name, Runnable action) {
        double bytes = AllocationProbe.bytesPerIteration(600, 120, action);
        System.out.println(name + " bytes/step: " + bytes);
        assertTrue(bytes >= 0);
    }
}
