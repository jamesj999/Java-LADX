package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BowWowMotionTest {

    private static final int ENTITY_BOW_WOW = 0x6D;

    @Test
    void setupUsesTheRomPrivateStateOffsetsAndSeedsBothTargets() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_BOW_WOW, 0x40, 0x50,
            EntityStatus.ACTIVE, definition, 0);
        BowWowMotion motion = new BowWowMotion();

        RoomEntity result = motion.advance(entity, 0, 0x60, 0x70, 0x00,
            () -> 0, null);

        assertEquals(0x44, result.x());
        assertEquals(0x58, result.y());
        assertEquals(0x00, result.z());
        assertEquals(1, motion.privateState4(0));
        assertEquals(0x44, motion.targetX(0));
        assertEquals(0x58, motion.targetY(0));
    }

    @Test
    void followingBranchTargetsLinkVisualYAndLoadsTheBankFiveSpeedTables() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_BOW_WOW, 0x40, 0x50,
            EntityStatus.ACTIVE, definition, 0);
        BowWowMotion motion = new BowWowMotion();
        IntSupplier random = sequence(0x00, 0x00, 0x00);

        entity = motion.advance(entity, 0, 0x60, 0x70, 0x00, random, null);
        entity = motion.advance(entity, 1, 0x60, 0x70, 0x08, random, null);
        entity = motion.advance(entity, 2, 0x60, 0x70, 0x08, random, null);

        assertEquals(0x60, motion.targetX(0));
        assertEquals(0x68, motion.targetY(0));
        assertEquals(1, motion.activeState(0));
        assertEquals(0x04, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));
    }

    @Test
    void movementUsesTheRomSixteenFrameFixedPointSpeedAndTargetCorrection() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity entity = new RoomEntity(0, -1, ENTITY_BOW_WOW, 0x40, 0x50,
            EntityStatus.ACTIVE, definition, 0);
        BowWowMotion motion = new BowWowMotion();
        IntSupplier random = sequence(0x00, 0x00, 0x00);

        entity = motion.advance(entity, 0, 0x60, 0x70, 0x00, random, null);
        entity = motion.advance(entity, 1, 0x60, 0x70, 0x00, random, null);
        entity = motion.advance(entity, 2, 0x60, 0x70, 0x00, random, null);
        int beforeMovementX = entity.x();
        int beforeMovementY = entity.y();
        entity = motion.advance(entity, 3, 0x60, 0x70, 0x00, random, null);

        assertEquals(beforeMovementX, entity.x());
        assertEquals(beforeMovementY - 1, entity.y());

        int beforeFarTargetX = entity.x();
        entity = motion.advance(entity, 4, 0xE0, 0x70, 0x00, random, null);
        assertEquals(beforeFarTargetX, entity.x());
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }
}
