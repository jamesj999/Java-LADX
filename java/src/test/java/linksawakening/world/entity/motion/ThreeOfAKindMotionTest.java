package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import org.junit.jupiter.api.Test;

final class ThreeOfAKindMotionTest {
    @Test
    void hitFreezesTheCurrentCardFaceForTheRomFortyFrameWindow() {
        ThreeOfAKindMotion motion = new ThreeOfAKindMotion();
        RoomEntity card = card(0);

        ThreeOfAKindMotion.Update update = motion.advance(card, 1, 5, 9, () -> 0, null);

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
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 0, 1);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 1, 1);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 2, 2);

        assertEquals(ThreeOfAKindMotion.PuzzleResult.MISMATCH, motion.resolvePuzzle(cards, countdowns));
        assertEquals(0, motion.state(0));
        assertEquals(0, motion.state(1));
        assertEquals(0, motion.state(2));

        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 0, 0);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 1, 0);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 2, 0);
        assertEquals(ThreeOfAKindMotion.PuzzleResult.MATCH, motion.resolvePuzzle(cards, countdowns));
        assertEquals(0x2D, motion.matchedDrop(0));
        assertEquals(0x02, motion.matchedJingle(0));

        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 0, 2);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 1, 2);
        linksawakening.world.entity.motion.MotionTestFixture.setSettledForTest(motion, 2, 2);
        assertEquals(0x1D, motion.matchedJingle(0));
    }

    private static RoomEntity card(int slot) {
        return new linksawakening.world.entity.RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(ThreeOfAKindMotion.ENTITY_TYPE)
                .at(0x40 + slot * 8, 0x50)
                .spriteDefinition(EntitySpriteDefinition.unsupported(ThreeOfAKindMotion.ENTITY_TYPE))
                .build();
    }
}
