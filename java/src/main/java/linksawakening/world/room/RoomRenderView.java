package linksawakening.world.room;

import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

/** Read-only room presentation. Live instances are valid only until the next tick or world mutation. */
public interface RoomRenderView {
    int tileCount();

    int tileId(int index);

    int attributeCount();

    int tileAttribute(int index);

    int paletteCount();

    int colorCount(int palette);

    int paletteColor(int palette, int color);

    RoomEntitySnapshot entities();

    int spritePaletteCount();

    int spriteColorCount(int palette);

    int spritePaletteColor(int palette, int color);

    InstrumentWarpVisual instrumentWarp();

    HeartPieceVisual heartPiece();

    int linkGrassPalette();

    /** Explicit ownership for retained scroll/capture history. */
    default RoomRenderSnapshot ownedCopy() {
        return this instanceof RoomRenderSnapshot snapshot
                ? snapshot
                : new RoomRenderSnapshot(
                        tileIds(),
                        tileAttrs(),
                        palettes(),
                        entities(),
                        instrumentWarp(),
                        heartPiece(),
                        linkGrassPalette());
    }

    default int[] tileIds() {
        int[] result = new int[tileCount()];
        for (int i = 0; i < result.length; i++) result[i] = tileId(i);
        return result;
    }

    default int[] tileAttrs() {
        int[] result = new int[attributeCount()];
        for (int i = 0; i < result.length; i++) result[i] = tileAttribute(i);
        return result;
    }

    default int[][] palettes() {
        int[][] result = new int[paletteCount()][];
        for (int p = 0; p < result.length; p++) {
            result[p] = new int[colorCount(p)];
            for (int c = 0; c < result[p].length; c++) result[p][c] = paletteColor(p, c);
        }
        return result;
    }
}
