package linksawakening.entity;

import linksawakening.world.EntityRoomLoader;

import java.util.HashMap;
import java.util.Map;

/** Immutable room selection for the four standard entity sprite slots. */
public final class EntitySpriteSelection {

    private final EntityRoomLoader.RoomTable roomTable;
    private final int roomId;
    private final int groupIndex;
    private final int[] sheetValues;
    private final boolean standardSheets;
    private final int[][] objectPalettes;
    private final Map<Integer, EntitySpriteDefinition> spriteOverrides;
    private final EntitySpriteDefinition burningSpriteDefinition;

    public EntitySpriteSelection(EntityRoomLoader.RoomTable roomTable, int roomId,
                                 int groupIndex, int[] sheetValues,
                                 boolean standardSheets, int[][] objectPalettes) {
        this(roomTable, roomId, groupIndex, sheetValues, standardSheets, objectPalettes,
            Map.of(), null);
    }

    public EntitySpriteSelection(EntityRoomLoader.RoomTable roomTable, int roomId,
                                 int groupIndex, int[] sheetValues,
                                 boolean standardSheets, int[][] objectPalettes,
                                 Map<Integer, EntitySpriteDefinition> spriteOverrides) {
        this(roomTable, roomId, groupIndex, sheetValues, standardSheets, objectPalettes,
            spriteOverrides, null);
    }

    private EntitySpriteSelection(EntityRoomLoader.RoomTable roomTable, int roomId,
                                  int groupIndex, int[] sheetValues,
                                  boolean standardSheets, int[][] objectPalettes,
                                  Map<Integer, EntitySpriteDefinition> spriteOverrides,
                                  EntitySpriteDefinition burningSpriteDefinition) {
        this.roomTable = roomTable;
        this.roomId = roomId;
        this.groupIndex = groupIndex;
        this.sheetValues = sheetValues.clone();
        this.standardSheets = standardSheets;
        this.objectPalettes = clonePalettes(objectPalettes);
        if (spriteOverrides == null) {
            throw new IllegalArgumentException("Entity sprite overrides cannot be null");
        }
        Map<Integer, EntitySpriteDefinition> overrides = new HashMap<>();
        for (Map.Entry<Integer, EntitySpriteDefinition> entry : spriteOverrides.entrySet()) {
            int type = entry.getKey();
            if ((type & ~0xFF) != 0 || entry.getValue() == null) {
                throw new IllegalArgumentException("Entity sprite overrides must contain valid types");
            }
            overrides.put(type, entry.getValue());
        }
        this.spriteOverrides = Map.copyOf(overrides);
        if (burningSpriteDefinition != null && !burningSpriteDefinition.supported()) {
            throw new IllegalArgumentException("Burning sprite definition must be supported");
        }
        this.burningSpriteDefinition = burningSpriteDefinition;
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

    public EntitySpriteDefinition spriteOverrideFor(int entityType) {
        return spriteOverrides.get(entityType);
    }

    public Map<Integer, EntitySpriteDefinition> spriteOverrides() {
        return spriteOverrides;
    }

    public EntitySpriteDefinition burningSpriteDefinition() {
        return burningSpriteDefinition;
    }

    public EntitySpriteSelection withBurningSpriteDefinition(EntitySpriteDefinition definition) {
        if (definition != null && !definition.supported()) {
            throw new IllegalArgumentException("Burning sprite definition must be supported");
        }
        return new EntitySpriteSelection(roomTable, roomId, groupIndex, sheetValues,
            standardSheets, objectPalettes, spriteOverrides, definition);
    }

    public EntitySpriteSelection withSpriteOverrides(
        Map<Integer, EntitySpriteDefinition> overrides) {
        return new EntitySpriteSelection(roomTable, roomId, groupIndex, sheetValues,
            standardSheets, objectPalettes, overrides, burningSpriteDefinition);
    }

    public EntitySpriteSelection withSpriteOverride(int entityType,
                                                    EntitySpriteDefinition definition) {
        Map<Integer, EntitySpriteDefinition> overrides = new HashMap<>(spriteOverrides);
        overrides.put(entityType, definition);
        return withSpriteOverrides(overrides);
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
