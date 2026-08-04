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

    @Test
    void marinConsumesTheNextPositionAndZHistoryEntriesWhileRefreshingTheCurrentEntry() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(0xC1);
        RoomEntity entity = new RoomEntity(0, -1, 0xC1, 0x10, 0x20,
            EntityStatus.ACTIVE, definition, 0);
        LinkPositionHistory history = new LinkPositionHistory();
        history.fill(0x10, 0x20, 0x01, 0x02);
        history.write(1, 0x30, 0x40, 0x03, 0x01);
        FollowingNpcMotion motion = new FollowingNpcMotion();
        motion.initialize(entity.slot(), entity.type());

        RoomEntity result = motion.advance(entity, 0, 0x50, 0x60, 0x04, 0x00,
            0x00, history, null);

        assertEquals(0x30, result.x());
        assertEquals(0x40, result.y());
        assertEquals(0x03, result.z());
        assertEquals(0x02, result.spriteVariant());
        assertEquals(0x50, history.xAt(0));
        assertEquals(0x60, history.yAt(0));
        assertEquals(0x04, history.zAt(0));
        assertEquals(0x00, history.directionAt(0));
    }

    @Test
    void marinAdvancesItsHistoryIndicesAndUsesTheFollowingDirectionOnTheNextFrame() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(0xC1);
        RoomEntity entity = new RoomEntity(0, -1, 0xC1, 0x10, 0x20,
            EntityStatus.ACTIVE, definition, 0);
        LinkPositionHistory history = new LinkPositionHistory();
        history.fill(0x10, 0x20, 0x00, 0x03);
        history.write(1, 0x30, 0x40, 0x01, 0x00);
        history.write(2, 0x31, 0x41, 0x02, 0x03);
        FollowingNpcMotion motion = new FollowingNpcMotion();
        motion.initialize(entity.slot(), entity.type());

        RoomEntity first = motion.advance(entity, 0, 0x50, 0x60, 0x00, 0x00,
            0x00, history, null);
        RoomEntity second = motion.advance(first, 1, 0x51, 0x61, 0x00, 0x00,
            0x00, history, null);
        RoomEntity third = motion.advance(second, 2, 0x52, 0x62, 0x00, 0x00,
            0x00, history, null);

        assertEquals(0x31, third.x());
        assertEquals(0x41, third.y());
        assertEquals(0x06, third.spriteVariant());
        assertEquals(0x52, history.xAt(1));
        assertEquals(0x62, history.yAt(1));
    }

    private static byte[] syntheticRom() {
        return new byte[0x100000];
    }
}
