package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

final class RoomTerrainView implements RoomTerrain {
    private final Dependencies dependencies;

    RoomTerrainView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return dependencies.roomObjectMutations().tickCrackedFloor(frame, dialogGotItemActive);
    }

    public boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return dependencies.roomObjectMutations().applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }

    public boolean breakIndoorSwordCrystal(int location) {
        return dependencies.roomObjectMutations().breakIndoorSwordCrystal(location);
    }

    record Dependencies(RoomObjectMutations roomObjectMutations) {}
}
