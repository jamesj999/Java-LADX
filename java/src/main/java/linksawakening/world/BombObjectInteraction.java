package linksawakening.world;

/**
 * ROM coordinate tables used by {@code CheckForBombDestroyableObjectBasic}.
 *
 * <p>The bomb handler probes one cell per explosion frame.  The table index is
 * the post-decrement transition countdown minus {@code $0E}; the resulting
 * coordinates are aligned to the padded room-object buffer exactly as the
 * source's {@code & $F0} operations do.</p>
 */
final class BombObjectInteraction {

    static final int FIRST_OBJECT_COUNTDOWN = 0x0E;
    static final int LAST_OBJECT_COUNTDOWN = 0x16;

    private static final int[] BASIC_X = {
        -0x08, 0x08, 0x18,
        -0x08, 0x08, 0x18,
        -0x08, 0x08, 0x18
    };
    private static final int[] BASIC_Y = {
        -0x08, -0x08, -0x08,
         0x08,  0x08,  0x08,
         0x18,  0x18, 0x18
    };

    record Candidate(int objectLeft, int objectTop) {
        Candidate {
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }

        int location() {
            return (objectTop | (objectLeft >>> 4)) & 0xFF;
        }
    }

    private BombObjectInteraction() {
    }

    static Candidate basicCandidate(int bombX, int bombVisualY, int countdown) {
        return candidate(bombX, bombVisualY, countdown);
    }

    static Candidate puzzleCandidate(int bombX, int bombY, int countdown) {
        return candidate(bombX, bombY, countdown);
    }

    private static Candidate candidate(int bombX, int bombY, int countdown) {
        int tableIndex = countdown - FIRST_OBJECT_COUNTDOWN;
        if (tableIndex < 0 || tableIndex >= BASIC_X.length) {
            return null;
        }

        int objectLeft = (bombX + BASIC_X[tableIndex] - 0x08) & 0xFF;
        int objectTop = (bombY + BASIC_Y[tableIndex] - 0x10) & 0xFF;
        return new Candidate(objectLeft, objectTop);
    }
}
