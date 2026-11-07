package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A ROM handler request carrying the complete Link-side push response. */
public record LinkFinalPositionRequest(int sourceSlot, boolean resetPegasusBoots,
                                       boolean resetHookshotChain,
                                       boolean markLinkPushing,
                                       boolean clearLinkPositionIncrement) implements EntityRequest {
    public LinkFinalPositionRequest(int sourceSlot) {
        this(sourceSlot, false, false, false, false);
    }

    public LinkFinalPositionRequest(int sourceSlot, boolean resetPegasusBoots,
                                    boolean resetHookshotChain,
                                    boolean markLinkPushing) {
        this(sourceSlot, resetPegasusBoots, resetHookshotChain, markLinkPushing, false);
    }

    public LinkFinalPositionRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Link final-position source slot out of range: "
                + sourceSlot);
        }
    }
}
