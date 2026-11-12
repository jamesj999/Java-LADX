package linksawakening.world.room;

import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

public record RoomRenderSnapshot(
        int[] tileIds,
        int[] tileAttrs,
        int[][] palettes,
        RoomEntitySnapshot entities,
        InstrumentWarpVisual instrumentWarp,
        HeartPieceVisual heartPiece,
        int linkGrassPalette)
        implements RoomRenderView {

    /**
     * Read-only same-thread render data. A cached view is valid only until the next
     * simulation tick or room change; never retain it as a historical frame.
     * Indexed access exposes no writable backing arrays. Use renderSnapshot() for ownership.
     */
    public interface View extends RoomRenderView {}

    public RoomRenderSnapshot(
            int[] tiles,
            int[] attrs,
            int[][] palettes,
            RoomEntitySnapshot entities,
            InstrumentWarpVisual warp,
            HeartPieceVisual heartPiece) {
        this(tiles, attrs, palettes, entities, warp, heartPiece, 0);
    }

    public RoomRenderSnapshot(
            int[] tiles, int[] attrs, int[][] palettes, RoomEntitySnapshot entities, InstrumentWarpVisual warp) {
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
        entities = entities == null ? null : entities.ownedCopy();
    }

    public int tileCount() {
        return tileIds.length;
    }

    public int tileId(int index) {
        return tileIds[index];
    }

    public int attributeCount() {
        return tileAttrs.length;
    }

    public int tileAttribute(int index) {
        return tileAttrs[index];
    }

    public int paletteCount() {
        return palettes.length;
    }

    public int colorCount(int palette) {
        return palettes[palette].length;
    }

    public int paletteColor(int palette, int color) {
        return palettes[palette][color];
    }

    public int spritePaletteCount() {
        return entityPalettes().length;
    }

    public int spriteColorCount(int palette) {
        return entityPalettes()[palette].length;
    }

    public int spritePaletteColor(int palette, int color) {
        return entityPalettes()[palette][color];
    }

    private int[][] entityPalettes() {
        return entities == null || entities.spriteSelection() == null
                ? new int[0][]
                : entities.spriteSelection().objectPalettes();
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
