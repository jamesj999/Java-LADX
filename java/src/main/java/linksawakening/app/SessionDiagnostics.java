package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import java.util.stream.Collectors;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Live diagnostic views plus the captured frame number for replay tools. */
public abstract sealed class SessionDiagnostics permits HeadlessGameDriver.Diagnostics {
    private final GPU gpu;
    private final RoomSession roomSession;
    private final TransitionController transitionController;
    private final int frameCounter;
    private final ScrollController scrollController;
    private final PlayerState playerState;
    private final InventoryMenu inventoryMenu;
    private final DialogController dialogController;
    private final EquipmentController equipmentController;
    private final Link link;

    SessionDiagnostics(SessionContext context) {
        this.gpu = context.services().rom().gpu();
        this.roomSession = context.services().world().roomSession();
        this.transitionController = context.services().world().transitionController();
        this.frameCounter = context.state().clock().value();
        this.scrollController = context.services().world().scrollController();
        this.playerState = context.services().player().playerState();
        this.inventoryMenu = context.services().ui().inventoryMenu();
        this.dialogController = context.services().ui().dialogController();
        this.equipmentController = context.services().player().equipmentController();
        this.link = context.services().player().link();
    }

    public GPU gpu() {
        return gpu;
    }

    public RoomSession roomSession() {
        return roomSession;
    }

    public TransitionController transitionController() {
        return transitionController;
    }

    public int frameCounter() {
        return frameCounter;
    }

    public ScrollController scrollController() {
        return scrollController;
    }

    public PlayerState playerState() {
        return playerState;
    }

    public InventoryMenu inventoryMenu() {
        return inventoryMenu;
    }

    public DialogController dialogController() {
        return dialogController;
    }

    public EquipmentController equipmentController() {
        return equipmentController;
    }

    public Link link() {
        return link;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof SessionDiagnostics other
                && Objects.equals(gpu, other.gpu)
                && Objects.equals(roomSession, other.roomSession)
                && Objects.equals(transitionController, other.transitionController)
                && frameCounter == other.frameCounter
                && Objects.equals(scrollController, other.scrollController)
                && Objects.equals(playerState, other.playerState)
                && Objects.equals(inventoryMenu, other.inventoryMenu)
                && Objects.equals(dialogController, other.dialogController)
                && Objects.equals(equipmentController, other.equipmentController)
                && Objects.equals(link, other.link);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + Objects.hashCode(gpu);
        hash = 31 * hash + Objects.hashCode(roomSession);
        hash = 31 * hash + Objects.hashCode(transitionController);
        hash = 31 * hash + Integer.hashCode(frameCounter);
        hash = 31 * hash + Objects.hashCode(scrollController);
        hash = 31 * hash + Objects.hashCode(playerState);
        hash = 31 * hash + Objects.hashCode(inventoryMenu);
        hash = 31 * hash + Objects.hashCode(dialogController);
        hash = 31 * hash + Objects.hashCode(equipmentController);
        hash = 31 * hash + Objects.hashCode(link);
        return hash;
    }

    @Override
    public String toString() {
        return "Diagnostics[" + "gpu=" + gpu + ", roomSession=" + roomSession + ", transitionController="
                + transitionController + ", frameCounter=" + frameCounter + ", scrollController=" + scrollController
                + ", playerState=" + playerState + ", inventoryMenu=" + inventoryMenu + ", dialogController="
                + dialogController + ", equipmentController=" + equipmentController + ", link=" + link + "]";
    }

    public boolean headlessHasInventoryItem(int item) {
        if (playerState.itemA() == item || playerState.itemB() == item) return true;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            if (playerState.subscreenItem(slot) == item) return true;
        }
        return false;
    }

    public String headlessInstrumentTrace() {
        return roomSession == null ? "" : roomSession.rooms().instrumentTrace();
    }

    public String headlessMoblinKingTrace() {
        return roomSession == null ? "-1" : roomSession.rooms().moblinKingTrace() + "," + playerState.activePowerUp();
    }

    public int headlessBowWowState() {
        return roomSession == null ? 0 : roomSession.story().bowWowState();
    }

    public String headlessCombatTrace() {
        Sword sword = equipmentController.activeSword();
        Sword.CollisionBox box = sword == null
                ? Sword.CollisionBox.inactive()
                : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(), link.direction());
        return "%d,%d,%d,%d,%d;%s"
                .formatted(
                        box.active() ? 1 : 0,
                        box.x(),
                        box.width(),
                        box.y(),
                        box.height(),
                        roomSession.rooms().activeRoom().entities().loadedEntities().stream()
                                .filter(e -> e.type() == 0x81 || e.type() == 0x82 || e.type() == 0x59)
                                .map(e -> "%d,%d,%d,%d,%d,%d"
                                        .formatted(
                                                e.type(),
                                                e.x(),
                                                e.y(),
                                                e.z(),
                                                roomSession.entityState().entityHealth(e.slot()),
                                                e.status() == EntityStatus.ACTIVE ? 5 : 1))
                                .collect(Collectors.joining(";")));
    }

    public String headlessEntityPositions() {
        return roomSession.rooms().activeRoom().entities().loadedEntities().stream()
                .map(e -> "%02X:%s@%d,%d".formatted(e.type(), e.status(), e.x(), e.y()))
                .collect(Collectors.joining(" "));
    }

    public HeadlessGameDriver.State headlessState() {
        return new HeadlessGameDriver.State(this);
    }
}
