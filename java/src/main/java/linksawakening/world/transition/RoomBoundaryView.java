package linksawakening.world.transition;

/** Read-only boundary registers. Borrowed probes expire at the next probe; owned states remain immutable. */
public interface RoomBoundaryView {
    int mapCategory();

    int roomId();

    boolean indoorHasSouthEntrance();

    boolean hasWarps();

    int shutterDoorMask();

    int linkX();

    int linkY();

    int mapId();

    boolean linkAirborne();

    int linkPhysicsModifier();

    boolean sideViewEntityActive();
}
