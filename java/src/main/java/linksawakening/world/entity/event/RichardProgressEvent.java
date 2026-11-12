package linksawakening.world.entity.event;

public record RichardProgressEvent(int slot, Kind kind, int goldenLeavesCount) implements EntityRequest {
    public enum Kind {
        START_QUEST,
        RETURN_ALL_LEAVES
    }
}
