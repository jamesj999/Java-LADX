package linksawakening.world;

import java.util.List;

public record LoadedRoom(int roomId,
                         int mapCategory,
                         int mapId,
                         int animatedTilesGroup,
                         int[] roomObjectsArea,
                         int[] gbcOverlay,
                         int[] renderValues,
                         int[] tileIds,
                         int[] tileAttrs,
                         int[][] palettes,
                         List<Warp> warps,
                         boolean indoorHasSouthEntrance) {

    public LoadedRoom {
        roomObjectsArea = roomObjectsArea.clone();
        gbcOverlay = gbcOverlay == null ? null : gbcOverlay.clone();
        renderValues = renderValues == null ? null : renderValues.clone();
        tileIds = tileIds.clone();
        tileAttrs = tileAttrs.clone();
        palettes = clonePalettes(palettes);
        warps = List.copyOf(warps);
    }

    @Override
    public int[] roomObjectsArea() {
        return roomObjectsArea.clone();
    }

    @Override
    public int[] gbcOverlay() {
        return gbcOverlay == null ? null : gbcOverlay.clone();
    }

    @Override
    public int[] renderValues() {
        return renderValues == null ? null : renderValues.clone();
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
        if (source == null) {
            return null;
        }
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
