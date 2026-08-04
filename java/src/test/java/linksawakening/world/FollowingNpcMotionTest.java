package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FollowingNpcMotionTest {

    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;

    @Test
    void ghostUsesTheBank19FollowThresholdVectorPhaseAndBobTable() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(ENTITY_GHOST);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_GHOST, 0x10, 0x10,
            EntityStatus.ACTIVE, definition, 0);
        FollowingNpcMotion motion = new FollowingNpcMotion();
        motion.initialize(entity.slot(), entity.type());

        RoomEntity frame0 = entity;
        RoomEntity frame8 = entity;
        RoomEntity frame16 = entity;
        RoomEntity current = entity;
        for (int frame = 0; frame <= 16; frame++) {
            current = motion.advance(current, frame, 0x50, 0x50, null);
            if (frame == 0) {
                frame0 = current;
            } else if (frame == 8) {
                frame8 = current;
            } else if (frame == 16) {
                frame16 = current;
            }
        }

        assertEquals(0x0C, frame0.z());
        assertEquals(0x0D, frame8.z());
        assertEquals(0x0E, frame16.z());
        assertEquals(0, frame0.spriteVariant());
        assertEquals(1, frame16.spriteVariant());
        assertEquals(0x10, frame0.x());
        assertEquals(0x18, frame16.x());
    }

    @Test
    void ghostStaysPutInsideTheRomFollowWindow() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(ENTITY_GHOST);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_GHOST, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        FollowingNpcMotion motion = new FollowingNpcMotion();
        motion.initialize(entity.slot(), entity.type());

        RoomEntity result = motion.advance(entity, 16, 0x40, 0x30, null);

        assertEquals(0x40, result.x());
        assertEquals(0x40, result.y());
        assertEquals(5, result.spriteVariant());
    }

    @Test
    void roosterUsesFourDirectionDisplayPairsAndEightFrameVectorRefresh() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(ENTITY_ROOSTER);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_ROOSTER, 0x10, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        FollowingNpcMotion motion = new FollowingNpcMotion();
        motion.initialize(entity.slot(), entity.type());

        RoomEntity frame0 = entity;
        RoomEntity frame8 = entity;
        RoomEntity frame16 = entity;
        RoomEntity current = entity;
        for (int frame = 0; frame <= 16; frame++) {
            current = motion.advance(current, frame, 0x50, 0x40, null);
            if (frame == 0) {
                frame0 = current;
            } else if (frame == 8) {
                frame8 = current;
            } else if (frame == 16) {
                frame16 = current;
            }
        }

        assertEquals(0, frame0.spriteVariant());
        assertEquals(1, frame8.spriteVariant());
        assertEquals(0, frame16.spriteVariant());
        assertEquals(0x10, frame0.x());
        assertEquals(0x1C, frame16.x());
        assertEquals(0, frame16.z());
    }

    private static byte[] syntheticRom() {
        return new byte[0x100000];
    }
}
