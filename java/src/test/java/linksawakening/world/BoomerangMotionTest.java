package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BoomerangMotionTest {

    @Test
    void spawnUsesTheSharedPlayerProjectileTables() {
        assertSpawnData(0, 0x20, 0x00);
        assertSpawnData(1, 0xE0, 0x00);
        assertSpawnData(2, 0x00, 0xE0);
        assertSpawnData(3, 0x00, 0x20);
    }

    @Test
    void launchAppliesTheRomCountdownAndOptionalDiagonalPressedMaskSpeeds() {
        BoomerangMotion motion = new BoomerangMotion();

        motion.initializeSpawn(0, 0, 0x05); // right + up

        assertEquals(0x18, motion.speedX(0));
        assertEquals(0xE8, motion.speedY(0));
        assertEquals(0x28, motion.transitionCountdown(0));
        assertEquals(0, motion.state(0));
    }

    @Test
    void positionUsesTheRomSixteenthPixelAccumulator() {
        BoomerangMotion motion = new BoomerangMotion();
        motion.initializeSpawn(0, 0, 0);
        RoomEntity entity = entity(0x10, 0x20);

        RoomEntity first = motion.advancePosition(entity);
        RoomEntity second = motion.advancePosition(first);

        assertEquals(0x12, first.x());
        assertEquals(0x14, second.x());
        assertEquals(0x20, first.y());
    }

    @Test
    void vectorTowardsLinkMatchesTheRomInfinityNormDivision() {
        BoomerangMotion motion = new BoomerangMotion();
        motion.initializeSpawn(0, 0, 0);
        RoomEntity entity = entity(0x20, 0x30);

        motion.setVectorTowardsLink(0, entity, 0x30, 0x38, 8);

        assertEquals(0x08, motion.speedX(0));
        assertEquals(0x04, motion.speedY(0));
    }

    @Test
    void returningVectorUsesTheBoomerangHeight() {
        BoomerangMotion motion = new BoomerangMotion();
        RoomEntity airborne = new RoomEntity(0, -1, BoomerangMotion.ENTITY_TYPE,
            0x40, 0x40, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(BoomerangMotion.ENTITY_TYPE), 0, 0, 0, 8);

        motion.setVectorTowardsLink(0, airborne, 0x50, 0x40, 8);

        assertEquals(8, motion.speedX(0));
        assertEquals(4, motion.speedY(0));
    }

    @Test
    void countdownAndStateAreIndependentRomHandlerFields() {
        BoomerangMotion motion = new BoomerangMotion();
        motion.initializeSpawn(0, 0, 0);

        motion.decrementTransitionCountdown(0);
        motion.setState(0, 1);

        assertEquals(0x27, motion.transitionCountdown(0));
        assertEquals(1, motion.state(0));
    }

    @ParameterizedTest
    @CsvSource({"5, 16, -16, 1, 0", "10, -16, 16, 0, 1"})
    void steeringPreservesFractionalMovementOnBothAxes(
        int buttons, int targetDeltaX, int targetDeltaY, int stepX, int stepY) {
        BoomerangMotion motion = new BoomerangMotion();
        motion.initializeSpawn(0, 0, buttons);
        // A diagonal $18/$E8 step leaves $80 in both position accumulators.
        RoomEntity first = motion.advancePosition(entity(0x40, 0x40));
        motion.setVectorTowardsLink(0, first, first.x() + targetDeltaX,
            first.y() + targetDeltaY, 8);
        RoomEntity steered = motion.advancePosition(first);
        assertEquals(first.x() + stepX, steered.x());
        assertEquals(first.y() + stepY, steered.y());
    }

    @Test
    void objectPhysicsPredicateMatchesTheBoomerangCollisionWindow() {
        assertEquals(false, BoomerangMotion.objectPhysicsCollides(0x00));
        assertEquals(true, BoomerangMotion.objectPhysicsCollides(0x01));
        assertEquals(false, BoomerangMotion.objectPhysicsCollides(0x50));
        assertEquals(false, BoomerangMotion.objectPhysicsCollides(0x7C));
        assertEquals(true, BoomerangMotion.objectPhysicsCollides(0x90));
        assertEquals(false, BoomerangMotion.objectPhysicsCollides(0xA0));
        for (int physics = 0xD0; physics <= 0xD3; physics++) {
            assertEquals(true, BoomerangMotion.objectPhysicsCollides(physics));
        }
        assertEquals(false, BoomerangMotion.objectPhysicsCollides(0xD4));
    }

    private static void assertSpawnData(int direction, int speedX, int speedY) {
        BoomerangMotion.SpawnData actual = BoomerangMotion.spawnData(direction);
        assertEquals(0, actual.offsetX());
        assertEquals(0, actual.offsetY());
        assertEquals(speedX, actual.speedX());
        assertEquals(speedY, actual.speedY());
        assertEquals(direction, actual.initialVariant());
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, -1, BoomerangMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(BoomerangMotion.ENTITY_TYPE),
            0);
    }
}
