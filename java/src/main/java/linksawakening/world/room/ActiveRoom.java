package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

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
    private final int staircaseLocation;
    private int shutterDoorMask;
    private RoomEntitySnapshot entities;
    private RoomEntitySnapshot ownedEntities;
    // NPC direction/walk presentations repeat while gameplay is gated by dialogs.
    private final RoomEntitySnapshot[] recentPresentations = new RoomEntitySnapshot[8];
    private int nextPresentation;
    private final RoomRenderSnapshot.View renderView = new RoomRenderLiveView(() -> this, null);

    private ActiveRoom(LoadedRoom room) {
        this(room, room.entities());
    }

    private ActiveRoom(LoadedRoom room, RoomEntitySnapshot entities) {
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
        this.staircaseLocation = room.staircaseLocation();
        this.shutterDoorMask = room.shutterDoorMask();
        this.entities = entities;
    }

    public static ActiveRoom from(LoadedRoom room) {
        return new ActiveRoom(room);
    }

    public static ActiveRoom from(LoadedRoom room, RoomEntitySnapshot entities) {
        return new ActiveRoom(room, entities);
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

    public int staircaseLocation() {
        return staircaseLocation;
    }

    public int shutterDoorMask() {
        return shutterDoorMask;
    }

    void openShutterDoors() {
        shutterDoorMask = 0;
    }

    void retainClosedShutterDoors(int mask) {
        shutterDoorMask &= mask;
    }

    /** Owned immutable entity state, suitable for retention by external callers. */
    public RoomEntitySnapshot entities() {
        if (entities == null) return null;
        if (!entities.samePresentationAs(ownedEntities)) {
            for (int i = 0; i < recentPresentations.length; i++) {
                var previous = recentPresentations[i];
                if (previous != null && entities.samePresentationAs(previous)) {
                    ownedEntities = previous;
                    return ownedEntities;
                }
            }
            ownedEntities = entities.ownedCopy();
            recentPresentations[nextPresentation] = ownedEntities;
            nextPresentation = (nextPresentation + 1) % recentPresentations.length;
        }
        return ownedEntities;
    }
    /** Immediate, same-thread room integration reads; never retain as historical state. */
    public RoomEntitySnapshot entityView() {
        return entities;
    }

    void replaceEntities(RoomEntitySnapshot entities) {
        this.entities = entities;
    }

    void replaceTilemap(int[] tileIds, int[] tileAttrs) {
        if (tileIds == null
                || tileIds.length != this.tileIds.length
                || tileAttrs == null
                || tileAttrs.length != this.tileAttrs.length) {
            throw new IllegalArgumentException("Replacement tilemap dimensions must match room");
        }
        System.arraycopy(tileIds, 0, this.tileIds, 0, tileIds.length);
        System.arraycopy(tileAttrs, 0, this.tileAttrs, 0, tileAttrs.length);
    }

    public void replaceFirstWarpTile(int tileLocation) {
        if (warps.isEmpty()) {
            return;
        }
        warps.set(0, warps.get(0).withTileLocation(tileLocation));
    }

    /** Cached read-only presentation, valid until the next tick or room mutation. */
    public RoomRenderSnapshot.View renderView() {
        return renderView;
    }

    public RoomRenderSnapshot renderSnapshot() {
        return new RoomRenderSnapshot(
                tileIds, tileAttrs, palettes, entities(), null, null, mapCategory == 0 && roomId == 0x32 ? 6 : 0);
    }
}
