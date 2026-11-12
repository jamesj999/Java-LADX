package linksawakening.world.entity.event;

public record InstrumentCompletionEvent(int slot, int mapId) implements EntityRequest {}
