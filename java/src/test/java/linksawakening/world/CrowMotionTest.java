package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrowMotionTest {

    @Test
    void waitsForLinkThenStartsTheRomTakeoffSequence() {
        CrowMotion motion = new CrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update triggered = motion.advance(entity, 0, 0x44, 0x50);

        assertEquals(1, motion.state(0));
        assertEquals(0x22, triggered.transitionCountdown());
        assertEquals(0x12, triggered.physicsFlags());
        assertEquals(0x4C, triggered.entity().y());
        assertEquals(2, triggered.entity().spriteVariant());
        assertFalse(triggered.unloaded());
        assertFalse(triggered.boomerangSound());
    }

    @Test
    void countdownClimbThenHomingAndOutwardFlightUseRomFixedPointSpeeds() {
        CrowMotion motion = new CrowMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update update = motion.advance(entity, 0, 0x44, 0x50);
        update = motion.advance(update.entity(), 0x21, 0x44, 0x50);
        assertEquals(0x08, motion.speedZ(0));
        assertTrue(update.boomerangSound());
        assertEquals(0, update.entity().z());

        update = motion.advance(update.entity(), 0, 0x44, 0x50);
        assertEquals(2, motion.state(0));
        assertEquals(0x30, update.transitionCountdown());

        update = motion.advance(update.entity(), 0x2F, 0x60, 0x50);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0x40, update.entity().x());
        assertTrue(update.boomerangSound());

        update = motion.advance(update.entity(), 0x2E, 0x60, 0x50);
        assertEquals(0x01, motion.speedX(0));
        assertEquals(0x01, motion.speedY(0));
        assertEquals(0x40, update.entity().x());
        assertTrue(update.boomerangSound());

        motion.setStateForTest(0, 3);
        motion.setSpeedForTest(0, 0x20, 0, 0);
        RoomEntity outwardStart = new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE,
            0xA6, 0x50, EntityStatus.ACTIVE, definition(), 0);
        CrowMotion.Update crossing = motion.advance(outwardStart, 0, 0x40, 0x50);
        assertFalse(crossing.unloaded());
        CrowMotion.Update unloaded = motion.advance(crossing.entity(), 0, 0x40, 0x50);
        assertTrue(unloaded.unloaded());
    }

    @Test
    void vectorSteeringApproachesTargetByOneRomSpeedUnit() {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, 2);
        motion.setSpeedForTest(0, 0, 0, 0);
        RoomEntity entity = entity(0x40, 0x50, 0);

        CrowMotion.Update update = motion.advance(entity, 0x02, 0x60, 0x60);

        assertEquals(1, motion.speedX(0));
        assertEquals(1, motion.speedY(0));
        assertEquals(0x40, update.entity().x());
        assertEquals(0x50, update.entity().y());
    }

    @ParameterizedTest
    @CsvSource({"2, 1, 1", "3, 255, 255"})
    void airbornePursuitAndRetreatSteerFromVisualHeight(int state, int speedX, int speedY) {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, state);
        RoomEntity airborne = new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE,
            0x40, 0x50, EntityStatus.ACTIVE, definition(), 0, 0, 0, 0x10);

        // Link is below the crow's visual Y ($40), but above its ground Y ($50).
        // Both flight states call bank $03 GetVectorTowardsLink via $3BB5.
        motion.advance(airborne, 2, 4, 0x60, 0x48);

        assertEquals(speedX, motion.speedX(0));
        assertEquals(speedY, motion.speedY(0));
    }

    @ParameterizedTest
    @CsvSource({
        "167, 80, 16, 16, 0, false", "168, 80, 16, 240, 0, true",
        "0, 80, 16, 240, 0, false", "255, 80, 16, 16, 0, true",
        "64, 151, 16, 0, 16, false", "64, 152, 16, 0, 240, true",
        "64, 16, 16, 0, 240, false", "64, 15, 16, 0, 16, true"
    })
    void escapeBoundsUseTheHandlerEntryPosition(int x, int y, int z,
                                               int speedX, int speedY, boolean unload) {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, 3);
        motion.setSpeedForTest(0, speedX, speedY, 0);
        RoomEntity crow = new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), 0, 0, 0, z);

        // Frame 1 skips steering. Each speed moves one pixel across a boundary.
        // $5E54 reads cached hActiveEntityPosX / hActiveEntityVisualPosY.
        CrowMotion.Update update = motion.advance(crow, 0, 1, 0x40, 0x50);

        assertEquals((x + (byte) speedX / 16) & 0xFF, update.entity().x());
        assertEquals((y + (byte) speedY / 16) & 0xFF, update.entity().y());
        assertEquals(unload, update.unloaded());
    }

    @Test
    void perchedFacingDoesNotPersistTheTemporaryMirroredVariant() {
        CrowMotion motion = new CrowMotion();
        CrowMotion.Update first = motion.advance(entity(0x40, 0x50, 0), 0, 0, 0xE0);
        assertEquals(2, first.entity().spriteVariant());
        CrowMotion.Update second = motion.advance(first.entity(), 0, 0, 0xE0);
        assertEquals(0, second.entity().spriteVariant());
        assertEquals(0, motion.state(0));
    }

    @Test
    void climbSelectsNextWingFrameFromAltitudeAfterMovement() {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, 1);
        RoomEntity crow = new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE, 0x40, 0x50,
            EntityStatus.ACTIVE, definition(), 0, 0, 0, 7);

        CrowMotion.Update first = motion.advance(crow, 0x20, 0x40, 0x50);
        assertEquals(7, first.entity().z());
        assertEquals(2, first.entity().spriteVariant());
        CrowMotion.Update second = motion.advance(first.entity(), 0x1F, 0x40, 0x50);
        assertEquals(8, second.entity().z());
        assertEquals(2, second.entity().spriteVariant());
        CrowMotion.Update third = motion.advance(second.entity(), 0x1E, 0x40, 0x50);
        assertEquals(3, third.entity().spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"2, 16, 3", "3, 16, 3", "2, 0, 2", "3, 0, 2"})
    void flightSelectsNextWingFrameFromTheMovementHelpersFinalByte(
        int state, int speedY, int nextVariant) {
        CrowMotion motion = new CrowMotion();
        motion.setStateForTest(0, state);
        motion.setSpeedForTest(0, 0, speedY, 0);
        // When Y moves, HL points at updated Y ($49). A zero Y speed returns
        // early with HL still at the speed byte (zero). Neither is the timer.
        CrowMotion.Update first = motion.advance(entity(0x40, 0x48, 0), 3, 1, 0, 0);
        assertEquals(2, first.entity().spriteVariant());
        CrowMotion.Update second = motion.advance(first.entity(), 3, 1, 0, 0);
        assertEquals(nextVariant, second.entity().spriteVariant());
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), variant);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(CrowMotion.ENTITY_TYPE, 0x06, 0x5C89,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second)));
    }
}
