package linksawakening.world.room;

import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

import linksawakening.world.entity.RoomEntitySnapshot;

public record RoomRenderSnapshot(int[] tileIds, int[] tileAttrs, int[][] palettes,
                                 RoomEntitySnapshot entities, InstrumentWarpVisual instrumentWarp, HeartPieceVisual heartPiece, int linkGrassPalette) {

    public RoomRenderSnapshot(int[] tiles, int[] attrs, int[][] palettes, RoomEntitySnapshot entities,
                              InstrumentWarpVisual warp, HeartPieceVisual heartPiece) {
        this(tiles, attrs, palettes, entities, warp, heartPiece, 0);
    }

    public RoomRenderSnapshot(int[] tiles, int[] attrs, int[][] palettes, RoomEntitySnapshot entities,
                              InstrumentWarpVisual warp) {
        this(tiles, attrs, palettes, entities, warp, null);
    }

    public RoomRenderSnapshot(int[] tiles, int[] attrs, int[][] palettes, RoomEntitySnapshot entities) {
        this(tiles, attrs, palettes, entities, null);
    }

    public RoomRenderSnapshot(int[] tileIds, int[] tileAttrs, int[][] palettes) {
        this(tileIds, tileAttrs, palettes, null);
    }

    public RoomRenderSnapshot {
        tileIds = tileIds.clone();
        tileAttrs = tileAttrs.clone();
        palettes = clonePalettes(palettes);
    }

    @Override
    public int[] tileIds() {
        return tileIds.clone();
    }

    @Override
    public int[] tileAttrs() {
        return tileAttrs.clone();
    }

    @Override
    public int[][] palettes() {
        return clonePalettes(palettes);
    }

    private static int[][] clonePalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}
