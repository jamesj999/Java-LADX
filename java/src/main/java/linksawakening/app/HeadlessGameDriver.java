package linksawakening.app;

import linksawakening.app.flow.SessionContext;
import linksawakening.app.headless.HeadlessState;
import linksawakening.app.headless.ReplayCheckpoint;
import linksawakening.app.headless.SessionDiagnosticSnapshot;
import linksawakening.app.headless.SessionDiagnostics;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.gpu.GPU;
import linksawakening.recording.RecordingOptions;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Owns an isolated, window-free game session for input replay and capture. Frames use the same input,
 * simulation and composition paths as the desktop. One instance is retained for the owning session.
 */
public final class HeadlessGameDriver implements AutoCloseable {
    private final GameSession session = new GameSession();

    public HeadlessGameDriver() {}

    public HeadlessGameDriver(RecordingOptions recording) {
        session.configureRecording(recording);
    }

    @Override
    public void close() {
        session.close();
    }

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

    /** Composed immutable checkpoint under the original public name. */
    public static final class State implements HeadlessState {
        private final ReplayCheckpoint checkpoint;

        public State(SessionDiagnostics snapshot) {
            checkpoint = new ReplayCheckpoint(snapshot);
        }

        @Override
        public int frame() {
            return checkpoint.frame();
        }

        @Override
        public int map() {
            return checkpoint.map();
        }

        @Override
        public int room() {
            return checkpoint.room();
        }

        @Override
        public int x() {
            return checkpoint.x();
        }

        @Override
        public int y() {
            return checkpoint.y();
        }

        @Override
        public boolean dialog() {
            return checkpoint.dialog();
        }

        @Override
        public boolean pageWait() {
            return checkpoint.pageWait();
        }

        @Override
        public String text() {
            return checkpoint.text();
        }

        @Override
        public int shield() {
            return checkpoint.shield();
        }

        @Override
        public int health() {
            return checkpoint.health();
        }

        @Override
        public int sword() {
            return checkpoint.sword();
        }

        @Override
        public int urchins() {
            return checkpoint.urchins();
        }

        @Override
        public boolean toadstool() {
            return checkpoint.toadstool();
        }

        @Override
        public int powder() {
            return checkpoint.powder();
        }

        @Override
        public int inventoryTick() {
            return checkpoint.inventoryTick();
        }

        @Override
        public int tarinX() {
            return checkpoint.tarinX();
        }

        @Override
        public int tarinY() {
            return checkpoint.tarinY();
        }

        @Override
        public boolean forestCleared() {
            return checkpoint.forestCleared();
        }

        @Override
        public int tailKey() {
            return checkpoint.tailKey();
        }

        @Override
        public int bgTick() {
            return checkpoint.bgTick();
        }

        @Override
        public int smallKeys() {
            return checkpoint.smallKeys();
        }

        @Override
        public int compass() {
            return checkpoint.compass();
        }

        @Override
        public int dungeonMap() {
            return checkpoint.dungeonMap();
        }

        @Override
        public int nightmareKey() {
            return checkpoint.nightmareKey();
        }

        @Override
        public int animation() {
            return checkpoint.animation();
        }

        @Override
        public int direction() {
            return checkpoint.direction();
        }

        @Override
        public int swordAnimation() {
            return checkpoint.swordAnimation();
        }

        @Override
        public int z() {
            return checkpoint.z();
        }

        @Override
        public int velocityZ() {
            return checkpoint.velocityZ();
        }

        @Override
        public int speedX() {
            return checkpoint.speedX();
        }

        @Override
        public int speedY() {
            return checkpoint.speedY();
        }

        @Override
        public boolean loading() {
            return checkpoint.loading();
        }

        @Override
        public int maxHearts() {
            return checkpoint.maxHearts();
        }

        @Override
        public int instrument1Flags() {
            return checkpoint.instrument1Flags();
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof State other && checkpoint.equals(other.checkpoint);
        }

        @Override
        public int hashCode() {
            return checkpoint.hashCode();
        }

        @Override
        public String toString() {
            return checkpoint.toString();
        }
    }
    /** Composed diagnostic view under the original public name. */
    public static final class Diagnostics implements SessionDiagnostics {
        private final SessionDiagnosticSnapshot snapshot;

        public Diagnostics(SessionContext context) {
            snapshot = new SessionDiagnosticSnapshot(context);
        }

        @Override
        public GPU gpu() {
            return snapshot.gpu();
        }

        @Override
        public RoomSession roomSession() {
            return snapshot.roomSession();
        }

        @Override
        public TransitionController transitionController() {
            return snapshot.transitionController();
        }

        @Override
        public int frameCounter() {
            return snapshot.frameCounter();
        }

        @Override
        public ScrollController scrollController() {
            return snapshot.scrollController();
        }

        @Override
        public PlayerState playerState() {
            return snapshot.playerState();
        }

        @Override
        public InventoryMenu inventoryMenu() {
            return snapshot.inventoryMenu();
        }

        @Override
        public DialogController dialogController() {
            return snapshot.dialogController();
        }

        @Override
        public EquipmentController equipmentController() {
            return snapshot.equipmentController();
        }

        @Override
        public Link link() {
            return snapshot.link();
        }

        @Override
        public boolean equals(Object value) {
            return snapshot.equals(value);
        }

        @Override
        public int hashCode() {
            return snapshot.hashCode();
        }

        @Override
        public String toString() {
            return snapshot.toString();
        }

        @Override
        public boolean headlessHasInventoryItem(int item) {
            return snapshot.headlessHasInventoryItem(item);
        }

        @Override
        public String headlessInstrumentTrace() {
            return snapshot.headlessInstrumentTrace();
        }

        @Override
        public String headlessMoblinKingTrace() {
            return snapshot.headlessMoblinKingTrace();
        }

        @Override
        public int headlessBowWowState() {
            return snapshot.headlessBowWowState();
        }

        @Override
        public String headlessCombatTrace() {
            return snapshot.headlessCombatTrace();
        }

        @Override
        public String headlessEntityPositions() {
            return snapshot.headlessEntityPositions();
        }

        @Override
        public HeadlessGameDriver.State headlessState() {
            return snapshot.headlessState();
        }
    }
}
