package linksawakening.world.room;

import java.util.Objects;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

/** Cached read-only projection; the owner controls updates and room replacement. */
final class RoomRenderLiveView implements RoomRenderSnapshot.View {
    private final ActiveRoom room;
    private final Visuals visuals;

    // Optional sprite metadata/cache key, including data-only ActiveRoom projections.
    private EntitySpriteSelection paletteSelection;
    private int[][] spritePalettes = EMPTY_PALETTES;

    private RoomRenderLiveView(ActiveRoom room, Visuals visuals) {
        this.room = Objects.requireNonNull(room);
        this.visuals = Objects.requireNonNull(visuals);
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
        @Override
        public InstrumentWarpVisual instrumentWarp() {
            return null;
        }

        @Override
        public HeartPieceVisual heartPiece() {
            return null;
        }
    };

    private record RuntimeVisuals(RoomEntityRuntime runtime) implements Visuals {
        RuntimeVisuals {
            Objects.requireNonNull(runtime);
        }

        @Override
        public InstrumentWarpVisual instrumentWarp() {
            return runtime.inspection().instrumentWarpVisual();
        }

        @Override
        public HeartPieceVisual heartPiece() {
            return runtime.inspection().heartPieceVisual();
        }
    }

    @Override
    public int tileCount() {
        return room.tileIds().length;
    }

    @Override
    public int tileId(int index) {
        return room.tileIds()[index];
    }

    @Override
    public int attributeCount() {
        return room.tileAttrs().length;
    }

    @Override
    public int tileAttribute(int index) {
        return room.tileAttrs()[index];
    }

    @Override
    public int paletteCount() {
        return room.palettes().length;
    }

    @Override
    public int colorCount(int palette) {
        return room.palettes()[palette].length;
    }

    @Override
    public int paletteColor(int palette, int color) {
        return room.palettes()[palette][color];
    }

    @Override
    public RoomEntitySnapshot entities() {
        return room.entityView();
    }

    @Override
    public int spritePaletteCount() {
        return spritePalettes().length;
    }

    @Override
    public int spriteColorCount(int palette) {
        return spritePalettes()[palette].length;
    }

    @Override
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

    @Override
    public int linkGrassPalette() {
        return room.mapCategory() == 0 && room.roomId() == 0x32 ? 6 : 0;
    }
    // Active visual events remain owned values; inactive channels return null without allocation.
    @Override
    public InstrumentWarpVisual instrumentWarp() {
        return visuals.instrumentWarp();
    }

    @Override
    public HeartPieceVisual heartPiece() {
        return visuals.heartPiece();
    }
}
