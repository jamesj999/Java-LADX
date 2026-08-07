package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class BombObjectInteractionTest {

    @Test
    void basicCandidateFollowsTheRomNineCellExplosionSweep() {
        int[] expectedLocations = {
            0x33, 0x34, 0x35,
            0x43, 0x44, 0x45,
            0x53, 0x54, 0x55
        };

        for (int index = 0; index < expectedLocations.length; index++) {
            int countdown = BombObjectInteraction.FIRST_OBJECT_COUNTDOWN + index;
            BombObjectInteraction.Candidate candidate = BombObjectInteraction.basicCandidate(
                0x48, 0x4F, countdown);

            assertEquals(expectedLocations[index], candidate.location(),
                () -> "countdown=$" + Integer.toHexString(countdown));
            assertEquals((expectedLocations[index] & 0x0F) << 4,
                candidate.objectLeft());
            assertEquals(expectedLocations[index] & 0xF0, candidate.objectTop());
        }
    }

    @Test
    void basicCandidateIsAbsentOutsideTheSourceObjectWindow() {
        assertNull(BombObjectInteraction.basicCandidate(0x48, 0x4F, 0x0D));
        assertNull(BombObjectInteraction.basicCandidate(0x48, 0x4F, 0x17));
    }

    @Test
    void puzzleCandidateUsesRawBombYInsteadOfVisualY() {
        BombObjectInteraction.Candidate candidate = BombObjectInteraction.puzzleCandidate(
            0x20, 0x18, 0x0E);

        assertEquals(0x10, candidate.objectLeft());
        assertEquals(0x00, candidate.objectTop());
        assertEquals(0x01, candidate.location());

        BombObjectInteraction.Candidate visualCandidate = BombObjectInteraction.basicCandidate(
            0x20, 0x17, 0x0E);
        assertEquals(0xF0, visualCandidate.objectTop());
    }
}
