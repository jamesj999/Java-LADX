package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.TestRom;

/** Deterministic headless setup shared by per-stage allocation probes. */
public final class AllocationSessions {
    private AllocationSessions() {}

    public static GameSession active() throws Exception {
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        }
        var session = new GameSession();
        session.initializeHeadlessNewGame();
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
        assertEquals(70, session.diagnostics().link().position().romEntityX());
        return session;
    }
}
