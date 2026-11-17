package linksawakening.world.room;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import linksawakening.world.entity.event.HookshotBridgeUpdate;

/** Link ground registers and switch state persist across room changes. */
final class RoomTerrainState {
    void resetGameOverSwitchBlocks() {
        switchBlocksState = 0;
        switchableObjectAnimationStage = 0;
    }

    private int crackedFloorCounter;
    private int linkGroundRoomPosition;
    private int linkGroundFinalRoomPosition;
    private int linkGroundObjectUnderEntity;
    private int linkGroundObjectPhysics;
    private int switchBlocksState;
    private int switchableObjectAnimationStage;

    static final class Overrides {
        private final Map<Integer, HookshotBridgeUpdate> hookshotBridgeTileOverrides = new HashMap<>();
        private final Map<Integer, Integer> bombedWallTileOverrides = new HashMap<>();
        private final Set<Integer> bombedBlockTileOverrides = new HashSet<>();
        private final Set<Integer> bombedCaveDoorTileOverrides = new HashSet<>();
        private final Map<Integer, int[]> retainedTorchTiles = new HashMap<>();

        Map<Integer, HookshotBridgeUpdate> hookshotBridgeTileOverrides() {
            return this.hookshotBridgeTileOverrides;
        }

        Map<Integer, Integer> bombedWallTileOverrides() {
            return this.bombedWallTileOverrides;
        }

        Set<Integer> bombedBlockTileOverrides() {
            return this.bombedBlockTileOverrides;
        }

        Set<Integer> bombedCaveDoorTileOverrides() {
            return this.bombedCaveDoorTileOverrides;
        }

        Map<Integer, int[]> retainedTorchTiles() {
            return this.retainedTorchTiles;
        }
    }

    int switchBlocksState() {
        return this.switchBlocksState;
    }

    int switchBlocksState(int value) {
        this.switchBlocksState = value;
        return this.switchBlocksState;
    }

    int switchableObjectAnimationStage() {
        return this.switchableObjectAnimationStage;
    }

    int switchableObjectAnimationStage(int value) {
        this.switchableObjectAnimationStage = value;
        return this.switchableObjectAnimationStage;
    }

    int linkGroundFinalRoomPosition() {
        return this.linkGroundFinalRoomPosition;
    }

    int linkGroundFinalRoomPosition(int value) {
        this.linkGroundFinalRoomPosition = value;
        return this.linkGroundFinalRoomPosition;
    }

    int linkGroundRoomPosition() {
        return this.linkGroundRoomPosition;
    }

    int linkGroundRoomPosition(int value) {
        this.linkGroundRoomPosition = value;
        return this.linkGroundRoomPosition;
    }

    int linkGroundObjectPhysics() {
        return this.linkGroundObjectPhysics;
    }

    int linkGroundObjectPhysics(int value) {
        this.linkGroundObjectPhysics = value;
        return this.linkGroundObjectPhysics;
    }

    int linkGroundObjectUnderEntity() {
        return this.linkGroundObjectUnderEntity;
    }

    int linkGroundObjectUnderEntity(int value) {
        this.linkGroundObjectUnderEntity = value;
        return this.linkGroundObjectUnderEntity;
    }

    int crackedFloorCounter() {
        return this.crackedFloorCounter;
    }

    int crackedFloorCounter(int value) {
        this.crackedFloorCounter = value;
        return this.crackedFloorCounter;
    }
}
