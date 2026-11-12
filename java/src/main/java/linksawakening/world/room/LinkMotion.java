package linksawakening.world.room;

import linksawakening.state.LinkMotionState;

/** Known Link motion dispatch states plus an unmodified raw byte for unknown ROM gates. */
public record LinkMotion(
        boolean airborne,
        LinkMotionState state,
        boolean shield,
        int invincibility,
        int speedX,
        int speedY,
        int rawState) {
    public LinkMotion {
        state = stateForRomByte(rawState);
    }

    public LinkMotion(
            boolean airborne, LinkMotionState state, boolean shield, int invincibility, int speedX, int speedY) {
        this(airborne, state, shield, invincibility, speedX, speedY, state.romValue());
    }

    /** Unknown raw dispatch bytes retain their numeric gating behavior without inventing an enum. */
    public static LinkMotionState stateForRomByte(int raw) {
        return raw >= 0 && raw <= LinkMotionState.UNKNOWN_0A.romValue() ? LinkMotionState.fromRom(raw) : null;
    }
}
