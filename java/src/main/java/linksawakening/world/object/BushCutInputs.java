package linksawakening.world.object;

record BushCutInputs(
        SwordProbe sword,
        int roomId,
        boolean isGbcOverworld,
        int[] roomObjectsArea,
        int[] mutableRenderValues,
        int[] fallbackOverlay,
        int[] roomTileIds,
        int[] roomTileAttrs) {
    public record SwordProbe(boolean active, linksawakening.state.Direction direction, int x, int y) {}
}
