package linksawakening.world.room;

public interface RoomTerrain {
    boolean tickCrackedFloor(LinkGroundInputs frame, boolean dialogGotItemActive);

    boolean tickCrackedFloorOnLanding(LinkGroundInputs frame, boolean dialogGotItemActive);

    boolean breakIndoorSwordCrystal(int location);
}
