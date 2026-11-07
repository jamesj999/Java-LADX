package linksawakening.world.entity.event;







public record HeartPieceVisual(int pieces, int[] spritePairs) {
    public HeartPieceVisual { spritePairs = spritePairs.clone(); }
    @Override public int[] spritePairs() { return spritePairs.clone(); }
}
