package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ThreeOfAKindMotionTest {
    @Test
    void hitFreezesTheCurrentCardFaceForTheRomFortyFrameWindow() {
        ThreeOfAKindMotion motion = new ThreeOfAKindMotion();
        RoomEntity card = card(0);

        ThreeOfAKindMotion.Update update = motion.advance(
            card, 1, 5, 9, () -> 0, null);

        assertEquals(2, motion.state(0));
        assertEquals(0x40, update.transitionCountdown());
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @Test
    void threeSettledMatchingFacesSolveWhileDifferentFacesRestart() {
        ThreeOfAKindMotion motion = new ThreeOfAKindMotion();
        List<RoomEntity> cards = List.of(card(0), card(1), card(2));
        int[] countdowns = new int[EntityRoomLoader.MAX_ENTITIES];
        motion.setSettledForTest(0, 1);
        motion.setSettledForTest(1, 1);
        motion.setSettledForTest(2, 2);

        assertEquals(ThreeOfAKindMotion.PuzzleResult.MISMATCH,
            motion.resolvePuzzle(cards, countdowns));
        assertEquals(0, motion.state(0));
        assertEquals(0, motion.state(1));
        assertEquals(0, motion.state(2));

        motion.setSettledForTest(0, 0);
        motion.setSettledForTest(1, 0);
        motion.setSettledForTest(2, 0);
        assertEquals(ThreeOfAKindMotion.PuzzleResult.MATCH,
            motion.resolvePuzzle(cards, countdowns));
        assertEquals(0x2D, motion.matchedDrop(0));
        assertEquals(0x02, motion.matchedJingle(0));

        motion.setSettledForTest(0, 2);
        motion.setSettledForTest(1, 2);
        motion.setSettledForTest(2, 2);
        assertEquals(0x1D, motion.matchedJingle(0));
    }

    private static RoomEntity card(int slot) {
        return new RoomEntity(slot, slot, ThreeOfAKindMotion.ENTITY_TYPE,
            0x40 + slot * 8, 0x50, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(ThreeOfAKindMotion.ENTITY_TYPE), 0);
    }
}
