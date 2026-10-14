package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EnemyDropMotionTest {

    @Test
    void topDownLaunchUsesFixedPointZAndRomGravity() {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, false);
        RoomEntity entity = entity(0, 0x2E, 0x40, 0x50, 0);

        entity = motion.advance(entity, 0, 0, false);

        assertEquals(0x01, entity.z());
        assertEquals(0x16, motion.speedZ(0));
    }

    @Test
    void topDownLandingStopsInShallowWaterAndSideScrollBouncesOnDownCollision() {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, false);
        RoomEntity falling = entity(0, 0x2E, 0x40, 0x50, 0x80);
        falling = motion.bounce(falling, 0x02, false, false);
        assertEquals(0, falling.z());
        assertEquals(0, motion.speedZ(0));

        motion.initialize(0, true);
        RoomEntity side = entity(0, 0x2E, 0x40, 0x63, 0);
        for (int frame = 0; frame < 18; frame++) {
            side = motion.advance(side, frame, 0, true);
        }
        assertEquals(0x10, motion.speedY(0));
        int landingY = side.y();

        side = motion.bounce(side, 0x00, true, true);

        assertEquals((landingY & 0xF0) + 0x05, side.y());
        assertEquals(0xF7, motion.speedY(0));
    }

    @Test
    void scatteredTopDownRewardMovesDiagonallyAndSlowsAfterItsFirstBounce() {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, false, 0x20);
        motion.initializeAwayFromLink(0, 0x40, 0x50, 0x30, 0x40, 0x0C);
        RoomEntity drop = entity(0, 0x2E, 0x40, 0x50, 0);

        // $20 Z speed with gravity $02 lands on handler pass 34. Both planar
        // components move 34 * 12/16 pixels, retaining the fractional halves.
        for (int frame = 0; frame < 34; frame++) {
            drop = motion.advance(drop, frame, 0, false);
        }
        assertEquals(0x59, drop.x());
        assertEquals(0x69, drop.y());
        assertEquals(0xFD, drop.z());
        drop = motion.bounce(drop, 0, false, false);
        assertEquals(0, drop.z());
        assertEquals(0x11, motion.speedZ(0));
        assertEquals(6, motion.speedX(0));
        assertEquals(6, motion.speedY(0));
    }

    @ParameterizedTest
    @CsvSource({"0, 224, 12, 48, 64, 15, 6", "0, 224, 12, 80, 96, 15, 250",
        "0, 224, 1, 80, 96, 15, 0", "0, 244, 12, 48, 64, 0, 0",
        "2, 224, 12, 48, 64, 0, 0"})
    void landingAppliesRomPlanarDampingAndClearsBothAxesWhenSettled(
        int ground, int fallingSpeedZ, int planarLength, int linkX, int linkY,
        int nextZ, int nextPlanar) {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, false, fallingSpeedZ);
        motion.initializeAwayFromLink(0, 0x40, 0x50, linkX, linkY, planarLength);

        RoomEntity drop = motion.bounce(entity(0, 0x2E, 0x40, 0x50, 0xFF),
            ground, false, false);

        assertEquals(0, drop.z());
        assertEquals(nextZ, motion.speedZ(0));
        assertEquals(nextPlanar, motion.speedX(0));
        assertEquals(nextPlanar, motion.speedY(0));
    }

    @Test
    void sideViewRewardMovesHorizontallyAndHalvesXSpeedOnGroundBounce() {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, true);
        motion.initializeAwayFromLink(0, 0x40, 0x50, 0x30, 0x50, 0x0C);
        RoomEntity drop = entity(0, 0x2E, 0x40, 0x50, 0);
        for (int frame = 0; frame < 8; frame++) {
            drop = motion.advance(drop, frame, 0, true);
        }
        assertEquals(0x46, drop.x());
        assertEquals(0x10, motion.speedY(0));
        drop = motion.bounce(drop, 0, true, true);
        assertEquals(6, motion.speedX(0));
        assertEquals(0xF7, motion.speedY(0));

        for (int frame = 8; frame < 13; frame++) {
            drop = motion.advance(drop, frame, 0, true);
        }
        assertEquals(1, motion.speedY(0));
        motion.bounce(drop, 0, true, true);
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @ParameterizedTest
    @CsvSource({"1, 48, 7, 12", "1, 48, 8, 11", "1, 80, 8, 245",
        "0, 48, 8, 12", "2, 48, 8, 11", "3, 48, 8, 11"})
    void sideViewNonzeroGroundStatusSlowsXEveryEighthFrame(
        int ground, int linkX, int frame, int speedX) {
        EnemyDropMotion motion = new EnemyDropMotion();
        motion.initialize(0, true);
        motion.initializeAwayFromLink(0, 0x40, 0x50, linkX, 0x50, 0x0C);
        motion.advance(entity(0, 0x2E, 0x40, 0x50, 0), frame, ground, true);
        assertEquals(speedX, motion.speedX(0));
    }

    private static RoomEntity entity(int slot, int type, int x, int y, int z) {
        return new RoomEntity(slot, -1, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1, 0, 0, z);
    }
}
