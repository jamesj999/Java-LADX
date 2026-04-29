package linksawakening.world;

import java.util.ArrayList;
import java.util.List;

public final class ActiveRoom {
    private final int roomId;
    private final int mapCategory;
    private final int mapId;
    private final int[] roomObjectsArea;
    private final int[] gbcOverlay;
    private final int[] renderValues;
    private final int[] tileIds;
    private final int[] tileAttrs;
    private final int[][] palettes;
    private final List<Warp> warps;
    private final boolean indoorHasSouthEntrance;

    private ActiveRoom(LoadedRoom room) {
        this.roomId = room.roomId();
        this.mapCategory = room.mapCategory();
        this.mapId = room.mapId();
        this.roomObjectsArea = room.roomObjectsArea();
        this.gbcOverlay = room.gbcOverlay();
        this.renderValues = room.renderValues();
        this.tileIds = room.tileIds();
        this.tileAttrs = room.tileAttrs();
        this.palettes = room.palettes();
        this.warps = new ArrayList<>(room.warps());
        this.indoorHasSouthEntrance = room.indoorHasSouthEntrance();
    }

    public static ActiveRoom from(LoadedRoom room) {
        return new ActiveRoom(room);
    }

    public int roomId() {
        return roomId;
    }

    public int mapCategory() {
        return mapCategory;
    }

    public int mapId() {
        return mapId;
    }

    public int[] roomObjectsArea() {
        return roomObjectsArea;
    }

    public int[] gbcOverlay() {
        return gbcOverlay;
    }

    public int[] renderValues() {
        return renderValues;
    }

    public int[] tileIds() {
        return tileIds;
    }

    public int[] tileAttrs() {
        return tileAttrs;
    }

    public int[][] palettes() {
        return palettes;
    }

    public List<Warp> warps() {
        return warps;
    }

    public boolean hasWarps() {
        return !warps.isEmpty();
    }

    public Warp firstWarp() {
        return warps.get(0);
    }

    public boolean indoorHasSouthEntrance() {
        return indoorHasSouthEntrance;
    }

    public void replaceFirstWarpTile(int tileLocation) {
        if (warps.isEmpty()) {
            return;
        }
        warps.set(0, warps.get(0).withTileLocation(tileLocation));
    }

    public RoomRenderSnapshot renderSnapshot() {
        return new RoomRenderSnapshot(tileIds, tileAttrs, palettes);
    }
}
