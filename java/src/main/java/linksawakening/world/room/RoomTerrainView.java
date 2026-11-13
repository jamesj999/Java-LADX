package linksawakening.world.room;

final class RoomTerrainView implements RoomTerrain {
    private final RoomSession session;

    RoomTerrainView(RoomSession session) {
        this.session = java.util.Objects.requireNonNull(session);
    }

    public boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return session.tickCrackedFloor(frame, dialogGotItemActive);
    }

    public boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return session.tickCrackedFloorOnLanding(frame, dialogGotItemActive);
    }

    public boolean breakIndoorSwordCrystal(int location) {
        return session.breakIndoorSwordCrystal(location);
    }
}
