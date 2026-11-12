package linksawakening.cutscene;

import java.util.List;
import java.util.Objects;

/** Immutable render state published by one opening-sequence frame. */
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
    /** @deprecated Use {@link Builder} for named construction. */
    @Deprecated(forRemoval = true)
    public IntroFrameSnapshot(
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
            int titleRevealRows) {
        this(
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
                null,
                null,
                0);
    }

    /** @deprecated Use {@link Builder} for named construction. */
    @Deprecated(forRemoval = true)
    public IntroFrameSnapshot {
        sceneId = Objects.requireNonNull(sceneId, "sceneId");
        substate = Objects.requireNonNull(substate, "substate");
        tilemap = tilemap.clone();
        attrmap = attrmap.clone();
        bgPalettes = copyPalettes(bgPalettes);
        objPalettes = copyPalettes(objPalettes);
        lineScrollX = lineScrollX == null ? null : lineScrollX.clone();
        sprites = List.copyOf(sprites);
        lineScrollY = lineScrollY == null ? null : lineScrollY.clone();
        animatedTiles = animatedTiles == null ? null : animatedTiles.clone();
    }

    @Override
    public int[] lineScrollX() {
        return lineScrollX == null ? null : lineScrollX.clone();
    }

    @Override
    public int[] lineScrollY() {
        return lineScrollY == null ? null : lineScrollY.clone();
    }

    @Override
    public byte[] animatedTiles() {
        return animatedTiles == null ? null : animatedTiles.clone();
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
