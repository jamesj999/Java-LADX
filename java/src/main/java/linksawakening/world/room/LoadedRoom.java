package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

public record LoadedRoom(
        LoadedRoomSpec.Identity identity,
        LoadedRoomSpec.Objects objects,
        LoadedRoomSpec.Graphics graphics,
        LoadedRoomSpec.Exits exits,
        RoomEntitySnapshot entities) {
    public LoadedRoom(LoadedRoomSpec spec) {
        this(spec.identity(), spec.objects(), spec.graphics(), spec.exits(), spec.entities());
    }

    public LoadedRoom {
        objects = new LoadedRoomSpec.Objects(
                objects.roomObjectsArea().clone(),
                objects.gbcOverlay() == null ? null : objects.gbcOverlay().clone(),
                objects.renderValues() == null ? null : objects.renderValues().clone());
        graphics = new LoadedRoomSpec.Graphics(
                graphics.tileIds().clone(), graphics.tileAttrs().clone(), clonePalettes(graphics.palettes()));
        exits = new LoadedRoomSpec.Exits(
                List.copyOf(exits.warps()),
                exits.indoorHasSouthEntrance(),
                exits.shutterDoorMask() & 0x0F,
                exits.staircaseLocation());
        if (exits.staircaseLocation() < -1 || exits.staircaseLocation() > 0xFF)
            throw new IllegalArgumentException("Staircase location must be -1 or an unsigned byte");
    }

    @Override
    public LoadedRoomSpec.Objects objects() {
        return new LoadedRoomSpec.Objects(roomObjectsArea(), gbcOverlay(), renderValues());
    }

    @Override
    public LoadedRoomSpec.Graphics graphics() {
        return new LoadedRoomSpec.Graphics(tileIds(), tileAttrs(), palettes());
    }

    public int roomId() {
        return identity.roomId();
    }

    public int mapCategory() {
        return identity.mapCategory();
    }

    public int mapId() {
        return identity.mapId();
    }

    public int animatedTilesGroup() {
        return identity.animatedTilesGroup();
    }

    public int[] roomObjectsArea() {
        return objects.roomObjectsArea().clone();
    }

    public int[] gbcOverlay() {
        return objects.gbcOverlay() == null ? null : objects.gbcOverlay().clone();
    }

    public int[] renderValues() {
        return objects.renderValues() == null ? null : objects.renderValues().clone();
    }

    public int[] tileIds() {
        return graphics.tileIds().clone();
    }

    public int[] tileAttrs() {
        return graphics.tileAttrs().clone();
    }

    public int[][] palettes() {
        return clonePalettes(graphics.palettes());
    }

    public List<Warp> warps() {
        return exits.warps();
    }

    public boolean indoorHasSouthEntrance() {
        return exits.indoorHasSouthEntrance();
    }

    public int shutterDoorMask() {
        return exits.shutterDoorMask();
    }

    public int staircaseLocation() {
        return exits.staircaseLocation();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + Integer.hashCode(identity.roomId());
        hash = 31 * hash + Integer.hashCode(identity.mapCategory());
        hash = 31 * hash + Integer.hashCode(identity.mapId());
        hash = 31 * hash + Integer.hashCode(identity.animatedTilesGroup());
        hash = 31 * hash + java.util.Objects.hashCode(objects.roomObjectsArea());
        hash = 31 * hash + java.util.Objects.hashCode(objects.gbcOverlay());
        hash = 31 * hash + java.util.Objects.hashCode(objects.renderValues());
        hash = 31 * hash + java.util.Objects.hashCode(graphics.tileIds());
        hash = 31 * hash + java.util.Objects.hashCode(graphics.tileAttrs());
        hash = 31 * hash + java.util.Objects.hashCode(graphics.palettes());
        hash = 31 * hash + java.util.Objects.hashCode(exits.warps());
        hash = 31 * hash + Boolean.hashCode(exits.indoorHasSouthEntrance());
        hash = 31 * hash + Integer.hashCode(exits.shutterDoorMask());
        hash = 31 * hash + Integer.hashCode(exits.staircaseLocation());
        hash = 31 * hash + java.util.Objects.hashCode(entities);
        return hash;
    }

    @Override
    public String toString() {
        return "LoadedRoom[" + "roomId=" + identity.roomId() + ", " + "mapCategory=" + identity.mapCategory() + ", "
                + "mapId=" + identity.mapId() + ", " + "animatedTilesGroup=" + identity.animatedTilesGroup() + ", "
                + "roomObjectsArea=" + objects.roomObjectsArea() + ", " + "gbcOverlay=" + objects.gbcOverlay() + ", "
                + "renderValues=" + objects.renderValues() + ", " + "tileIds=" + graphics.tileIds() + ", "
                + "tileAttrs=" + graphics.tileAttrs() + ", " + "palettes=" + graphics.palettes() + ", " + "warps="
                + exits.warps() + ", " + "indoorHasSouthEntrance=" + exits.indoorHasSouthEntrance() + ", "
                + "shutterDoorMask=" + exits.shutterDoorMask() + ", " + "staircaseLocation=" + exits.staircaseLocation()
                + ", " + "entities=" + entities + "]";
    }

    private static int[][] clonePalettes(int[][] source) {
        if (source == null) {
            return null;
        }
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
