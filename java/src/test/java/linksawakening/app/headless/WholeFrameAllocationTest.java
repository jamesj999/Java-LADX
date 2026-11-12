package linksawakening.app.headless;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.AllocationSessions;
import linksawakening.app.GameSession;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

/** Full simulation, render assembly, scene building and composition into a retained capture buffer. */
@RequiresRom
class WholeFrameAllocationTest {
    @Test
    void idleOpeningDialog() throws Exception {
        try (var session = newGame()) {
            var diagnostic = session.diagnostics();
            measure("Idle", session, () -> {});
            assertTrue(diagnostic.dialogController().isActive());
        }
    }

    @Test
    void walkingWithEntities() throws Exception {
        try (var session = AllocationSessions.active()) {
            session.headlessButton("down", true);
            for (int i = 0; i < 32; i++) session.headlessFrame();
            session.headlessButton("down", false);
            var diagnostic = session.diagnostics();
            int[] bounds = {255, 0};
            int[] frame = {0};
            measure("Walking", session, () -> {
                if ((frame[0] & 31) == 0) {
                    boolean right = (frame[0] & 32) == 0;
                    session.headlessButton("left", !right);
                    session.headlessButton("right", right);
                }
                frame[0]++;
                int x = diagnostic.link().position().romEntityX();
                bounds[0] = Math.min(bounds[0], x);
                bounds[1] = Math.max(bounds[1], x);
            });
            assertFalse(diagnostic.dialogController().isActive());
            assertTrue(bounds[1] - bounds[0] > 8, "Walking must move Link: " + bounds[0] + ".." + bounds[1]);
            assertFalse(diagnostic
                    .roomSession()
                    .rooms()
                    .activeRoom()
                    .entityView()
                    .loadedEntities()
                    .isEmpty());
        }
    }

    @Test
    void inventoryOpen() throws Exception {
        try (var session = AllocationSessions.active()) {
            var diagnostic = session.diagnostics();
            session.headlessButton("start", true);
            session.headlessFrame();
            session.headlessButton("start", false);
            for (int i = 0; i < 100 && !diagnostic.inventoryMenu().isFullyOpen(); i++) session.headlessFrame();
            assertTrue(diagnostic.inventoryMenu().isFullyOpen());
            measure("Inventory", session, () -> {});
            assertTrue(diagnostic.inventoryMenu().isFullyOpen());
        }
    }

    @Test
    void retainedOutputMatchesOwnedCapture() throws Exception {
        try (var owned = newGame();
                var retained = newGame()) {
            byte[] output = new byte[retained.services().visual().indexedDisplayBuffer().length];
            for (int frame = 0; frame < 12; frame++) {
                byte[] expected = owned.headlessFrame();
                assertSame(output, retained.headlessFrame(output));
                assertArrayEquals(expected, output);
                assertNotSame(expected, output);
            }
        }
    }

    private void measure(String label, GameSession session, Runnable input) {
        byte[] output = new byte[session.services().visual().indexedDisplayBuffer().length];
        double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> {
            input.run();
            assertSame(output, session.headlessFrame(output));
        });
        System.out.println(label + " whole-frame bytes: " + bytes);
        // EA-off achieved 1072 idle, about 1029.1 walking, 720 inventory; +25% headroom.
        int budget =
                switch (label) {
                    case "Idle" -> 1340;
                    case "Walking" -> 1287;
                    case "Inventory" -> 900;
                    default -> throw new AssertionError(label);
                };
        assertTrue(bytes <= budget, label + " whole-frame bytes: " + bytes);
    }

    private GameSession newGame() throws Exception {
        try (var input = TestRom.open()) {
            assertTrue(input.read() >= 0);
        }
        var session = new GameSession();
        session.initializeHeadlessNewGame();
        return session;
    }
}
