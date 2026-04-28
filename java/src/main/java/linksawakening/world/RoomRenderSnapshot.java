package linksawakening.world;

public record RoomRenderSnapshot(int[] tileIds, int[] tileAttrs, int[][] palettes) {

    public RoomRenderSnapshot {
        tileIds = tileIds.clone();
        tileAttrs = tileAttrs.clone();
        palettes = clonePalettes(palettes);
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
