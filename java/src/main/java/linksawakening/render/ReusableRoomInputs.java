package linksawakening.render;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.room.RoomRenderSnapshot;
import linksawakening.world.room.RoomRenderView;

/** Borrowed room planes and cached immutable metadata; no live world data is copied per build. */
final class ReusableRoomInputs {
    int[] tiles, attributes;
    int[][] palettes, objectPalettes;
    RoomEntitySnapshot entities;
    InstrumentWarpVisual warp;
    HeartPieceVisual heart;
    int grassPalette;
    private RoomRenderView captured;
    private int[] liveTiles, liveAttributes;
    private int[][] livePalettes;
    private EntitySpriteSelection selection;
    private int[][] cachedObjectPalettes;

    void capture(RoomRenderView room) {
        if (room instanceof RoomRenderSnapshot && room == captured) {
            refreshSelection();
            return;
        }
        if (room == null) {
            set(null, null, null, null, null, null, 0);
            return;
        }
        if (!(room instanceof RoomRenderSnapshot)) {
            captureLive(room);
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

    private void captureLive(RoomRenderView room) {
        if (liveTiles == null || liveTiles.length != room.tileCount()) liveTiles = new int[room.tileCount()];
        if (liveAttributes == null || liveAttributes.length != room.attributeCount())
            liveAttributes = new int[room.attributeCount()];
        if (livePalettes == null || livePalettes.length != room.paletteCount())
            livePalettes = new int[room.paletteCount()][];
        for (int i = 0; i < liveTiles.length; i++) liveTiles[i] = room.tileId(i);
        for (int i = 0; i < liveAttributes.length; i++) liveAttributes[i] = room.tileAttribute(i);
        for (int p = 0; p < livePalettes.length; p++) {
            if (livePalettes[p] == null || livePalettes[p].length != room.colorCount(p))
                livePalettes[p] = new int[room.colorCount(p)];
            for (int c = 0; c < livePalettes[p].length; c++) livePalettes[p][c] = room.paletteColor(p, c);
        }
        set(
                liveTiles,
                liveAttributes,
                livePalettes,
                room.entities(),
                room.instrumentWarp(),
                room.heartPiece(),
                room.linkGrassPalette());
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
