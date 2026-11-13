package linksawakening.world.room;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

/** Cached read-only projection; the owner controls updates and room replacement. */
final class RoomRenderLiveView implements RoomRenderSnapshot.View {
    private final ActiveRoom room;
    private final Visuals visuals;

    private EntitySpriteSelection paletteSelection;
    private int[][] spritePalettes = EMPTY_PALETTES;

    private RoomRenderLiveView(ActiveRoom room, Visuals visuals) {
        this.room = java.util.Objects.requireNonNull(room);
        this.visuals = java.util.Objects.requireNonNull(visuals);
    }

    static RoomRenderLiveView snapshot(ActiveRoom room) {
        return new RoomRenderLiveView(room, NO_VISUALS);
    }

    static RoomRenderLiveView live(ActiveRoom room, RoomEntityRuntime runtime) {
        return new RoomRenderLiveView(room, new RuntimeVisuals(runtime));
    }

    private interface Visuals {
        InstrumentWarpVisual instrumentWarp();

        HeartPieceVisual heartPiece();
    }

    private static final Visuals NO_VISUALS = new Visuals() {
        public InstrumentWarpVisual instrumentWarp() {
            return null;
        }

        public HeartPieceVisual heartPiece() {
            return null;
        }
    };

    private record RuntimeVisuals(RoomEntityRuntime runtime) implements Visuals {
        RuntimeVisuals {
            java.util.Objects.requireNonNull(runtime);
        }

        public InstrumentWarpVisual instrumentWarp() {
            return runtime.inspection().instrumentWarpVisual();
        }

        public HeartPieceVisual heartPiece() {
            return runtime.inspection().heartPieceVisual();
        }
    }

    public int tileCount() {
        return room.tileIds().length;
    }

    public int tileId(int index) {
        return room.tileIds()[index];
    }

    public int attributeCount() {
        return room.tileAttrs().length;
    }

    public int tileAttribute(int index) {
        return room.tileAttrs()[index];
    }

    public int paletteCount() {
        return room.palettes().length;
    }

    public int colorCount(int palette) {
        return room.palettes()[palette].length;
    }

    public int paletteColor(int palette, int color) {
        return room.palettes()[palette][color];
    }

    public RoomEntitySnapshot entities() {
        return room.entityView();
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
        return room.mapCategory() == 0 && room.roomId() == 0x32 ? 6 : 0;
    }
    // Active visual events remain owned values; inactive channels return null without allocation.
    public InstrumentWarpVisual instrumentWarp() {
        return visuals.instrumentWarp();
    }

    public HeartPieceVisual heartPiece() {
        return visuals.heartPiece();
    }
}
