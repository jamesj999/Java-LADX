package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.world.transition.RoomBoundaryInputs;
import linksawakening.world.transition.RoomBoundaryState;
import org.junit.jupiter.api.Test;

class RoomValueBoundaryTest {
    @Test
    void loadedRoomKeepsDefensiveCopiesAtConstructionAndOnEveryArrayView() {
        int[] objects = {7};
        int[] tiles = {9};
        int[][] palettes = {{11}};
        var room = room(objects, tiles, palettes, -1);
        objects[0] = 0;
        tiles[0] = 0;
        palettes[0][0] = 0;
        assertEquals(7, room.roomObjectsArea()[0]);
        assertEquals(9, room.tileIds()[0]);
        assertEquals(11, room.palettes()[0][0]);
        room.objects().roomObjectsArea()[0] = 2;
        room.graphics().tileIds()[0] = 2;
        room.graphics().palettes()[0][0] = 2;
        assertEquals(7, room.roomObjectsArea()[0]);
        assertEquals(9, room.tileIds()[0]);
        assertEquals(11, room.palettes()[0][0]);
        assertEquals(3, room.shutterDoorMask());
    }

    @Test
    void loadedRoomKeepsStaircaseBoundsChecks() {
        assertThrows(IllegalArgumentException.class, () -> room(new int[1], new int[1], null, -2));
        assertThrows(IllegalArgumentException.class, () -> room(new int[1], new int[1], null, 256));
        assertEquals(255, room(new int[1], new int[1], null, 255).staircaseLocation());
    }

    @Test
    void boundaryValueKeepsFlatRecordEqualityHashAndText() {
        var value = new RoomBoundaryState(new RoomBoundaryInputs(
                new RoomBoundaryInputs.Room(0, 146, false, false, 3, -1),
                new RoomBoundaryInputs.Link(40, 50, true, 2, false)));
        var reference = new PreviousBoundary(0, 146, false, false, 3, 40, 50, -1, true, 2, false);
        assertEquals(reference.hashCode(), value.hashCode());
        assertEquals(reference.toString().replace("PreviousBoundary", "RoomBoundaryState"), value.toString());
        assertEquals(value, new RoomBoundaryState(new RoomBoundaryInputs(value.room(), value.link())));
    }

    private LoadedRoom room(int[] objects, int[] tiles, int[][] palettes, int staircase) {
        return new LoadedRoom(new LoadedRoomSpec(
                new LoadedRoomSpec.Identity(0, 0, 0, 0),
                new LoadedRoomSpec.Objects(objects, null, null),
                new LoadedRoomSpec.Graphics(tiles, tiles, palettes),
                new LoadedRoomSpec.Exits(List.of(), false, 0x13, staircase),
                null));
    }

    private record PreviousBoundary(
            int mapCategory,
            int roomId,
            boolean indoorHasSouthEntrance,
            boolean hasWarps,
            int shutterDoorMask,
            int linkX,
            int linkY,
            int mapId,
            boolean linkAirborne,
            int linkPhysicsModifier,
            boolean sideViewEntityActive) {}
}
