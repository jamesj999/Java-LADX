package linksawakening.world.entity.motion;

/** Test-only access to package-private motion setup and constants. */
public final class MotionTestAccess {
    private MotionTestAccess() {}

    public static final int HinoxMotion_ENTITY_BOMB = HinoxMotion.ENTITY_BOMB;
    public static final int MagicRodFireballMotion_ENTITY_TYPE = MagicRodFireballMotion.ENTITY_TYPE;
    public static final int RoosterMotion_ENTITY_TYPE = RoosterMotion.ENTITY_TYPE;
    public static final int ThreeOfAKindMotion_ENTITY_TYPE = ThreeOfAKindMotion.ENTITY_TYPE;
    public static final int MagicRodFireballMotion_FIRE_TRANSITION_COUNTDOWN =
            MagicRodFireballMotion.FIRE_TRANSITION_COUNTDOWN;
    public static final int HinoxMotion_STATE_CHARGE = HinoxMotion.STATE_CHARGE;
    public static final int HinoxMotion_STATE_INITIAL = HinoxMotion.STATE_INITIAL;
    public static final int HinoxMotion_STATE_WANDER = HinoxMotion.STATE_WANDER;

    public static void setStateForTest(
            DogMotion motion, int slot, int newState, int newDirection, int newSpeedX, int newSpeedY, int newSpeedZ) {
        MotionTestFixture.setStateForTest(motion, slot, newState, newDirection, newSpeedX, newSpeedY, newSpeedZ);
    }

    public static void setStateForTest(
            WitchRatMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newDirection,
            int newSpeedX,
            int newSpeedY,
            int newSpeedZ) {
        MotionTestFixture.setStateForTest(
                motion, slot, newState, transitionCountdown, newDirection, newSpeedX, newSpeedY, newSpeedZ);
    }

    public static void initializeStateOne(BuzzBlobMotion motion, int slot) {
        motion.initializeStateOne(slot);
    }

    public static int frameVariant(int frameCounter) {
        return MagicRodFireballMotion.frameVariant(frameCounter);
    }
}
