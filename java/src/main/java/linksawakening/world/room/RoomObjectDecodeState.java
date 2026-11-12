package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.transition.Warp;

final class RoomObjectDecodeState {
    int[] roomObjectsArea;
    int shutterDoorMask;
    int staircaseLocation = -1;
    int floorObject;
    final List<Warp> warps = new ArrayList<>();
    final List<Integer> doorPositions = new ArrayList<>();
    int roomStatusFlags;
}
