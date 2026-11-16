package linksawakening.world.transition;

public record RoomBoundaryState(RoomBoundaryInputs.Room room, RoomBoundaryInputs.Link link)
        implements RoomBoundaryView {
    public RoomBoundaryState(RoomBoundaryInputs inputs) {
        this(inputs.room(), inputs.link());
    }

    public RoomBoundaryState(
            int mapCategory,
            int roomId,
            boolean indoorHasSouthEntrance,
            boolean hasWarps,
            int shutterDoorMask,
            int linkX,
            int linkY) {
        this(new linksawakening.world.transition.RoomBoundaryInputs(
                new linksawakening.world.transition.RoomBoundaryInputs.Room(
                        mapCategory, roomId, indoorHasSouthEntrance, hasWarps, shutterDoorMask, -1),
                new linksawakening.world.transition.RoomBoundaryInputs.Link(linkX, linkY, false, 0, false)));
    }

    public RoomBoundaryState(
            int mapCategory, int roomId, boolean indoorHasSouthEntrance, boolean hasWarps, int linkX, int linkY) {
        this(new linksawakening.world.transition.RoomBoundaryInputs(
                new linksawakening.world.transition.RoomBoundaryInputs.Room(
                        mapCategory, roomId, indoorHasSouthEntrance, hasWarps, 0, -1),
                new linksawakening.world.transition.RoomBoundaryInputs.Link(linkX, linkY, false, 0, false)));
    }

    public RoomBoundaryState(
            int mapCategory,
            int roomId,
            boolean indoorHasSouthEntrance,
            boolean hasWarps,
            int shutterDoorMask,
            int linkX,
            int linkY,
            int mapId) {
        this(new linksawakening.world.transition.RoomBoundaryInputs(
                new linksawakening.world.transition.RoomBoundaryInputs.Room(
                        mapCategory, roomId, indoorHasSouthEntrance, hasWarps, shutterDoorMask, mapId),
                new linksawakening.world.transition.RoomBoundaryInputs.Link(linkX, linkY, false, 0, false)));
    }

    @Override
    public int mapCategory() {
        return room.mapCategory();
    }

    @Override
    public int roomId() {
        return room.roomId();
    }

    @Override
    public boolean indoorHasSouthEntrance() {
        return room.indoorHasSouthEntrance();
    }

    @Override
    public boolean hasWarps() {
        return room.hasWarps();
    }

    @Override
    public int shutterDoorMask() {
        return room.shutterDoorMask();
    }

    @Override
    public int linkX() {
        return link.x();
    }

    @Override
    public int linkY() {
        return link.y();
    }

    @Override
    public int mapId() {
        return room.mapId();
    }

    @Override
    public boolean linkAirborne() {
        return link.airborne();
    }

    @Override
    public int linkPhysicsModifier() {
        return link.physicsModifier();
    }

    @Override
    public boolean sideViewEntityActive() {
        return link.sideViewEntityActive();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + Integer.hashCode(room.mapCategory());
        hash = 31 * hash + Integer.hashCode(room.roomId());
        hash = 31 * hash + Boolean.hashCode(room.indoorHasSouthEntrance());
        hash = 31 * hash + Boolean.hashCode(room.hasWarps());
        hash = 31 * hash + Integer.hashCode(room.shutterDoorMask());
        hash = 31 * hash + Integer.hashCode(link.x());
        hash = 31 * hash + Integer.hashCode(link.y());
        hash = 31 * hash + Integer.hashCode(room.mapId());
        hash = 31 * hash + Boolean.hashCode(link.airborne());
        hash = 31 * hash + Integer.hashCode(link.physicsModifier());
        hash = 31 * hash + Boolean.hashCode(link.sideViewEntityActive());
        return hash;
    }

    @Override
    public String toString() {
        return "RoomBoundaryState[" + "mapCategory=" + room.mapCategory() + ", " + "roomId=" + room.roomId() + ", "
                + "indoorHasSouthEntrance=" + room.indoorHasSouthEntrance() + ", " + "hasWarps=" + room.hasWarps()
                + ", " + "shutterDoorMask=" + room.shutterDoorMask() + ", " + "linkX=" + link.x() + ", " + "linkY="
                + link.y() + ", " + "mapId=" + room.mapId() + ", " + "linkAirborne=" + link.airborne() + ", "
                + "linkPhysicsModifier=" + link.physicsModifier() + ", " + "sideViewEntityActive="
                + link.sideViewEntityActive() + "]";
    }
}
