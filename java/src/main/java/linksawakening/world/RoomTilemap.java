package linksawakening.world;

public record RoomTilemap(int[] tileIds, int[] tileAttrs, int[] gbcOverlay, int[] renderValues) {

    public RoomTilemap {
        tileIds = tileIds.clone();
        tileAttrs = tileAttrs.clone();
        gbcOverlay = gbcOverlay == null ? null : gbcOverlay.clone();
        renderValues = renderValues == null ? null : renderValues.clone();
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
    public int[] gbcOverlay() {
        return gbcOverlay == null ? null : gbcOverlay.clone();
    }

    @Override
    public int[] renderValues() {
        return renderValues == null ? null : renderValues.clone();
    }
}
