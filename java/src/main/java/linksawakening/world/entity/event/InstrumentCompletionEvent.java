package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

public record InstrumentCompletionEvent(int slot, int mapId) implements EntityRequest {
}
