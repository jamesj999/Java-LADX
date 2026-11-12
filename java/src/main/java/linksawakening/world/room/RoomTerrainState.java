package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import linksawakening.world.entity.event.HookshotBridgeUpdate;

final class RoomTerrainState {
    int crackedFloorCounter;
    int linkGroundRoomPosition;
    int linkGroundFinalRoomPosition;
    int linkGroundObjectUnderEntity;
    int linkGroundObjectPhysics;
    final Map<Integer, HookshotBridgeUpdate> hookshotBridgeTileOverrides = new HashMap<>();
    /** Immediate bomb-wall draw commands use a different tile order than the room object table. */
    final Map<Integer, Integer> bombedWallTileOverrides = new HashMap<>();
    /** The basic A9 breakable path also draws floor tiles in source command order. */
    final Set<Integer> bombedBlockTileOverrides = new HashSet<>();
    /** Bombed overworld cave doors use the dedicated GBC redraw tile order. */
    final Set<Integer> bombedCaveDoorTileOverrides = new HashSet<>();

    final Map<Integer, int[]> retainedTorchTiles = new HashMap<>();
    /** WRAM wSwitchBlocksState; the source reset value is zero. */
    int switchBlocksState;
    /** WRAM wSwitchableObjectAnimationStage; zero means no update is active. */
    int switchableObjectAnimationStage;
}
