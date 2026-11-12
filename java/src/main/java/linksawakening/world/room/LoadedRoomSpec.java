package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

/** Constructor inputs; LoadedRoom takes defensive copies at the same ownership boundary. */
public record LoadedRoomSpec(
        Identity identity, Objects objects, Graphics graphics, Exits exits, RoomEntitySnapshot entities) {
    public record Identity(int roomId, int mapCategory, int mapId, int animatedTilesGroup) {}

    public record Objects(int[] roomObjectsArea, int[] gbcOverlay, int[] renderValues) {}

    public record Graphics(int[] tileIds, int[] tileAttrs, int[][] palettes) {}

    public record Exits(List<Warp> warps, boolean indoorHasSouthEntrance, int shutterDoorMask, int staircaseLocation) {
        public Exits(List<Warp> warps, boolean indoorHasSouthEntrance, int shutterDoorMask) {
            this(warps, indoorHasSouthEntrance, shutterDoorMask, -1);
        }

        public Exits(List<Warp> warps, boolean indoorHasSouthEntrance) {
            this(warps, indoorHasSouthEntrance, 0, -1);
        }
    }
}
