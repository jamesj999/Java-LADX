package linksawakening.world.entity.motion;

/** Pure byte-state transitions from bank 04:4F65–4FAD. Countdown ticking and
 * fixed-point Z integration belong to the caller, in ROM execution order. */
public final class SlimeEyeHalfMotion {
    private SlimeEyeHalfMotion() {}

    public record Update(
            int state, int countdown, int speedZ, int spriteVariant, int privateState4, int x, int y, int jingle) {}

    /** Apply state 2 after AddEntityZSpeedToPos, using its resulting byte Z. */
    public static Update rise(int integratedZ, int countdown, int spriteVariant, int privateState4, int x, int y) {
        return new Update(
                (integratedZ & 0xFF) < 0x70 ? 2 : 3,
                (integratedZ & 0xFF) < 0x70 ? countdown : 0x30,
                0x60,
                spriteVariant,
                privateState4,
                x,
                y,
                -1);
    }

    /** State 3 hides the half, then drops it at Link's current coordinates. */
    public static Update waitAbove(int countdown, int speedZ, int privateState4, int x, int y, int linkX, int linkY) {
        if (countdown != 0) {
            return new Update(3, countdown, speedZ, 0xFF, privateState4, x, y, -1);
        }
        return new Update(1, 0x18, 0xC0, 0xFF, 1, linkX & 0xFF, linkY & 0xFF, 0x08);
    }
}
