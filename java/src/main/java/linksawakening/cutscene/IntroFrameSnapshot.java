package linksawakening.cutscene;

import java.util.List;
import java.util.Objects;

/**
 * Immutable render state published by one opening-sequence frame.
 * Line-scroll arrays are absent when the scene uses uniform scroll; animated tiles are absent
 * when the frame performs no VRAM upload. The has* predicates expose those domain states while
 * the nullable array accessors retain the existing renderer contract.
 */
public record IntroFrameSnapshot(
        String sceneId,
        String substate,
        int frameCounter,
        int scrollX,
        int scrollY,
        int[] lineScrollX,
        int verticalWaveOffset,
        int[] tilemap,
        int[] attrmap,
        int[][] bgPalettes,
        int[][] objPalettes,
        List<IntroSprite> sprites,
        int titleRevealRows,
        int[] lineScrollY,
        byte[] animatedTiles,
        int animatedTilesDestination) {

    private static final IntroFrameSnapshot EMPTY = new Builder()
            .sceneId("")
            .substate("")
            .tilemap(new int[0])
            .attrmap(new int[0])
            .bgPalettes(new int[0][])
            .objPalettes(new int[0][])
            .sprites(List.of())
            .titleRevealRows(7)
            .build();

    /** Empty presentation before a sequence has published its initial frame. */
    static IntroFrameSnapshot empty() {
        return EMPTY;
    }

    /** @deprecated Use {@link Builder} for named construction. */
    @Deprecated(forRemoval = true)
    public IntroFrameSnapshot {
        Objects.requireNonNull(sceneId, "sceneId");
        Objects.requireNonNull(substate, "substate");
        tilemap = tilemap.clone();
        attrmap = attrmap.clone();
        bgPalettes = copyPalettes(bgPalettes);
        objPalettes = copyPalettes(objPalettes);
        lineScrollX = copyScroll(lineScrollX);
        sprites = List.copyOf(sprites);
        lineScrollY = copyScroll(lineScrollY);
        animatedTiles = copyAnimatedTiles(animatedTiles);
    }

    public boolean hasLineScrollX() {
        return lineScrollX != null;
    }

    public boolean hasLineScrollY() {
        return lineScrollY != null;
    }

    public boolean hasAnimatedTiles() {
        return animatedTiles != null;
    }

    /** Reads a line without allocating a defensive array copy; empty scroll maps use fallback. */
    public int scrollXAt(int screenY, int fallback) {
        if (!hasLineScrollX() || lineScrollX.length == 0) return fallback;
        return lineScrollX[Math.max(0, Math.min(screenY, lineScrollX.length - 1))];
    }

    private static int[] copyScroll(int[] scroll) {
        return scroll == null ? null : scroll.clone();
    }

    private static byte[] copyAnimatedTiles(byte[] tiles) {
        return tiles == null ? null : tiles.clone();
    }

    @Override
    public int[] lineScrollX() {
        return copyScroll(lineScrollX);
    }

    @Override
    public int[] lineScrollY() {
        return copyScroll(lineScrollY);
    }

    @Override
    public byte[] animatedTiles() {
        return copyAnimatedTiles(animatedTiles);
    }

    @Override
    public int[] tilemap() {
        return tilemap.clone();
    }

    @Override
    public int[] attrmap() {
        return attrmap.clone();
    }

    @Override
    public int[][] bgPalettes() {
        return copyPalettes(bgPalettes);
    }

    @Override
    public int[][] objPalettes() {
        return copyPalettes(objPalettes);
    }

    private static int[][] copyPalettes(int[][] source) {
        Objects.requireNonNull(source, "palettes");
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    /** Reusable named construction; build preserves the record's defensive copies. */
    public static final class Builder {
        private String sceneId;
        private String substate;
        private int frameCounter;
        private int scrollX;
        private int scrollY;
        private int[] lineScrollX;
        private int verticalWaveOffset;
        private int[] tilemap;
        private int[] attrmap;
        private int[][] bgPalettes;
        private int[][] objPalettes;
        private List<IntroSprite> sprites;
        private int titleRevealRows;
        private int[] lineScrollY;
        private byte[] animatedTiles;
        private int animatedTilesDestination;

        public Builder sceneId(String value) {
            sceneId = value;
            return this;
        }

        public Builder substate(String value) {
            substate = value;
            return this;
        }

        public Builder frameCounter(int value) {
            frameCounter = value;
            return this;
        }

        public Builder scrollX(int value) {
            scrollX = value;
            return this;
        }

        public Builder scrollY(int value) {
            scrollY = value;
            return this;
        }

        public Builder lineScrollX(int[] value) {
            lineScrollX = value;
            return this;
        }

        public Builder verticalWaveOffset(int value) {
            verticalWaveOffset = value;
            return this;
        }

        public Builder tilemap(int[] value) {
            tilemap = value;
            return this;
        }

        public Builder attrmap(int[] value) {
            attrmap = value;
            return this;
        }

        public Builder bgPalettes(int[][] value) {
            bgPalettes = value;
            return this;
        }

        public Builder objPalettes(int[][] value) {
            objPalettes = value;
            return this;
        }

        public Builder sprites(List<IntroSprite> value) {
            sprites = value;
            return this;
        }

        public Builder titleRevealRows(int value) {
            titleRevealRows = value;
            return this;
        }

        public Builder lineScrollY(int[] value) {
            lineScrollY = value;
            return this;
        }

        public Builder animatedTiles(byte[] value) {
            animatedTiles = value;
            return this;
        }

        public Builder animatedTilesDestination(int value) {
            animatedTilesDestination = value;
            return this;
        }

        public IntroFrameSnapshot build() {
            return new IntroFrameSnapshot(
                    sceneId,
                    substate,
                    frameCounter,
                    scrollX,
                    scrollY,
                    lineScrollX,
                    verticalWaveOffset,
                    tilemap,
                    attrmap,
                    bgPalettes,
                    objPalettes,
                    sprites,
                    titleRevealRows,
                    lineScrollY,
                    animatedTiles,
                    animatedTilesDestination);
        }
    }
}
