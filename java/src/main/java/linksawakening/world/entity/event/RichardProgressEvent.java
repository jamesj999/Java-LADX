package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

public record RichardProgressEvent(int slot, Kind kind, int goldenLeavesCount) implements EntityRequest {
    public enum Kind { START_QUEST, RETURN_ALL_LEAVES }
}
