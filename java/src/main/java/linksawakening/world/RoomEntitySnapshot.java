package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Immutable room entity slots captured at room-load time. */
public final class RoomEntitySnapshot {

    private final List<RoomEntity> slots;
    private final List<RoomEntity> loadedEntities;
    private final EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;

    public RoomEntitySnapshot(List<RoomEntity> slots) {
        this(slots, null, null);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection) {
        this(slots, spriteSelection, null);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles) {
        if (slots == null || slots.size() != EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("A room must expose exactly "
                + EntityRoomLoader.MAX_ENTITIES + " entity slots");
        }
        this.slots = List.copyOf(slots);
        this.spriteSelection = spriteSelection;
        this.spriteTiles = spriteTiles;
        List<RoomEntity> loaded = new ArrayList<>();
        for (RoomEntity entity : this.slots) {
            if (entity.loaded()) {
                loaded.add(entity);
            }
        }
        this.loadedEntities = List.copyOf(loaded);
    }

    public RoomEntitySnapshot withSpriteSelection(EntitySpriteSelection selection) {
        return new RoomEntitySnapshot(slots, selection, spriteTiles);
    }

    public RoomEntitySnapshot withSpriteTiles(EntitySpriteTileSnapshot tiles) {
        return new RoomEntitySnapshot(slots, spriteSelection, tiles);
    }

    public List<RoomEntity> slots() {
        return slots;
    }

    public List<RoomEntity> loadedEntities() {
        return loadedEntities;
    }

    public EntitySpriteSelection spriteSelection() {
        return spriteSelection;
    }

    public EntitySpriteTileSnapshot spriteTiles() {
        return spriteTiles;
    }
}
