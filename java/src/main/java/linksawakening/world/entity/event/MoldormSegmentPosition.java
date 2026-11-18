package linksawakening.world.entity.event;

public record MoldormSegmentPosition(int x, int y) {
    public MoldormSegmentPosition {
        x &= 0xFF;
        y &= 0xFF;
    }
}
