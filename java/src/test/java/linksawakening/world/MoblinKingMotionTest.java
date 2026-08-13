package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MoblinKingMotionTest {
    @Test
    void sourceIntroFacesLinkThenOpensDialog191AfterTwentyFrames() {
        MoblinKingMotion motion = new MoblinKingMotion();
        RoomEntity king = entity(0x58, 0x58);

        MoblinKingMotion.Update setup = motion.advance(king, 0x30, 0x58,
            0, 0, 0, () -> 0, null, false);

        assertEquals(1, motion.state(0));
        assertEquals(0x20, setup.transitionCountdown());
        assertEquals(1, motion.direction(0));
        assertFalse(setup.openIntroDialog());

        MoblinKingMotion.Update intro = motion.advance(setup.entity(), 0x30, 0x58,
            0, 0, 0x20, () -> 0, null, false);

        assertEquals(2, motion.state(0));
        assertEquals(0x30, intro.slowTransitionCountdown());
        assertTrue(intro.openIntroDialog());
    }

    @Test
    void sourceAttackWindupLaunchesHorizontalMoblinArrowAtCountdownTwelve() {
        MoblinKingMotion motion = new MoblinKingMotion();
        RoomEntity king = enterAttackState(motion);
        motion.setPrivateCountdownsForTest(0, 0x0D, 0x20);

        MoblinKingMotion.Update update = motion.advance(king, 0x20, 0x58,
            0, 0x20, 0, () -> 0, null, false);

        assertNotNull(update.arrowRequest());
        assertEquals(0x50, update.arrowRequest().x());
        assertEquals(0x54, update.arrowRequest().y());
        assertEquals(0xE0, update.arrowRequest().speedX());
        assertEquals(1, update.arrowRequest().direction());
    }

    @Test
    void wallImpactReversesTheChargeAndStartsTheSourceStunJump() {
        MoblinKingMotion motion = new MoblinKingMotion();
        RoomEntity king = enterChargeState(motion);

        MoblinKingMotion.Update impact = motion.advance(king, 0x20, 0x58,
            0x22, 0, 0, () -> 0, (entity, direction, x, y) -> true, false);

        assertEquals(5, motion.state(0));
        assertEquals(0x60, impact.transitionCountdown());
        assertEquals(0x28, motion.speedZ(0));
        assertEquals(0x0A, motion.speedX(0));
        assertTrue(impact.strongBump());
        assertEquals(0x20, impact.screenShakeCountdown());
    }

    @Test
    void chargingIntoLinkUsesTheDedicatedStateEightDamageBranch() {
        MoblinKingMotion motion = new MoblinKingMotion();
        RoomEntity king = enterChargeState(motion);

        MoblinKingMotion.Update collision = motion.advance(king, 0x50, 0x58,
            0x22, 0, 0, () -> 0, null, true);

        assertEquals(9, motion.state(0));
        assertEquals(0x60, collision.transitionCountdown());
        assertEquals(0x08, collision.linkDamage());
        assertEquals(0x28, collision.ignoreLinkCollisionsCountdown());
        assertEquals(0x0B, collision.jingleId());
    }

    private static RoomEntity enterAttackState(MoblinKingMotion motion) {
        RoomEntity king = entity(0x58, 0x58);
        MoblinKingMotion.Update setup = motion.advance(king, 0x20, 0x58,
            0, 0, 0, () -> 0, null, false);
        return motion.advance(setup.entity(), 0x20, 0x58,
            0, 0, 0, () -> 0, null, false).entity();
    }

    private static RoomEntity enterChargeState(MoblinKingMotion motion) {
        RoomEntity king = enterAttackState(motion);
        motion.setStateForTest(0, 3, 1, 0x28, 0, 0);
        MoblinKingMotion.Update prepare = motion.advance(king, 0x20, 0x58,
            0, 0, 0, () -> 0, null, false);
        return prepare.entity();
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0xE4, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xE4), -1, 0, 0, 0);
    }
}
