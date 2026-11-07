package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

public record WitchExchangeEvent(int slot, int inventorySlot) implements EntityRequest {
}
