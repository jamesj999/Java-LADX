package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;

import org.junit.jupiter.api.Test;

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

    private static RoomEntity entity(int slot, int type, int x, int y, int z) {
        return new RoomEntity(slot, -1, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1, 0, 0, z);
    }
}
