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
    int titleRevealRows
) {
    public IntroFrameSnapshot {
        sceneId = Objects.requireNonNull(sceneId, "sceneId");
        substate = Objects.requireNonNull(substate, "substate");
        tilemap = tilemap.clone();
        attrmap = attrmap.clone();
        bgPalettes = copyPalettes(bgPalettes);
        objPalettes = copyPalettes(objPalettes);
        lineScrollX = lineScrollX == null ? null : lineScrollX.clone();
        sprites = List.copyOf(sprites);
    }

    @Override
    public int[] lineScrollX() {
        return lineScrollX == null ? null : lineScrollX.clone();
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
}
