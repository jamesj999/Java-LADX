package linksawakening.world.room;

import java.util.Optional;
import linksawakening.state.LinkMotionState;

/** Motion inputs retain the ROM dispatch byte, including unknown numeric gate values. */
public record LinkMotion(boolean airborne, int rawState, boolean shield, int invincibility, int speedX, int speedY) {
    /** Explicit absence for values outside the known ROM dispatch table. */
    public Optional<LinkMotionState> knownState() {
        return Optional.ofNullable(stateForRomByte(rawState));
    }
    /** Legacy nullable classification; absent for unknown ROM dispatch bytes. */
    public LinkMotionState state() {
        return stateForRomByte(rawState);
    }
    /** Returns null for a genuinely unknown ROM dispatch byte. */
    public static LinkMotionState stateForRomByte(int raw) {
        return raw >= 0 && raw <= LinkMotionState.UNKNOWN_0A.romValue() ? LinkMotionState.fromRom(raw) : null;
    }
}
