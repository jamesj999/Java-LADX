package linksawakening.world.room;

public record EntityPickupInputs(int frameCounter, Link link, SwordBox sword) {
    public record Link(int x, int y, boolean airborne, boolean interactive, int direction, int z) {}
}
