package linksawakening.world.room;

public interface RoomTerrain {
    boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive);

    boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive);

    boolean breakIndoorSwordCrystal(int location);
}
