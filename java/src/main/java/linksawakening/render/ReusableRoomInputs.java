package linksawakening.render;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.room.RoomRenderSnapshot;

/** Borrowed room planes and cached immutable metadata; no live world data is copied per build. */
final class ReusableRoomInputs {
    int[] tiles, attributes;
    int[][] palettes, objectPalettes;
    RoomEntitySnapshot entities;
    InstrumentWarpVisual warp;
    HeartPieceVisual heart;
    int grassPalette;
    private RoomRenderSnapshot captured;
    private EntitySpriteSelection selection;
    private int[][] cachedObjectPalettes;

    void capture(RoomRenderSnapshot room) {
        if (room != null && room == captured) {
            refreshSelection();
            return;
        }
        if (room == null) {
            set(null, null, null, null, null, null, 0);
            return;
        }
        set(
                room.tileIds(),
                room.tileAttrs(),
                room.palettes(),
                room.entities(),
                room.instrumentWarp(),
                room.heartPiece(),
                room.linkGrassPalette());
        captured = room;
    }

    void set(
            int[] tiles,
            int[] attributes,
            int[][] palettes,
            RoomEntitySnapshot entities,
            InstrumentWarpVisual warp,
            HeartPieceVisual heart,
            int grassPalette) {
        captured = null;
        this.tiles = tiles;
        this.attributes = attributes;
        this.palettes = palettes;
        this.entities = entities;
        this.warp = warp;
        this.heart = heart;
        this.grassPalette = grassPalette;
        refreshSelection();
    }

    void refreshSelection() {
        EntitySpriteSelection next = entities == null ? null : entities.spriteSelection();
        if (next == null) {
            objectPalettes = null;
            return;
        }
        if (next != selection) {
            selection = next;
            cachedObjectPalettes = next.objectPalettes();
        }
        objectPalettes = cachedObjectPalettes;
    }

    RoomRenderSnapshot snapshot() {
        return tiles == null
                ? null
                : new RoomRenderSnapshot(tiles, attributes, palettes, entities, warp, heart, grassPalette);
    }
}
