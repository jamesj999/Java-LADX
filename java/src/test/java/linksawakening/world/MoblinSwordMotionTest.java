package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MoblinSwordMotionTest {

    @Test
    void idleStateStartsTheRomWalkingTimerAndFlipsTheInitDirection() {
        MoblinSwordMotion motion = new MoblinSwordMotion();
        motion.initialize(0, 0x50);
        RoomEntity source = entity(0, 0x50, 0x40);

        MoblinSwordMotion.Update update = motion.advance(
            source, 0xC0, 0x40, null, 0, 0);

        assertEquals(source, update.entity());
        assertEquals(1, motion.state(0));
        assertEquals(0x80, motion.transitionCountdown(0));
        assertEquals(0, motion.direction(0));
        assertEquals(0x06, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @Test
    void alertingWithinTheRomWindowEntersStateTwoAndFacesLink() {
        MoblinSwordMotion motion = new MoblinSwordMotion();
        motion.initialize(0, 0x50);
        RoomEntity source = entity(0, 0x50, 0x40);

        motion.advance(source, 0xC0, 0x40, null, 0, 0);
        MoblinSwordMotion.Update update = motion.advance(
            source, 0x70, 0x40, null, 0, 1);

        assertEquals(source.x(), update.entity().x());
        assertEquals(source.y(), update.entity().y());
        assertEquals(2, motion.state(0));
        assertEquals(0x80, motion.transitionCountdown(0));
        assertEquals(0x10, motion.privateCountdown1(0));
        assertEquals(0, motion.direction(0));
    }

    @Test
    void walkingStateDoesNotDispatchStateTwoOnItsTransitionFrame() {
        MoblinSwordMotion motion = new MoblinSwordMotion();
        motion.initialize(0, 0x50);
        RoomEntity current = entity(0, 0x50, 0x40);

        current = motion.advance(current, 0xC0, 0x40, null, 0, 0).entity();
        for (int frame = 1; frame <= 0x80; frame++) {
            current = motion.advance(current, 0xC0, 0x40, null, 0, frame).entity();
        }

        assertEquals(2, motion.state(0));
        assertEquals(0x30, motion.transitionCountdown(0));
        assertEquals(0x80, motion.inertia(0));
    }

    @Test
    void stateTwoTransitionLeavesSpeedUntilStateZeroStartsAgain() {
        MoblinSwordMotion motion = new MoblinSwordMotion();
        motion.initialize(1, 0x50);
        RoomEntity current = entity(1, 0x50, 0x40);

        current = motion.advance(current, 0xC0, 0x40, null, 0, 0).entity();
        for (int frame = 1; frame <= 0xB0; frame++) {
            current = motion.advance(current, 0xC0, 0x40, null, 0, frame).entity();
        }

        assertEquals(0, motion.state(1));
        assertEquals(0x18, motion.transitionCountdown(1));
        // The state-2 attraction refresh has selected +$0A by this point;
        // the state-2 -> state-0 transition must not reverse it prematurely.
        assertEquals(0x0A, motion.speedX(1));
    }

    @Test
    void hideoutAlertOpensDialog190OnceAtTransitionSequenceFour() {
        MoblinSwordMotion motion = new MoblinSwordMotion();
        motion.initialize(0, 0x50);
        RoomEntity current = entity(0, 0x50, 0x40);

        current = motion.advance(current, 0xC0, 0x40, null, 0, 0).entity();
        current = motion.advance(current, 0x70, 0x40, null, 0, 1).entity();

        MoblinSwordMotion.Update dialogFrame = motion.advance(
            current, 0x70, 0x40, null, 0, 0, 0x15, 0x04, 2);
        assertTrue(dialogFrame.dialogRequested());
        assertEquals(1, motion.privateState3(0));

        MoblinSwordMotion.Update followingFrame = motion.advance(
            dialogFrame.entity(), 0x70, 0x40, null, 0, 0, 0x15, 0x04, 3);
        assertFalse(followingFrame.dialogRequested());
        assertEquals(1, motion.privateState3(0));
    }

    private static RoomEntity entity(int slot, int x, int y) {
        return new RoomEntity(slot, 0, 0x14, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x14), -1);
    }
}
