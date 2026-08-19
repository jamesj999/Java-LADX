package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class HinoxMotionTest {
    private static final int HINOX = 0x89;

    @Test
    void initialPathStopsOnCollisionAndWanderChoosesRomCardinalSpeed() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(entity.slot());

        HinoxMotion.Update blocked = motion.advance(entity, 0, 0, 0x40, 0x40, 0,
            0, () -> 0x11, null, 0x01, 0);
        assertEquals(HinoxMotion.STATE_WANDER, blocked.state());
        assertEquals(0x11, blocked.transitionCountdown());
        assertEquals(0, motion.speedX(entity.slot()));
        assertEquals(0, motion.speedY(entity.slot()));

        HinoxMotion.Update wander = motion.advance(blocked.entity(), 0,
            1, 0x40, 0x40, 0, 0, () -> 0x00, null, 0, 0);
        assertEquals(0x08, motion.speedX(entity.slot()));
        assertEquals(0x00, motion.speedY(entity.slot()));
        assertEquals(HinoxMotion.STATE_WANDER, wander.state());
    }

    @Test
    void initialStateUsesHinoxCollisionTableInsteadOfCallerLinkCollision() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(entity.slot());

        HinoxMotion.Update noEntityCollision = motion.advance(entity, 0x12, 0,
            0x40, 0x40, 0, 0, () -> 0x00, null, 0x00, 0);
        assertEquals(HinoxMotion.STATE_INITIAL, noEntityCollision.state());

        motion.setEntityCollisionForTest(entity.slot(), 0x01);
        HinoxMotion.Update entityCollision = motion.advance(noEntityCollision.entity(), 0x12, 1,
            0x40, 0x40, 0, 0, () -> 0x00, null, 0x00, 0);
        assertEquals(HinoxMotion.STATE_WANDER, entityCollision.state());
    }

    @Test
    void wanderStateDoesNotAdvanceInertia() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40, 1);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_WANDER, 0, 0);
        motion.setInertiaForTest(entity.slot(), 0x0F);

        HinoxMotion.Update update = motion.advance(entity, 0x12, 0,
            0x40, 0x40, 0, 0, () -> 0, null, 0, 0);

        assertEquals(0x0F, motion.inertia(entity.slot()));
        assertEquals(1, update.spriteVariant());
    }

    @Test
    void wanderStateDoesNotUpdatePositionUntilTheNextInitialStatePass() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_WANDER, 0, 0);
        motion.setSpeedForTest(entity.slot(), 0x08, 0);

        HinoxMotion.Update update = motion.advance(entity, 0x12, 0,
            0x20, 0x40, 0, 0, () -> 0, null, 0, 0);

        assertEquals(0x40, update.entity().x());
        assertEquals(0x40, update.entity().y());
    }

    @Test
    void initialStateAdvancesInertiaAfterItsBackgroundPass() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40, 0);
        motion.initialize(entity.slot());
        motion.setInertiaForTest(entity.slot(), 0x0F);

        HinoxMotion.Update update = motion.advance(entity, 0x12, 0,
            0x40, 0x40, 0, 0, () -> 0, null, 0, 0);

        assertEquals(0x10, motion.inertia(entity.slot()));
        assertEquals(1, update.spriteVariant());
    }

    @Test
    void privateCountdownIsConsumedOnlyByTheSharedTimerOwner() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_GRAB, 0, 0);
        motion.setPrivateCountdown1ForTest(entity.slot(), 2);

        assertTrue(motion.advance(entity, 0x1F, 0, 0x40, 0x40, 0,
            0, () -> 0, null, 0, 0).linkMotionBlocked());
        motion.decrementPrivateCountdown1(entity.slot());
        assertTrue(motion.advance(entity, 0x1F, 1, 0x40, 0x40, 0,
            0, () -> 0, null, 0, 0).linkMotionBlocked());
    }

    @Test
    void secondWanderCycleCanEnterThirtyTickChargeWindupAndVectorTowardLink() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_WANDER, 0, 0);
        motion.setPrivateState3ForTest(entity.slot(), 1);

        HinoxMotion.Update windup = motion.advance(entity, 0, 0, 0x40, 0x40, 0,
            0, () -> 0x00, null, 0, 0);
        assertEquals(HinoxMotion.STATE_CHARGE_WINDUP, windup.state());
        assertEquals(0x30, windup.transitionCountdown());

        HinoxMotion.Update charge = motion.advance(windup.entity(), 0, 0, 0x70, 0x40, 0,
            0, () -> 0, null, 0, 0);
        assertEquals(HinoxMotion.STATE_CHARGE, charge.state());
        assertEquals(0x18, motion.speedX(entity.slot()));
        assertEquals(0, motion.speedY(entity.slot()));
        assertEquals(0x20, charge.jingleId());

        HinoxMotion.Update noJingle = motion.advance(charge.entity(), 0x20, 0x01,
            0x70, 0x40, 0, 0, () -> 0, null, 0, 0);
        assertEquals(-1, noJingle.jingleId());
    }

    @Test
    void chargeGrabsLinkThenThrowsAtTwentyWithSourceEffects() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_CHARGE, 0x40, 0);
        motion.setSpeedForTest(entity.slot(), 0, 0);

        HinoxMotion.Update grabbed = motion.advance(entity, 0x10, 0, 0x60, 0x40, 0,
            0, () -> 0, null, 0, 0);
        assertEquals(HinoxMotion.STATE_GRAB, grabbed.state());
        assertEquals(0x4F, grabbed.transitionCountdown());
        HinoxMotion.Update held = motion.advance(grabbed.entity(), grabbed.transitionCountdown(), 0,
            0x60, 0x40, 0, 0, () -> 0, null, 0, 0);
        assertFalse(held.linkMotionBlocked());
        assertEquals(0x50, held.heldLinkX());
        assertEquals(0x40, held.heldLinkY());
        assertEquals(0x02, held.linkAirborneState());
        assertTrue(held.applyHeldLinkPose());
        assertEquals(0x00, held.spriteVariant());
        assertTrue(held.heldLinkPositionWritten());

        HinoxMotion.Update thrown = motion.advance(grabbed.entity(), 0x20, 1, 0x60, 0x40, 0x0C,
            0, () -> 0, null, 0, 0);
        assertFalse(thrown.linkMotionBlocked());
        assertEquals(0x50, thrown.heldLinkX());
        assertEquals(0x40, thrown.heldLinkY());
        assertEquals(0xE0, thrown.linkSpeedX());
        assertEquals(0x20, thrown.linkSpeedY());
        assertEquals(0x10, thrown.linkVelocityZ());
        assertEquals(0x02, thrown.linkAirborneState());
        assertEquals(0x08, thrown.linkDamage());
        assertEquals(0x0C, thrown.heldLinkZ());
        assertFalse(thrown.applyHeldLinkPose());
        assertEquals(0x08, thrown.jingleId());
        assertEquals(-1, thrown.waveId());
        assertTrue(thrown.heldLinkPositionWritten());

        HinoxMotion.Update finalHeld = motion.advance(grabbed.entity(), 0x20, 2,
            0x60, 0x40, 0, 0, () -> 0, null, 0, 0);
        assertEquals(-1, finalHeld.spriteVariant());
    }

    @Test
    void chargeTransitionEmitsGrowlOnWaveAndBounceOnJingleChannels() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_CHARGE, 0, 0);
        motion.setSpeedForTest(entity.slot(), 0, 0);

        HinoxMotion.Update update = motion.advance(entity, 0, 0,
            0x30, 0x30, 0, 0, () -> 0, null, 0, 0);

        assertEquals(0x16, update.waveId());
        assertEquals(0x20, update.jingleId());
    }

    @Test
    void stateFourBelowTwentyReturnsBeforeWritingHeldPosition() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_GRAB, 0, 0);

        HinoxMotion.Update update = motion.advance(entity, 0x1F, 0,
            0x60, 0x40, 0, 0, () -> 0, null, 0, 0);

        assertFalse(update.heldLinkPositionWritten());
        assertEquals(0x60, update.heldLinkX());
        assertEquals(0x40, update.heldLinkY());
    }

    @Test
    void grabCountdownZeroReturnsToInitialState() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_GRAB, 0, 0);

        HinoxMotion.Update update = motion.advance(entity, 0, 0,
            0x50, 0x40, 0, 0, () -> 0, null, 0, 0);

        assertEquals(HinoxMotion.STATE_INITIAL, update.state());
        assertEquals(0, update.transitionCountdown());
    }

    @Test
    void flashAtThreeEntersBombStateAndSpawnsEnemyBombAtTen() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_WANDER, 0x10, 0);

        HinoxMotion.Update bombState = motion.advance(entity, 0, 2, 0x50, 0x40, 0,
            0, () -> 0, null, 0, 0x03);
        assertEquals(HinoxMotion.STATE_BOMB, bombState.state());
        assertEquals(0x20, bombState.transitionCountdown());

        HinoxMotion.Update bomb = motion.advance(bombState.entity(), 0x10, 3, 0x30, 0x40, 0,
            0, () -> 0, null, 0, 0);
        assertNotNull(bomb.bombSpawn());
        assertEquals(HinoxMotion.ENTITY_BOMB, bomb.bombSpawn().type());
        assertEquals(0x44, bomb.bombSpawn().x());
        assertEquals(0x40, bomb.bombSpawn().y());
        assertEquals(0x10, bomb.bombSpawn().z());
        assertEquals(0xF0, bomb.bombSpawn().speedX());
        assertEquals(0x0C, bomb.bombSpawn().speedY());
        assertEquals(0x18, bomb.bombSpawn().speedZ());

        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_GRAB, 0, 0);
        HinoxMotion.Update grabbedFlash = motion.advance(entity, 0x20, 4,
            0x30, 0x40, 0, 0, () -> 0, null, 0, 0x03);
        assertEquals(HinoxMotion.STATE_GRAB, grabbedFlash.state());
    }

    @Test
    void bombUsesTheLastStateThreeDustOriginInsteadOfCurrentPosition() {
        HinoxMotion motion = new HinoxMotion();
        RoomEntity entity = entity(0x50, 0x40);
        motion.initialize(entity.slot());
        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_CHARGE, 0, 0);
        motion.setSpeedForTest(entity.slot(), 0, 0);

        HinoxMotion.Update dust = motion.advance(entity, 1, 0, 0x30, 0x30, 0,
            0, () -> 0, null, 0, 0);
        assertTrue(dust.dustRequested());

        motion.setStateForTest(entity.slot(), HinoxMotion.STATE_BOMB, 0, 0);
        HinoxMotion.Update bomb = motion.advance(dust.entity(), 0x10, 1,
            0x30, 0x30, 0, 0, () -> 0, null, 0, 0);

        assertEquals(0x44, bomb.bombSpawn().x());
        assertEquals(0x4A, bomb.bombSpawn().y());
    }

    private static RoomEntity entity(int x, int y) {
        return entity(x, y, -1);
    }

    private static RoomEntity entity(int x, int y, int spriteVariant) {
        return new RoomEntity(0, 0, HINOX, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(HINOX), spriteVariant);
    }
}
