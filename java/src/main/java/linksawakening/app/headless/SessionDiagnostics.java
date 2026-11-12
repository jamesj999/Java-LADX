package linksawakening.app.headless;

import linksawakening.app.HeadlessGameDriver;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Public session diagnostic contract retained by HeadlessGameDriver. */
public interface SessionDiagnostics {
    GPU gpu();

    RoomSession roomSession();

    TransitionController transitionController();

    int frameCounter();

    ScrollController scrollController();

    PlayerState playerState();

    InventoryMenu inventoryMenu();

    DialogController dialogController();

    EquipmentController equipmentController();

    Link link();

    boolean headlessHasInventoryItem(int item);

    String headlessInstrumentTrace();

    String headlessMoblinKingTrace();

    int headlessBowWowState();

    String headlessCombatTrace();

    String headlessEntityPositions();

    HeadlessGameDriver.State headlessState();
}
