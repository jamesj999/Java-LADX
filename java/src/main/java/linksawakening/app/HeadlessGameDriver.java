package linksawakening.app;



/**
 * Owns an isolated, window-free game session for input replay and capture.
 * Frames use the same input, simulation and composition paths as the desktop.
 */

import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

import static linksawakening.world.RoomConstants.*;

public final class HeadlessGameDriver {
    private final GameSession session = new GameSession();

    public void initializeHeadlessNewGame() { session.initializeHeadlessNewGame(); }
    public void headlessButton(String button, boolean pressed) { session.headlessButton(button, pressed); }
    public byte[] headlessFrame() { return session.headlessFrame(); }
    public boolean headlessHasInventoryItem(int item) { return session.diagnostics().headlessHasInventoryItem(item); }
    public String headlessInstrumentTrace() { return session.diagnostics().headlessInstrumentTrace(); }
    public String headlessMoblinKingTrace() { return session.diagnostics().headlessMoblinKingTrace(); }
    public int headlessBowWowState() { return session.diagnostics().headlessBowWowState(); }
    public String headlessCombatTrace() { return session.diagnostics().headlessCombatTrace(); }
    public String headlessEntityPositions() { return session.diagnostics().headlessEntityPositions(); }
    public State headlessState() { return session.diagnostics().headlessState(); }

    public record State(int frame, int map, int room, int x, int y,
                         boolean dialog, boolean pageWait, String text, int shield, int health, int sword, int urchins, boolean toadstool, int powder, int inventoryTick, int tarinX, int tarinY, boolean forestCleared, int tailKey, int bgTick, int smallKeys, int compass, int dungeonMap, int nightmareKey, int animation, int direction, int swordAnimation, int z, int velocityZ, int speedX, int speedY, boolean loading, int maxHearts, int instrument1Flags) {}
    public record Diagnostics(
        GPU gpu,
        RoomSession roomSession,
        TransitionController transitionController,
        int frameCounter,
        ScrollController scrollController,
        PlayerState playerState,
        InventoryMenu inventoryMenu,
        DialogController dialogController,
        EquipmentController equipmentController,
        Link link) {
    public boolean headlessHasInventoryItem(int item) {
        if (playerState.itemA() == item || playerState.itemB() == item) return true;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            if (playerState.subscreenItem(slot) == item) return true;
        }
        return false;
    }
    public String headlessInstrumentTrace() {
        return roomSession == null ? "" : roomSession.instrumentTrace();
    }
    public String headlessMoblinKingTrace() {
        return roomSession == null ? "-1" : roomSession.moblinKingTrace() + "," + playerState.activePowerUp();
    }
    public int headlessBowWowState() { return roomSession == null ? 0 : roomSession.bowWowState(); }
    public String headlessCombatTrace() {
        Sword sword = equipmentController.activeSword();
        Sword.CollisionBox box = sword == null ? Sword.CollisionBox.inactive()
            : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(), link.direction());
        return "%d,%d,%d,%d,%d;%s".formatted(box.active() ? 1 : 0, box.x(), box.width(), box.y(), box.height(),
            roomSession.activeRoom().entities().loadedEntities().stream()
                .filter(e -> e.type() == 0x81 || e.type() == 0x82 || e.type() == 0x59)
                .map(e -> "%d,%d,%d,%d,%d,%d".formatted(e.type(), e.x(), e.y(), e.z(),
                    roomSession.entityHealth(e.slot()), e.status() == linksawakening.world.EntityStatus.ACTIVE ? 5 : 1))
                .collect(java.util.stream.Collectors.joining(";")));
    }
    public String headlessEntityPositions() {
        return roomSession.activeRoom().entities().loadedEntities().stream()
            .map(e -> "%02X:%s@%d,%d".formatted(e.type(), e.status(), e.x(), e.y()))
            .collect(java.util.stream.Collectors.joining(" "));
    }
    public HeadlessGameDriver.State headlessState() {
        var tarin = roomSession.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3F).findFirst().orElse(null);
        return new HeadlessGameDriver.State(frameCounter, roomSession.mapId(), roomSession.currentRoomId(),
            link.romEntityX(), link.romEntityY(), dialogController.isActive(),
            dialogController.isWaitingForPageAdvance(), dialogController.visibleText(),
            playerState.shieldLevel(), playerState.health(), playerState.swordLevel(), (int) roomSession.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xC5 && entity.status() == linksawakening.world.EntityStatus.ACTIVE).count(), playerState.hasToadstool(), playerState.magicPowderCount(), inventoryMenu.instrumentPaletteTick(), tarin == null ? -1 : tarin.x(), tarin == null ? -1 : tarin.y(),
                (roomSession.overworldRoomStatusSnapshot()[0x51] & 0x10) != 0,
                playerState.tailKeyCount(), gpu.animatedTilesFrameCount(),
                roomSession.currentDungeonItemFlagsSnapshot()[4] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[1] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[0] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[3] & 0xFF, link.romAnimationState(), 3 - link.direction(), equipmentController.activeSword() == null ? 0 : equipmentController.activeSword().state(), link.romEntityZ(), link.zVelocity(), link.romSpeedX(), link.romSpeedY(), scrollController.isActive() || transitionController.isInputBlocked(), playerState.maxHearts(), roomSession.dungeonProgressFlagsSnapshot()[0] & 0xFF);
    }
    }
}
