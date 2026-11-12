package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** LinkItemState owns its Link state and operations. */
final class LinkItemState {
    int romLinkPushing;
    int romAttackStepAnimationCountdown;

    void accessMarkRomLinkPushing(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Link pushing countdown must be an unsigned byte");
        }
        romLinkPushing = countdown;
    }

    void accessTickRomLinkPushing() {
        if (romLinkPushing > 0) {
            romLinkPushing--;
        }
    }

    boolean accessIsRomLinkPushing() {
        return romLinkPushing != 0;
    }

    int accessRomAttackStepAnimationCountdown() {
        return romAttackStepAnimationCountdown & 0xFF;
    }
}
