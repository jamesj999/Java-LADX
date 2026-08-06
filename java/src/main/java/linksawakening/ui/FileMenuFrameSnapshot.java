package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable render state for one file-selection or New Game frame.
 */
public record FileMenuFrameSnapshot(
    String sceneId,
    int selectedSlot,
    boolean commandRowArrowShifted,
    boolean creation,
    int selectedCharacter,
    int namePosition,
    int[] nameBytes,
    int[] tilemap,
    int[] attrmap,
    int[][] bgPalettes,
    int[][] objectPalettes,
    List<IntroSprite> sprites
) {

    public FileMenuFrameSnapshot {
        nameBytes = nameBytes.clone();
        tilemap = tilemap.clone();
        attrmap = attrmap.clone();
        bgPalettes = copyPalettes(bgPalettes);
        objectPalettes = copyPalettes(objectPalettes);
        sprites = List.copyOf(sprites);
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
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
    public int[][] objectPalettes() {
        return copyPalettes(objectPalettes);
    }

    private static int[][] copyPalettes(int[][] palettes) {
        if (palettes == null) {
            return null;
        }
        int[][] copy = new int[palettes.length][];
        for (int index = 0; index < palettes.length; index++) {
            copy[index] = palettes[index] == null ? null : palettes[index].clone();
        }
        return copy;
    }
}
