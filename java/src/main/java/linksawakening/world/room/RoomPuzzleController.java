package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;

/** RoomPuzzleController operations for the live room session. */
final class RoomPuzzleController {
    private final Dependencies dependencies;
    private final RoomEventTriggers roomEventTriggers;
    private final RoomBlockMotion roomBlockMotion;
    private final RoomPuzzleReveals roomPuzzleReveals;
    private final RoomShutterController roomShutterController;

    RoomPuzzleController(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomEventTriggers = new RoomEventTriggers(new RoomEventTriggers.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.location(),
                dependencies.puzzles(),
                dependencies.romTables(),
                dependencies.roomPuzzleController(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomStoryState(),
                dependencies.transientVfxSystem()));
        roomBlockMotion = new RoomBlockMotion(new RoomBlockMotion.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.entityInputs(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.roomPuzzleController(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw()));
        roomPuzzleReveals = new RoomPuzzleReveals(new RoomPuzzleReveals.Dependencies(
                dependencies.entityFrame(),
                dependencies.gpu(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.romData(),
                dependencies.roomObjectMutations(),
                dependencies.roomTileRedraw(),
                dependencies.roomTransitionController()));
        roomShutterController = new RoomShutterController(new RoomShutterController.Dependencies(
                dependencies.entityFrame(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.romData(),
                dependencies.roomPuzzleController(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw()));
    }

    void harvestThrownRoomEventCollisions() {
        roomEventTriggers.harvestThrownRoomEventCollisions();
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        roomEventTriggers.tickRoomEvent(linkEntityX, linkEntityY);
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        roomBlockMotion.tickPushedBlockMotion(linkEntityX, linkEntityY);
    }

    boolean pushedBlocksTriggerResolved() {
        return roomEventTriggers.pushedBlocksTriggerResolved();
    }

    void tickIndoorKeyDoorAnimation() {
        roomBlockMotion.tickIndoorKeyDoorAnimation();
    }

    void replaceKeyDoorObjects(int direction, int location) {
        roomBlockMotion.replaceKeyDoorObjects(direction, location);
    }

    void replaceBossDoorObjects(int location) {
        roomBlockMotion.replaceBossDoorObjects(location);
    }

    boolean killAllEnemiesTriggerResolved() {
        return roomEventTriggers.killAllEnemiesTriggerResolved();
    }

    void tickSwitchButton(int linkEntityX, int linkEntityY) {
        roomPuzzleReveals.tickSwitchButton(linkEntityX, linkEntityY);
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        roomPuzzleReveals.tickRoomEventChestReveal(linkEntityX, linkEntityY);
    }

    void tickRoomEventStairReveal() {
        roomPuzzleReveals.tickRoomEventStairReveal();
    }

    boolean tickShutterClosing() {
        return roomShutterController.tickShutterClosing();
    }

    void openActiveRoomShutterDoors() {
        roomShutterController.openActiveRoomShutterDoors();
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        roomShutterController.openActiveRoomShutterDoors(persistDirectionalStatus);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            GPU gpu,
            OverworldCollision overworldCollision,
            Rom romData,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController,
            TransientVfxSystem transientVfxSystem) {}
}
