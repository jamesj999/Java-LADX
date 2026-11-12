package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;

/** Inputs for WarpMotion.advance. */
public record WarpFrame(
        RoomEntity entity,
        int state,
        int countdown,
        int mapId,
        int roomId,
        boolean minibossReady,
        int linkX,
        int linkY,
        int linkZ) {}
