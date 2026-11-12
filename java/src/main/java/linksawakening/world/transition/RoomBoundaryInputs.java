package linksawakening.world.transition;

public record RoomBoundaryInputs(Room room, Link link) {
    public record Room(
            int mapCategory,
            int roomId,
            boolean indoorHasSouthEntrance,
            boolean hasWarps,
            int shutterDoorMask,
            int mapId) {}

    public record Link(int x, int y, boolean airborne, int physicsModifier, boolean sideViewEntityActive) {}
}
