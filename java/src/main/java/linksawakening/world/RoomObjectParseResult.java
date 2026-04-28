package linksawakening.world;

import java.util.List;

public record RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps) {

    public RoomObjectParseResult {
        roomObjectsArea = roomObjectsArea.clone();
        warps = List.copyOf(warps);
    }

    @Override
    public int[] roomObjectsArea() {
        return roomObjectsArea.clone();
    }

    public int[] mutableRoomObjectsAreaCopy() {
        return roomObjectsArea.clone();
    }

    public int objectAtLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return 0xFF;
        }
        return roomObjectsArea[areaIndex];
    }
}
