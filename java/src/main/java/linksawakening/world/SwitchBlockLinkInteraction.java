package linksawakening.world;

/**
 * ROM-shaped Link collision rules for the two switchable block object IDs.
 *
 * <p>This is the bank-$02 {@code ApplyCollisionWithOceanOrSwitchBlock} and
 * {@code ApplyLinkGroundPhysics_Default} decision boundary. The source table
 * is {@code SwitchBlocksStateTable = [$00, $02]}.</p>
 */
public final class SwitchBlockLinkInteraction {
    public static final int PHYSICS_OCEAN_SWITCH_BLOCK = 0x04;
    public static final int LOWERED_BLOCK = 0xDB;
    public static final int RAISED_BLOCK = 0xDC;

    private static final int[] EXPECTED_STATE = {0x00, 0x02};

    private SwitchBlockLinkInteraction() {
    }

    public static boolean isSwitchBlock(int objectId) {
        return objectId == LOWERED_BLOCK || objectId == RAISED_BLOCK;
    }

    public static int expectedState(int objectId) {
        if (!isSwitchBlock(objectId)) {
            throw new IllegalArgumentException("Not a switch-block object: " + objectId);
        }
        return EXPECTED_STATE[objectId - LOWERED_BLOCK];
    }

    public static boolean blocks(int objectId, int physicsFlag, int switchBlocksState,
                                 boolean standingOnSwitchBlock) {
        validateState(switchBlocksState);
        if (physicsFlag != PHYSICS_OCEAN_SWITCH_BLOCK || !isSwitchBlock(objectId)) {
            return false;
        }
        return !standingOnSwitchBlock
            && ((switchBlocksState ^ expectedState(objectId)) != 0);
    }

    public static boolean marksStanding(int objectId, int physicsFlag, int switchBlocksState) {
        validateState(switchBlocksState);
        return physicsFlag == PHYSICS_OCEAN_SWITCH_BLOCK
            && isSwitchBlock(objectId)
            && ((switchBlocksState ^ expectedState(objectId)) != 0);
    }

    private static void validateState(int switchBlocksState) {
        if (switchBlocksState != 0x00 && switchBlocksState != 0x02) {
            throw new IllegalArgumentException("Switch-block state must be $00 or $02: "
                + switchBlocksState);
        }
    }
}
