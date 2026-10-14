package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BowWowMotionTest {

    private static final int ENTITY_BOW_WOW = 0x6D;

    @ParameterizedTest
    @CsvSource({"0, 15, 0", "1, 0, 15"})
    void targetScanSkipsItsOwnSlotAndContinuesToTheEnd(int random, int bowWowSlot, int targetSlot) {
        BowWowMotion motion = new BowWowMotion();
        RoomEntity bowWow = bowWowEntity(bowWowSlot, 0x40, 0x50, 0);
        RoomEntity current = motion.advance(bowWow, 0, 0x44, 0x58, 0, () -> random, null);
        List<RoomEntity> slots = emptySlots();
        slots.set(bowWowSlot, current);
        slots.set(targetSlot, targetEntity(targetSlot, 0x54, 0x58));
        BowWowMotion.Update update = motion.advanceWithTargetScan(current, 1,
            0x44, 0x58, 0, () -> random, null, slots, type -> true);
        assertEquals(targetSlot, update.targetSlot());
        assertEquals(4, motion.activeState(bowWowSlot));
    }

    @ParameterizedTest
    @CsvSource({"48, 0, -30, 0", "-48, 0, 29, 0", "0, 32, 0, -30", "0, -32, 0, 29"})
    void leashRestorationEndsTheLungeImmediately(
        int targetDx, int targetDy, int linkDx, int linkDy) {
        BowWowMotion motion = new BowWowMotion();
        List<RoomEntity> slots = emptySlots();
        RoomEntity current = acquireTarget(motion, slots, 0x44 + targetDx, 0x58 + targetDy);
        BowWowMotion.Update update = motion.advanceWithTargetScan(current, 2,
            0x44 + linkDx, 0x58 + linkDy, 0, () -> 0, null, slots, type -> true);
        assertEquals(3, motion.activeState(5));
        assertEquals(0x10, motion.transitionCountdown(5));
        assertEquals(0, motion.speedX(5));
        assertEquals(0, motion.speedY(5));
        assertEquals(0x44, update.entity().x());
        assertEquals(0x58, update.entity().y());
    }

    @ParameterizedTest
    @CsvSource({"15, 0, false", "-11, 0, true", "0, 16, true"})
    void biteWindowUsesPositionAndHeightCachedBeforeMovement(int targetDx, int targetDy, boolean bite) {
        BowWowMotion motion = new BowWowMotion();
        List<RoomEntity> slots = emptySlots();
        RoomEntity current = acquireTarget(motion, slots, 0x74, 0x58);
        slots.set(6, targetEntity(6, 0x44 + targetDx, 0x58 + targetDy));
        BowWowMotion.Update update = motion.advanceWithTargetScan(current, 2,
            0x44, 0x58, 0, () -> 0, null, slots, type -> true);
        assertEquals(0x47, update.entity().x());
        assertEquals(1, update.entity().z());
        if (bite) {
            assertNotNull(update.targetContact());
        } else {
            assertNull(update.targetContact());
        }
    }

    @Test
    void acquiredLungeUsesTheHeightAfterGravityHasRun() {
        BowWowMotion motion = new BowWowMotion();
        List<RoomEntity> slots = emptySlots();
        RoomEntity current = motion.advance(bowWowEntity(5, 0x40, 0x50, 8), 0,
            0x64, 0x58, 0, () -> 0, null);
        current = motion.advanceWithTargetScan(current, 1,
            0x64, 0x58, 0, () -> 0, null, slots, type -> true).entity();
        assertEquals(8, current.z());
        motion.setTransitionCountdown(5, 1);
        slots.set(5, current);
        slots.set(6, targetEntity(6, 0x64, 0x58));
        BowWowMotion.Update acquired = motion.advanceWithTargetScan(current, 2,
            0x64, 0x58, 0, () -> 0, null, slots, type -> true);
        assertEquals(7, acquired.entity().z());
        assertEquals(0x30, motion.speedX(5));
        assertEquals(0x0A, motion.speedY(5));
    }

    private static RoomEntity acquireTarget(BowWowMotion motion, List<RoomEntity> slots,
                                             int targetX, int targetY) {
        RoomEntity current = motion.advance(bowWowEntity(5, 0x40, 0x50, 0), 0,
            targetX, targetY, 0, () -> 0, null);
        slots.set(5, current);
        slots.set(6, targetEntity(6, targetX, targetY));
        BowWowMotion.Update acquired = motion.advanceWithTargetScan(current, 1,
            targetX, targetY, 0, () -> 0, null, slots, type -> true);
        assertEquals(4, motion.activeState(5));
        return acquired.entity();
    }

    private static List<RoomEntity> emptySlots() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return slots;
    }

    private static RoomEntity bowWowEntity(int slot, int x, int y, int z) {
        return new RoomEntity(slot, -1, ENTITY_BOW_WOW, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(ENTITY_BOW_WOW), 0, 0, 0, z);
    }

    private static RoomEntity targetEntity(int slot, int x, int y) {
        return new RoomEntity(slot, 0, 0x09, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x09), 0);
    }

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

    @Test
    void movingAttackChecksTheRomTargetContactWindowEveryFrame() {
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
        RoomEntity initialized = motion.advance(bowWow, 0, 0x44, 0x58, 0,
            () -> 0, null);
        slots.set(5, initialized);
        BowWowMotion.Update acquired = motion.advanceWithTargetScan(initialized, 1,
            0x44, 0x58, 0, () -> 0, null, slots, type -> type == 0x09);

        BowWowMotion.Update moving = motion.advanceWithTargetScan(acquired.entity(), 2,
            0x44, 0x58, 0, () -> 0, null, slots, type -> type == 0x09);

        assertEquals(0x27, motion.transitionCountdown(5));
        assertNotNull(moving.targetContact());
        assertEquals(6, moving.targetContact().slot());
    }

    @Test
    void bowWowMovementSkipsTheRomBackgroundWallCollisionPath() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity bowWow = new RoomEntity(5, -1, ENTITY_BOW_WOW, 0x20, 0x20,
            EntityStatus.ACTIVE, definition, 0);
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(5, bowWow);
        slots.set(6, new RoomEntity(6, 0, 0x09, 0x40, 0x44,
            EntityStatus.ACTIVE, definition, 0));

        BowWowMotion motion = new BowWowMotion();
        RoomEntity initialized = motion.advance(bowWow, 0, 0x60, 0x70, 0,
            () -> 0, null);
        slots.set(5, initialized);
        BowWowMotion.Update acquired = motion.advanceWithTargetScan(initialized, 1,
            0x40, 0x44, 0, () -> 0, null, slots, type -> type == 0x09);
        int beforeX = acquired.entity().x();
        int beforeY = acquired.entity().y();

        BowWowMotion.Update moved = motion.advanceWithTargetScan(acquired.entity(), 2,
            0x40, 0x44, 0, () -> 0,
            (entity, direction, nextX, nextY) -> true,
            slots, type -> type == 0x09);

        assertTrue(moved.entity().x() != beforeX || moved.entity().y() != beforeY);
    }

    @Test
    void completedFollowingAttackReturnsToRomTargetSelectionState() {
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
        RoomEntity current = motion.advance(bowWow, 0, 0x44, 0x58, 0,
            () -> 0, null);
        slots.set(5, current);
        current = motion.advanceWithTargetScan(current, 1,
            0x44, 0x58, 0, () -> 0, null, slots, type -> type == 0x09).entity();
        motion.setTransitionCountdownForTest(5, 1);
        current = motion.advanceWithTargetScan(current, 2,
            0x44, 0x58, 0, () -> 0, null, slots, type -> type == 0x09).entity();
        slots.set(6, RoomEntity.disabled(6));

        for (int frame = 3; frame < 19; frame++) {
            current = motion.advanceWithTargetScan(current, frame,
                0x44, 0x58, 0, () -> 0, null, slots, type -> type == 0x09).entity();
        }

        assertEquals(0, motion.activeState(5));
        assertEquals(0x10, motion.transitionCountdown(5));
    }

    @Test
    void completedWanderReturnsToRomTargetSelectionState() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(new byte[0x100000])
            .forFollowerEntityType(ENTITY_BOW_WOW);
        RoomEntity current = new RoomEntity(5, -1, ENTITY_BOW_WOW, 0x40, 0x50,
            EntityStatus.ACTIVE, definition, 0);
        BowWowMotion motion = new BowWowMotion();
        IntSupplier random = sequence(0, 0, 0);

        current = motion.advance(current, 0, 0x44, 0x58, 0, random, null);
        current = motion.advance(current, 1, 0x44, 0x58, 0, random, null);
        current = motion.advance(current, 2, 0x44, 0x58, 0, random, null);
        assertEquals(1, motion.activeState(5));

        for (int frame = 3; frame < 35; frame++) {
            current = motion.advance(current, frame, 0x44, 0x58, 0, random, null);
        }

        assertEquals(0, motion.activeState(5));
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }
}
