package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.room.RoomSession.LinkGroundFrame;

public interface RoomTerrain {
    boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive);

    boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive);

    boolean breakIndoorSwordCrystal(int location);
}
