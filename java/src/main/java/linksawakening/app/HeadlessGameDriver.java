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
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Owns an isolated, window-free game session for input replay and capture. Frames use the same input,
 * simulation and composition paths as the desktop. One instance is retained for the owning session.
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

    /** Composed immutable checkpoint under the original public name. */
    public static final class State implements HeadlessState {
        private final ReplayCheckpoint checkpoint;

        public State(SessionDiagnostics snapshot) {
            checkpoint = new ReplayCheckpoint(snapshot);
        }

        public int frame() {
            return checkpoint.frame();
        }

        public int map() {
            return checkpoint.map();
        }

        public int room() {
            return checkpoint.room();
        }

        public int x() {
            return checkpoint.x();
        }

        public int y() {
            return checkpoint.y();
        }

        public boolean dialog() {
            return checkpoint.dialog();
        }

        public boolean pageWait() {
            return checkpoint.pageWait();
        }

        public String text() {
            return checkpoint.text();
        }

        public int shield() {
            return checkpoint.shield();
        }

        public int health() {
            return checkpoint.health();
        }

        public int sword() {
            return checkpoint.sword();
        }

        public int urchins() {
            return checkpoint.urchins();
        }

        public boolean toadstool() {
            return checkpoint.toadstool();
        }

        public int powder() {
            return checkpoint.powder();
        }

        public int inventoryTick() {
            return checkpoint.inventoryTick();
        }

        public int tarinX() {
            return checkpoint.tarinX();
        }

        public int tarinY() {
            return checkpoint.tarinY();
        }

        public boolean forestCleared() {
            return checkpoint.forestCleared();
        }

        public int tailKey() {
            return checkpoint.tailKey();
        }

        public int bgTick() {
            return checkpoint.bgTick();
        }

        public int smallKeys() {
            return checkpoint.smallKeys();
        }

        public int compass() {
            return checkpoint.compass();
        }

        public int dungeonMap() {
            return checkpoint.dungeonMap();
        }

        public int nightmareKey() {
            return checkpoint.nightmareKey();
        }

        public int animation() {
            return checkpoint.animation();
        }

        public int direction() {
            return checkpoint.direction();
        }

        public int swordAnimation() {
            return checkpoint.swordAnimation();
        }

        public int z() {
            return checkpoint.z();
        }

        public int velocityZ() {
            return checkpoint.velocityZ();
        }

        public int speedX() {
            return checkpoint.speedX();
        }

        public int speedY() {
            return checkpoint.speedY();
        }

        public boolean loading() {
            return checkpoint.loading();
        }

        public int maxHearts() {
            return checkpoint.maxHearts();
        }

        public int instrument1Flags() {
            return checkpoint.instrument1Flags();
        }

        public boolean equals(Object value) {
            return checkpoint.equals(value);
        }

        public int hashCode() {
            return checkpoint.hashCode();
        }

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

        public GPU gpu() {
            return snapshot.gpu();
        }

        public RoomSession roomSession() {
            return snapshot.roomSession();
        }

        public TransitionController transitionController() {
            return snapshot.transitionController();
        }

        public int frameCounter() {
            return snapshot.frameCounter();
        }

        public ScrollController scrollController() {
            return snapshot.scrollController();
        }

        public PlayerState playerState() {
            return snapshot.playerState();
        }

        public InventoryMenu inventoryMenu() {
            return snapshot.inventoryMenu();
        }

        public DialogController dialogController() {
            return snapshot.dialogController();
        }

        public EquipmentController equipmentController() {
            return snapshot.equipmentController();
        }

        public Link link() {
            return snapshot.link();
        }

        public boolean equals(Object value) {
            return snapshot.equals(value);
        }

        public int hashCode() {
            return snapshot.hashCode();
        }

        public String toString() {
            return snapshot.toString();
        }

        public boolean headlessHasInventoryItem(int item) {
            return snapshot.headlessHasInventoryItem(item);
        }

        public String headlessInstrumentTrace() {
            return snapshot.headlessInstrumentTrace();
        }

        public String headlessMoblinKingTrace() {
            return snapshot.headlessMoblinKingTrace();
        }

        public int headlessBowWowState() {
            return snapshot.headlessBowWowState();
        }

        public String headlessCombatTrace() {
            return snapshot.headlessCombatTrace();
        }

        public String headlessEntityPositions() {
            return snapshot.headlessEntityPositions();
        }

        public HeadlessGameDriver.State headlessState() {
            return snapshot.headlessState();
        }
    }
}
