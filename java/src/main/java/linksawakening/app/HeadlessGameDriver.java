package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

/**
 * Owns an isolated, window-free game session for input replay and capture.
 * Frames use the same input, simulation and composition paths as the desktop.
 */
public final class HeadlessGameDriver {
    private final GameSession session = new GameSession();

    public void initializeHeadlessNewGame() {
        session.initializeHeadlessNewGame();
    }

    public void headlessButton(String button, boolean pressed) {
        session.headlessButton(button, pressed);
    }

    public byte[] headlessFrame() {
        return session.headlessFrame();
    }

    public boolean headlessHasInventoryItem(int item) {
        return session.diagnostics().headlessHasInventoryItem(item);
    }

    public String headlessInstrumentTrace() {
        return session.diagnostics().headlessInstrumentTrace();
    }

    public String headlessMoblinKingTrace() {
        return session.diagnostics().headlessMoblinKingTrace();
    }

    public int headlessBowWowState() {
        return session.diagnostics().headlessBowWowState();
    }

    public String headlessCombatTrace() {
        return session.diagnostics().headlessCombatTrace();
    }

    public String headlessEntityPositions() {
        return session.diagnostics().headlessEntityPositions();
    }

    public State headlessState() {
        return session.diagnostics().headlessState();
    }

    /** Immutable checkpoint, retained under the driver's original public name. */
    public static final class State extends HeadlessState {
        State(SessionDiagnostics snapshot) {
            super(snapshot);
        }
    }
    /** Diagnostic view, retained under the driver's original public name. */
    public static final class Diagnostics extends SessionDiagnostics {
        public Diagnostics(SessionContext context) {
            super(context);
        }
    }
}
