package linksawakening.world;

/**
 * Source-shaped countdown branch for KeyDropPointEntityHandler in bank $03.
 *
 * <p>The generic entity timer has already decremented the transition byte when
 * this helper runs. The handler only gives the item/dialog when that resulting
 * value is $10; all other nonzero values keep the item above Link.</p>
 */
final class KeyDropPointMotion {
    static final int TRANSITION_COUNTDOWN = 0x68;
    static final int DIALOG_TABLE = 0;

    private KeyDropPointMotion() {
    }

    record Update(int nextTransitionCountdown, boolean holdAboveLink,
                  int dialogLowId, int rewardItemType) {
        Update {
            if (nextTransitionCountdown < 0 || nextTransitionCountdown > 0xFF) {
                throw new IllegalArgumentException("Transition countdown must be a byte");
            }
            if (dialogLowId < -1 || dialogLowId > 0xFF) {
                throw new IllegalArgumentException("Dialog id must be -1 or a byte");
            }
            if (rewardItemType < -1 || rewardItemType > 0xFF) {
                throw new IllegalArgumentException("Reward item must be -1 or a byte");
            }
        }
    }

    static Update advance(int transitionCountdown, int spriteVariant, boolean hookshotRoom) {
        if (transitionCountdown == 0) {
            return new Update(0, false, -1, -1);
        }
        if (transitionCountdown == 0x10) {
            if (hookshotRoom) {
                return new Update(0x0F, true, 0x93, ChestContentsTable.CHEST_HOOKSHOT);
            }
            int dialog = switch (spriteVariant) {
                case 1 -> 0x00;
                case 2 -> 0xA3;
                case 3 -> 0xA4;
                case 4 -> 0xA5;
                default -> 0x00;
            };
            int reward = switch (spriteVariant) {
                case 1 -> ChestContentsTable.CHEST_TAIL_KEY;
                case 2 -> ChestContentsTable.CHEST_ANGLER_KEY;
                case 3 -> ChestContentsTable.CHEST_FACE_KEY;
                case 4 -> ChestContentsTable.CHEST_BIRD_KEY;
                default -> -1;
            };
            return new Update(0x0F, true, dialog, reward);
        }
        return new Update(transitionCountdown, true, -1, -1);
    }
}
