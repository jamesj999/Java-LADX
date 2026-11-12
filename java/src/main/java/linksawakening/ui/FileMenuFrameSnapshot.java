package linksawakening.ui;

import java.util.List;
import linksawakening.cutscene.IntroSprite;

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
        List<IntroSprite> sprites) {

    /** @deprecated Use {@link Builder} for named construction. */
    @Deprecated(forRemoval = true)
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

    /** Reusable named construction; build preserves the record's defensive copies. */
    public static final class Builder {
        private String sceneId;
        private int selectedSlot;
        private boolean commandRowArrowShifted;
        private boolean creation;
        private int selectedCharacter;
        private int namePosition;
        private int[] nameBytes;
        private int[] tilemap;
        private int[] attrmap;
        private int[][] bgPalettes;
        private int[][] objectPalettes;
        private List<IntroSprite> sprites;

        public Builder sceneId(String value) {
            sceneId = value;
            return this;
        }

        public Builder selectedSlot(int value) {
            selectedSlot = value;
            return this;
        }

        public Builder commandRowArrowShifted(boolean value) {
            commandRowArrowShifted = value;
            return this;
        }

        public Builder creation(boolean value) {
            creation = value;
            return this;
        }

        public Builder selectedCharacter(int value) {
            selectedCharacter = value;
            return this;
        }

        public Builder namePosition(int value) {
            namePosition = value;
            return this;
        }

        public Builder nameBytes(int[] value) {
            nameBytes = value;
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

        public Builder objectPalettes(int[][] value) {
            objectPalettes = value;
            return this;
        }

        public Builder sprites(List<IntroSprite> value) {
            sprites = value;
            return this;
        }

        public FileMenuFrameSnapshot build() {
            return new FileMenuFrameSnapshot(
                    sceneId,
                    selectedSlot,
                    commandRowArrowShifted,
                    creation,
                    selectedCharacter,
                    namePosition,
                    nameBytes,
                    tilemap,
                    attrmap,
                    bgPalettes,
                    objectPalettes,
                    sprites);
        }
    }
}
