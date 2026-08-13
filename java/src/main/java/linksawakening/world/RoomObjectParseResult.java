package linksawakening.world;

import java.util.List;

public record RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps,
                                    int shutterDoorMask, int staircaseLocation) {

    public RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps) {
        this(roomObjectsArea, warps, 0, -1);
    }

    public RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps,
                                 int shutterDoorMask) {
        this(roomObjectsArea, warps, shutterDoorMask, -1);
    }

    public RoomObjectParseResult {
        roomObjectsArea = roomObjectsArea.clone();
        warps = List.copyOf(warps);
        shutterDoorMask &= 0x0F;
        if (staircaseLocation < -1 || staircaseLocation > 0xFF) {
            throw new IllegalArgumentException(
                "Staircase location must be -1 or an unsigned byte");
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
