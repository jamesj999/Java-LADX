package linksawakening.entity;

import linksawakening.world.EntityRoomLoader;

/** Immutable room selection for the four standard entity sprite slots. */
public final class EntitySpriteSelection {

    private final EntityRoomLoader.RoomTable roomTable;
    private final int roomId;
    private final int groupIndex;
    private final int[] sheetValues;
    private final boolean standardSheets;
    private final int[][] objectPalettes;

    public EntitySpriteSelection(EntityRoomLoader.RoomTable roomTable, int roomId,
                                 int groupIndex, int[] sheetValues,
                                 boolean standardSheets, int[][] objectPalettes) {
        this.roomTable = roomTable;
        this.roomId = roomId;
        this.groupIndex = groupIndex;
        this.sheetValues = sheetValues.clone();
        this.standardSheets = standardSheets;
        this.objectPalettes = clonePalettes(objectPalettes);
    }

    public EntityRoomLoader.RoomTable roomTable() {
        return roomTable;
    }

    public int roomId() {
        return roomId;
    }

    public int groupIndex() {
        return groupIndex;
    }

    public int[] sheetValues() {
        return sheetValues.clone();
    }

    public boolean hasStandardSheets() {
        return standardSheets;
    }

    public int[][] objectPalettes() {
        return clonePalettes(objectPalettes);
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
