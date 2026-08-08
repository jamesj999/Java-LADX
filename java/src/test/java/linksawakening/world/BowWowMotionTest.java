package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void followingScanUsesTheRomCandidateFiltersAndLoadsTheTargetVector() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity bowWow = new RoomEntity(5, -1, ENTITY_BOW_WOW, 0x20, 0x20,
            EntityStatus.ACTIVE, definition, 0);
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(5, bowWow);
        slots.set(6, new RoomEntity(6, -1, 0x09, 0x50, 0x60,
            EntityStatus.ACTIVE, definition, 1));
        slots.set(7, new RoomEntity(7, -1, 0x09, 0x50, 0x60,
            EntityStatus.DYING, definition, 0));
        slots.set(8, new RoomEntity(8, -1, 0x0B, 0x54, 0x68,
            EntityStatus.ACTIVE, definition, 0));
        slots.set(10, new RoomEntity(10, -1, 0x09, 0x54, 0x68,
            EntityStatus.ACTIVE, definition, 0));

        BowWowMotion motion = new BowWowMotion();
        RoomEntity initialized = motion.advance(bowWow, 0, 0x00, 0x00, 0x00,
            () -> 0, null);
        slots.set(5, initialized);

        BowWowMotion.Update update = motion.advanceWithTargetScan(initialized, 1,
            0x60, 0x70, 0x00, () -> 0, null, slots, type -> type == 0x09);

        assertEquals(10, update.targetSlot());
        assertEquals(4, motion.activeState(5));
        assertEquals(0x28, motion.transitionCountdown(5));
        assertEquals(0x24, motion.speedX(5));
        assertEquals(0x30, motion.speedY(5));
        assertEquals(0x10, motion.speedZ(5));
    }

    @Test
    void terminalFollowingPhaseReportsTheRomTargetContactWindow() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity bowWow = new RoomEntity(5, -1, ENTITY_BOW_WOW, 0x40, 0x50,
            EntityStatus.ACTIVE, definition, 0);
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(5, bowWow);
        slots.set(6, new RoomEntity(6, 0, 0x09, 0x44, 0x58,
            EntityStatus.ACTIVE, definition, 0));

        BowWowMotion motion = new BowWowMotion();
        RoomEntity initialized = motion.advance(bowWow, 0, 0x44, 0x58, 0x00,
            () -> 0, null);
        slots.set(5, initialized);
        BowWowMotion.Update acquired = motion.advanceWithTargetScan(initialized, 1,
            0x44, 0x58, 0x00, () -> 0, null, slots, type -> type == 0x09);
        motion.setTransitionCountdownForTest(5, 1);

        BowWowMotion.Update terminal = motion.advanceWithTargetScan(acquired.entity(), 2,
            0x44, 0x58, 0x00, () -> 0, null, slots, type -> type == 0x09);

        assertNotNull(terminal.targetContact());
        assertEquals(6, terminal.targetContact().slot());
        assertEquals(0x09, terminal.targetContact().type());
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }
}
