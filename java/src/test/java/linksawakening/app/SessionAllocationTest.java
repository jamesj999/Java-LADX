package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

/** Update-only budgets include audio/world work, but exclude framebuffer composition. */
@RequiresRom
final class SessionAllocationTest {
    @Test
    void simulationAllocation() throws Exception {
        var session = newGame();
        try {
            long bytesPerFrame = measure(session);
            assertTrue(session.diagnostics().dialogController().isActive(), "Opening dialog sample");
            System.out.println("Idle simulation allocation bytes/frame: " + bytesPerFrame);
            // Original same-scenario baseline: 5,304 bytes/frame, before retaining collaborators.
            assertTrue(bytesPerFrame <= 8_192, "Idle simulation bytes/frame: " + bytesPerFrame);
        } finally {
            session.cleanup();
        }
    }

    @Test
    void activeGameplayAllocation() throws Exception {
        var session = newGame();
        try {
            for (int i = 0; i < 520; i++) session.stepWithoutRender();
            for (int i = 0; i < 600 && session.diagnostics().dialogController().isActive(); i++) {
                session.headlessButton("a", (i & 1) == 0);
                session.stepWithoutRender();
            }
            session.headlessButton("a", false);
            session.headlessButton("right", true);
            for (int i = 0; i < 32; i++) session.stepWithoutRender();
            session.headlessButton("right", false);
            assertFalse(session.diagnostics().dialogController().isActive());
            assertEquals(70, session.diagnostics().link().romEntityX(), "Left the bed");
            long bytesPerFrame = measure(session);
            System.out.println("Active simulation allocation bytes/frame: " + bytesPerFrame);
            // Includes world snapshot/outbox/puzzle allocations and audio PCM. JFR sampled these
            // as major sources; this is a total-step budget, not an app-only attribution claim.
            assertTrue(bytesPerFrame <= 24_576, "Active simulation bytes/frame: " + bytesPerFrame);
        } finally {
            session.cleanup();
        }
    }

    private static GameSession newGame() throws Exception {
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        }
        var session = new GameSession();
        session.initializeHeadlessNewGame();
        return session;
    }

    private static long measure(GameSession session) {
        // Five 120-frame windows cover the same 600 frames a single window would.
        long bytesPerFrame = (long) AllocationProbe.bytesPerIteration(600, 120, session::stepWithoutRender);
        assertTrue(bytesPerFrame >= 0);
        return bytesPerFrame;
    }
}
