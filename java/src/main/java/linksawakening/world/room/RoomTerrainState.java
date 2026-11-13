package linksawakening.world.room;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import linksawakening.world.entity.event.HookshotBridgeUpdate;

/** Link ground registers and switch state persist across room changes. */
final class RoomTerrainState {
    int crackedFloorCounter;
    int linkGroundRoomPosition;
    int linkGroundFinalRoomPosition;
    int linkGroundObjectUnderEntity;
    int linkGroundObjectPhysics;
    int switchBlocksState;
    int switchableObjectAnimationStage;

    static final class Overrides {
        final Map<Integer, HookshotBridgeUpdate> hookshotBridgeTileOverrides = new HashMap<>();
        final Map<Integer, Integer> bombedWallTileOverrides = new HashMap<>();
        final Set<Integer> bombedBlockTileOverrides = new HashSet<>();
        final Set<Integer> bombedCaveDoorTileOverrides = new HashSet<>();
        final Map<Integer, int[]> retainedTorchTiles = new HashMap<>();
    }
}
