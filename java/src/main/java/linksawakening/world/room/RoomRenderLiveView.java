package linksawakening.world.room;

import java.util.function.Supplier;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

/** Cached read-only projection; the owner controls updates and room replacement. */
final class RoomRenderLiveView implements RoomRenderSnapshot.View {
    private final Supplier<ActiveRoom> room;
    private final Supplier<RoomEntityRuntime> runtime;
    private EntitySpriteSelection paletteSelection;
    private int[][] spritePalettes = EMPTY_PALETTES;

    RoomRenderLiveView(Supplier<ActiveRoom> room, Supplier<RoomEntityRuntime> runtime) {
        this.room = room;
        this.runtime = runtime;
    }

    public int tileCount() {
        return room.get().tileIds().length;
    }

    public int tileId(int index) {
        return room.get().tileIds()[index];
    }

    public int attributeCount() {
        return room.get().tileAttrs().length;
    }

    public int tileAttribute(int index) {
        return room.get().tileAttrs()[index];
    }

    public int paletteCount() {
        return room.get().palettes().length;
    }

    public int colorCount(int palette) {
        return room.get().palettes()[palette].length;
    }

    public int paletteColor(int palette, int color) {
        return room.get().palettes()[palette][color];
    }

    public RoomEntitySnapshot entities() {
        return room.get().entityView();
    }

    public int spritePaletteCount() {
        return spritePalettes().length;
    }

    public int spriteColorCount(int palette) {
        return spritePalettes()[palette].length;
    }

    public int spritePaletteColor(int palette, int color) {
        return spritePalettes()[palette][color];
    }

    private int[][] spritePalettes() {
        var entities = entities();
        var selection = entities == null ? null : entities.spriteSelection();
        if (selection != paletteSelection) {
            spritePalettes = selection == null ? EMPTY_PALETTES : selection.objectPalettes();
            paletteSelection = selection;
        }
        return spritePalettes;
    }

    private static final int[][] EMPTY_PALETTES = new int[0][];

    public int linkGrassPalette() {
        return room.get().mapCategory() == 0 && room.get().roomId() == 0x32 ? 6 : 0;
    }
    // Active visual events remain owned values; inactive channels return null without allocation.
    public InstrumentWarpVisual instrumentWarp() {
        return runtime == null || runtime.get() == null
                ? null
                : runtime.get().inspection().instrumentWarpVisual();
    }

    public HeartPieceVisual heartPiece() {
        return runtime == null || runtime.get() == null
                ? null
                : runtime.get().inspection().heartPieceVisual();
    }
}
