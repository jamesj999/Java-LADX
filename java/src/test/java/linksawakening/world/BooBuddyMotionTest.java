package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BooBuddyMotionTest {

    @Test
    void turningAndAnimationCadenceDisplayThePreviouslySelectedFrame() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity first = motion.advance(entity(0x40, 0x50, 0), 15,
            0x20, 0x50, 0, 0, false, () -> 0).entity();
        assertEquals(0, first.spriteVariant());
        assertEquals(2, motion.selectedVariant(0));
        RoomEntity second = motion.advance(first, 16,
            0x20, 0x50, 0, 0, false, () -> 0).entity();
        assertEquals(2, second.spriteVariant());
        assertEquals(3, motion.selectedVariant(0));
        RoomEntity third = motion.advance(second, 17,
            0x70, 0x50, 0, 0, false, () -> 0).entity();
        assertEquals(3, third.spriteVariant());
        assertEquals(1, motion.selectedVariant(0));
    }

    @Test
    void disappearanceBlinkIsSavedForTheNextHandlerPass() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity first = motion.advance(entity(0x40, 0x50, 0), 3,
            0x70, 0x50, 3, 0, false, () -> 0).entity();
        assertEquals(0, first.spriteVariant());
        assertEquals(-1, motion.selectedVariant(0));
        RoomEntity second = motion.advance(first, 4,
            0x70, 0x50, 2, 0, false, () -> 0).entity();
        assertEquals(-1, second.spriteVariant());
        assertEquals(6, motion.selectedVariant(0));
    }

    @Test
    void fleeVariantSurvivesTheStateChangeAndSlotReuseClearsIt() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity first = motion.advance(entity(0x40, 0x50, 0), 15,
            0x70, 0x50, 0, 1, false, () -> 0).entity();
        RoomEntity second = motion.advance(first, 16,
            0x70, 0x50, 0, 1, false, () -> 0).entity();
        assertEquals(0, second.spriteVariant());
        assertEquals(5, motion.selectedVariant(0));
        RoomEntity third = motion.advance(second, 17,
            0x70, 0x50, 0, 0, false, () -> 0).entity();
        assertEquals(5, third.spriteVariant());
        assertEquals(5, motion.selectedVariant(0));
        motion.clear(0);
        RoomEntity reused = motion.advance(entity(0x40, 0x50, 0), 18,
            0x70, 0x50, 0, 0, false, () -> 0).entity();
        assertEquals(0, reused.spriteVariant());
    }

    @Test
    void driftsWithRomRandomVectorAndFourUnitAcceleration() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        BooBuddyMotion.Update first = motion.advance(entity, 0, 0x60, 0x50,
            0, 0, false, () -> 0);

        assertEquals(4, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0x40, first.entity().x());
        assertEquals(0, first.entity().spriteVariant());
        assertFalse(first.unloaded());
        assertTrue(motion.allowsEnemyCollision(0, 0, 0, false));
        assertFalse(motion.allowsSwordCollision(0, 0));

        BooBuddyMotion.Update second = motion.advance(first.entity(), 1, 0x60, 0x50,
            0, 0, false, () -> 0);
        assertEquals(8, motion.speedX(0));
        assertEquals(0x40, second.entity().x());
    }

    @Test
    void recoilSharesTheOrdinarySubpixelAccumulatorsWithoutChangingDriftSpeeds() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setSpeedForTest(0, 8, 8, 0);
        RoomEntity drifting = motion.advance(entity(0x40, 0x50, 0), 0,
            0x80, 0x50, 0, 0, true, () -> 0).entity();
        RoomEntity recoiling = motion.advanceRecoil(drifting, 8, 8);
        assertEquals(0x41, recoiling.x());
        assertEquals(0x51, recoiling.y());
        assertEquals(8, motion.speedX(0));
        assertEquals(8, motion.speedY(0));
        RoomEntity resumed = motion.advance(recoiling, 1,
            0x80, 0x50, 0, 0, true, () -> 0).entity();
        assertEquals(0x41, resumed.x());
        assertEquals(0x51, resumed.y());
    }

    @Test
    void coincidentPursuitUsesBothComponentsFromTheRomDivisionLoop() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.advance(entity(0x40, 0x50, 0), 0, 0x40, 0x50,
            0, 0, false, () -> 0);
        assertEquals(4, motion.speedX(0));
        assertEquals(4, motion.speedY(0));
    }

    @Test
    void coincidentFleeingNegatesBothRomVectorComponents() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);
        motion.advance(entity, 0, 0x40, 0x50, 0, 1, false, () -> 0);
        motion.advance(entity, 1, 0x40, 0x50, 0, 1, false, () -> 0);
        assertEquals(0xFC, motion.speedX(0));
        assertEquals(0xFC, motion.speedY(0));
    }

    @Test
    void roomTriggerMovesToFleeStateAndAimsAwayFromLink() {
        BooBuddyMotion motion = new BooBuddyMotion();
        RoomEntity entity = entity(0x40, 0x50, 0);

        BooBuddyMotion.Update triggered = motion.advance(entity, 0, 0x60, 0x50,
            0, 1, false, () -> 0);
        assertEquals(1, motion.state(0));
        assertFalse(motion.allowsEnemyCollision(0, 0, 0, false));
        assertTrue(motion.allowsSwordCollision(0, 1));
        assertEquals(-1, triggered.healthOverride());

        BooBuddyMotion.Update fleeing = motion.advance(triggered.entity(), 1,
            0x60, 0x50, 0, 1, false, () -> 0);
        assertEquals(0xFC, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, fleeing.entity().spriteVariant());
        assertEquals(4, motion.selectedVariant(0));
        assertEquals(1, fleeing.healthOverride());
        assertTrue(motion.allowsEnemyCollision(0, 1, 0, false));
    }

    @Test
    void transitionAnimationClearsAStalePrivateStateBeforeTheLastTick() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setPrivateStateForTest(0, 1);
        RoomEntity entity = entity(0x30, 0x40, 0);

        BooBuddyMotion.Update hidden = motion.advance(entity, 0, 0x60, 0x40,
            2, 0, false, () -> 0);
        assertEquals(0, hidden.entity().spriteVariant());
        assertEquals(-1, motion.selectedVariant(0));
        assertEquals(0x30, hidden.entity().x());
        assertEquals(0x40, hidden.entity().y());
        assertEquals(0, motion.privateCountdown1(0));
    }

    @ParameterizedTest
    @CsvSource({"0xA7, 0x50, 16, 0, false", "0xA8, 0x50, -16, 0, true",
        "0x40, 0x87, 0, 16, false", "0x40, 0x88, 0, -16, true"})
    void boundaryCheckUsesTheCoordinatesCachedBeforeMovement(
        int x, int y, int speedX, int speedY, boolean unloaded) {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setSpeedForTest(0, speedX, speedY, 0);
        RoomEntity entity = entity(x, y, 0);

        BooBuddyMotion.Update update = motion.advance(entity, 0, 0x40, 0x50,
            0, 0, false, () -> 0);

        assertEquals(unloaded, update.unloaded());
    }

    @ParameterizedTest
    @CsvSource({"0x70, 0x50", "0x40, 0x10"})
    void distantSwordSwingKeepsExistingSpeedAndSkipsRandomSteering(int linkX, int linkY) {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setSpeedForTest(0, 0x10, 0xF0, 0);
        BooBuddyMotion.Update update = motion.advance(entity(0x40, 0x50, 0), 0,
            linkX, linkY, 0, 0, true,
            () -> { throw new AssertionError("Distant swing must skip GetRandomByte"); });
        assertEquals(0x41, update.entity().x());
        assertEquals(0x4F, update.entity().y());
        assertEquals(0x10, motion.speedX(0));
        assertEquals(0xF0, motion.speedY(0));
        assertEquals(0, update.transitionCountdown());
    }

    @Test
    void nearbySwordSwingStartsDisappearanceAndStopsAfterTheCurrentMovement() {
        BooBuddyMotion motion = new BooBuddyMotion();
        motion.setSpeedForTest(0, 0x10, 0xF0, 0);
        BooBuddyMotion.Update update = motion.advance(entity(0x40, 0x50, 0), 0,
            0x40, 0x50, 0, 0, true,
            () -> { throw new AssertionError("Nearby swing must skip GetRandomByte"); });
        assertEquals(0x41, update.entity().x());
        assertEquals(0x4F, update.entity().y());
        assertEquals(0x20, update.transitionCountdown());
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, update.entity().spriteVariant());
        assertEquals(-1, motion.selectedVariant(0));
    }

    @Test
    void finalDisappearanceTickReflectsPositionAndStartsPrivateCountdown() {
        BooBuddyMotion motion = new BooBuddyMotion();
        // Movement precedes the reflection: ($30,$40) becomes ($31,$3F).
        motion.setSpeedForTest(0, 0x10, 0xF0, 0);
        BooBuddyMotion.Update teleported = motion.advance(entity(0x30, 0x40, 0), 4,
            0x50, 0x40, 1, 0, false, () -> 0);
        assertEquals(0x6F, teleported.entity().x());
        assertEquals(0x51, teleported.entity().y());
        assertEquals(0, teleported.entity().spriteVariant());
        assertEquals(7, motion.selectedVariant(0));
        assertEquals(0x20, motion.privateCountdown1(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));

        RoomEntity current = teleported.entity();
        for (int frame = 5; frame < 36; frame++) {
            current = motion.advance(current, frame, 0x50, 0x40, 0, 0, false,
                () -> { throw new AssertionError("Reappearance must skip steering"); })
                .entity();
        }
        assertEquals(1, motion.privateCountdown1(0));
        assertEquals(0x6F, current.x());
        assertEquals(0x51, current.y());
        motion.advance(current, 36, 0x50, 0x40, 0, 0, false, () -> 0);
        assertEquals(0, motion.privateCountdown1(0));
        assertEquals(0xFC, motion.speedX(0));
    }

    private static RoomEntity entity(int x, int y, int variant) {
        return new RoomEntity(0, 0, BooBuddyMotion.ENTITY_TYPE, x, y,
            EntityStatus.ACTIVE, definition(), variant);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        return new EntitySpriteDefinition(BooBuddyMotion.ENTITY_TYPE, 0x06, 0x79A9,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second),
                new EntitySpriteDefinition.Variant(first, second)));
    }
}
