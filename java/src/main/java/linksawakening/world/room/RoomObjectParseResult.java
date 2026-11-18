package linksawakening.world.room;

import java.util.List;
import java.util.Map;
import linksawakening.world.transition.Warp;

record RoomObjectParseResult(
        int[] roomObjectsArea,
        List<Warp> warps,
        int shutterDoorMask,
        int staircaseLocation,
        Map<Integer, Integer> shutterLocations) {

    public RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps) {
        this(roomObjectsArea, warps, 0, -1);
    }

    public RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps, int shutterDoorMask) {
        this(roomObjectsArea, warps, shutterDoorMask, -1);
    }

    public RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps, int shutterDoorMask, int staircaseLocation) {
        this(roomObjectsArea, warps, shutterDoorMask, staircaseLocation, Map.of());
    }

    public RoomObjectParseResult {
        roomObjectsArea = roomObjectsArea.clone();
        warps = List.copyOf(warps);
        shutterLocations = Map.copyOf(shutterLocations);
        shutterDoorMask &= 0x0F;
        if (staircaseLocation < -1 || staircaseLocation > 0xFF) {
            throw new IllegalArgumentException("Staircase location must be -1 or an unsigned byte");
        }
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
