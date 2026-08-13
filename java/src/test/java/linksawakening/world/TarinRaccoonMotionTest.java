package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TarinRaccoonMotionTest {
    private static final int SLOT = 0;

    @Test
    void stateZeroUsesTheSourceAnimationAndLostWoodsVariants() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();

        TarinRaccoonMotion.Update normal = motion.advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x50, 0x60, 0, false, false, false));

        assertEquals(0, normal.state());
        assertEquals(0, normal.spriteVariant());
        assertEquals(0, normal.entity().spriteVariant());
        assertFalse(normal.shouldGetLost());

        TarinRaccoonMotion.Update blocked = motion.advance(normal.entity(),
            new TarinRaccoonMotion.Input(1, 0x50, 0x1F, 0, false, false, false));

        assertTrue(blocked.shouldGetLost());
        assertEquals(2, blocked.spriteVariant());

        TarinRaccoonMotion.Update laterFrame = motion.advance(blocked.entity(),
            new TarinRaccoonMotion.Input(8, 0x50, 0x2F, 0, false, false, false));
        assertEquals(3, laterFrame.spriteVariant());

        TarinRaccoonMotion.Update normalLaterFrame = motion.advance(laterFrame.entity(),
            new TarinRaccoonMotion.Input(0x10, 0x50, 0x30, 0, false, false, false));
        assertEquals(1, normalLaterFrame.spriteVariant());
        assertFalse(normalLaterFrame.shouldGetLost());
    }

    @Test
    void crossingAboveY20OpensDialog021OnlyOnceUntilTheLatchResets() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();

        TarinRaccoonMotion.Update firstCrossing = motion.advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x78, 0x1F, 0, false, false, false));
        assertEquals(0x021, firstCrossing.dialogGlobalId());

        TarinRaccoonMotion.Update stillAbove = motion.advance(firstCrossing.entity(),
            new TarinRaccoonMotion.Input(1, 0x78, 0x1E, 0, false, false, false));
        assertEquals(-1, stillAbove.dialogGlobalId());

        motion.advance(stillAbove.entity(),
            new TarinRaccoonMotion.Input(2, 0x78, 0x30, 0, false, false, false));
        TarinRaccoonMotion.Update secondCrossing = motion.advance(stillAbove.entity(),
            new TarinRaccoonMotion.Input(3, 0x78, 0x1F, 0, false, false, false));
        assertEquals(0x021, secondCrossing.dialogGlobalId());
    }

    @Test
    void actionTalkRequiresSourceProximityFacingAndNoActiveDialog() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();

        TarinRaccoonMotion.Update facing = motion.advance(raccoon(),
            input(0, 0x78, 0x50, 2, true, false));
        assertEquals(0x00D, facing.dialogGlobalId());

        TarinRaccoonMotion.Update wrongDirection = motion.advance(facing.entity(),
            input(1, 0x78, 0x50, 3, true, false));
        assertEquals(-1, wrongDirection.dialogGlobalId());

        TarinRaccoonMotion.Update tooFar = motion.advance(wrongDirection.entity(),
            input(2, 0x40, 0x50, 0, true, false));
        assertEquals(-1, tooFar.dialogGlobalId());

        TarinRaccoonMotion.Update dialogActive = motion.advance(tooFar.entity(),
            input(3, 0x78, 0x50, 2, true, true));
        assertEquals(-1, dialogActive.dialogGlobalId());
    }

    @Test
    void attackStepCountdownSuppressesDialog00d() {
        TarinRaccoonMotion.Input attacking = new TarinRaccoonMotion.Input(
            0, 0x78, 0x50, 2, true, false, false, 1);

        TarinRaccoonMotion.Update update = new TarinRaccoonMotion().advance(
            raccoon(), attacking);

        assertEquals(-1, update.dialogGlobalId());
    }

    @Test
    void inputRejectsDirectionsOutsideTheRomRange() {
        assertThrows(IllegalArgumentException.class,
            () -> new TarinRaccoonMotion.Input(0, 0, 0, -1, false, false, false));
        assertThrows(IllegalArgumentException.class,
            () -> new TarinRaccoonMotion.Input(0, 0, 0, 4, false, false, false));
    }

    @Test
    void stateZeroLeavesLaterTransformationEventsInactive() {
        TarinRaccoonMotion.Update update = new TarinRaccoonMotion().advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x50, 0x60, 0, false, false, true));

        assertEquals(0, update.state());
        assertFalse(update.linkMotionBlocked());
        assertFalse(update.roomChanged());
        assertFalse(update.tarinFlag());
        assertEquals(-1, update.soundChannel());
        assertEquals(-1, update.soundId());
        assertFalse(update.spawnBomb());
    }

    @Test
    void stateOneBlocksLinkClearsAttackFacesLinkAndCyclesTheSourceVariants() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();
        motion.setStateForTest(SLOT, 1, 0, 0, 0, 0, 0, false);

        List<Integer> observed = new ArrayList<>();
        RoomEntity entity = raccoon();
        for (int frame = 0; frame < 0x1000 && observed.size() < 6; frame += 2) {
            TarinRaccoonMotion.Update update = motion.advance(entity,
                inputWithCountdowns(frame & 0xFF, 0x50, 0x40, 0, 0x70, 0));
            assertTrue(update.linkMotionBlocked());
            assertTrue(update.clearLinkAttack());
            assertEquals(0, update.linkFacingDirection());
            if (observed.isEmpty()
                || observed.get(observed.size() - 1) != update.spriteVariant()) {
                observed.add(update.spriteVariant());
            }
            entity = update.entity();
        }
        assertEquals(List.of(0, 4, 5, 6, 7, 1), observed);
    }

    @Test
    void stateOneMovesBeforeTheFinalWindowAndBouncesOnBackgroundCollision() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();
        motion.setStateForTest(SLOT, 1, 0x10, 0, 0, 0, 0, false);
        RoomEntityBackgroundInteraction wall = (entity, direction, x, y) ->
            EntityBackgroundCollisionResult.blocked(direction, 0xFF, 0, x, y);

        TarinRaccoonMotion.Update update = motion.advance(
            withPosition(raccoon(), 0x78, 0x2F, 0),
            inputWithCountdowns(1, 0x50, 0x40, 0, 5, 0), wall);

        assertEquals(0x78, update.entity().x());
        assertEquals(0xF0, update.speedX());
        assertEquals(8, update.slowTransitionCountdown());
        assertEquals(0x09, update.soundId());
    }

    @Test
    void stateOneLowYExtendsTheWindowButHighYStartsTheSourceZAndSpeedDamping() {
        TarinRaccoonMotion low = new TarinRaccoonMotion();
        low.setStateForTest(SLOT, 1, 0x10, 0, 0, 0, 0, false);
        RoomEntity lowEntity = withPosition(raccoon(), 0x78, 0x2F, 0);
        TarinRaccoonMotion.Update extended = low.advance(lowEntity,
            inputWithCountdowns(1, 0x50, 0x40, 0, 5, 0));
        assertEquals(8, extended.slowTransitionCountdown());
        assertEquals(0, extended.speedZ());

        TarinRaccoonMotion high = new TarinRaccoonMotion();
        high.setStateForTest(SLOT, 1, 0xF0, 0x10, 0, 0, 0, false);
        RoomEntity highEntity = withPosition(raccoon(), 0x78, 0x30, 0);
        TarinRaccoonMotion.Update arcing = high.advance(highEntity,
            inputWithCountdowns(1, 0x50, 0x40, 0, 5, 0));
        assertEquals(5, arcing.slowTransitionCountdown());
        assertEquals(1, arcing.speedZ());
        assertEquals(0xF1, arcing.speedX());
        assertEquals(0x0F, arcing.speedY());
    }

    @Test
    void stateOneExpiryRequestsTheExactBombAndPersistenceWrites() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();
        motion.setStateForTest(SLOT, 1, 0x20, 0xE0, 0x12, 0, 0, false);
        RoomEntity source = withPosition(raccoon(), 0x66, 0x55, 0x0A);

        TarinRaccoonMotion.Update update = motion.advance(source,
            inputWithCountdowns(4, 0x50, 0x40, 0, 0, 0));

        assertEquals(2, update.state());
        assertEquals(9, update.spriteVariant());
        assertEquals(0, update.speedZ());
        assertTrue(update.spawnBomb());
        assertTrue(update.roomChanged());
        assertTrue(update.tarinFlag());
    }

    @Test
    void stateTwoArcLandsWithSourceCountdownFacingAndNearLatch() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();
        motion.setStateForTest(SLOT, 2, 0, 0, 0xF0, 0, 0, false);
        RoomEntity source = withPosition(raccoon(), 0x78, 0x40, 0);

        TarinRaccoonMotion.Update update = motion.advance(source,
            inputWithCountdowns(0, 0x79, 0x40, 0, 0, 0));

        assertEquals(3, update.state());
        assertEquals(0, update.entity().z());
        assertEquals(0x40, update.transitionCountdown());
        assertEquals(8, update.spriteVariant());
        assertTrue(update.nearLinkLatch());
        assertEquals(0x23, update.soundId());
    }

    @Test
    void stateThreeUsesAlreadyDecrementedCountdownThenFacesPushesAndTalks() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();
        motion.setStateForTest(SLOT, 3, 0, 0, 0, 0, 0, false);

        TarinRaccoonMotion.Update dialogA = motion.advance(raccoon(),
            inputWithCountdowns(1, 0x78, 0x50, 2, 0, 1));
        assertEquals(0x00A, dialogA.dialogGlobalId());
        assertFalse(dialogA.linkMotionBlocked());

        TarinRaccoonMotion.Update blocked = motion.advance(dialogA.entity(),
            inputWithCountdowns(2, 0x78, 0x50, 2, 0, 2));
        assertTrue(blocked.linkMotionBlocked());

        TarinRaccoonMotion.Update talk = motion.advance(blocked.entity(),
            new TarinRaccoonMotion.Input(0x20, 0x78, 0x50, 2, true, false,
                false, 7, false, false, 0, 0x80, 0, 0));
        assertEquals(0x00B, talk.dialogGlobalId());
        assertEquals(11, talk.spriteVariant());
        assertTrue(talk.pushLink());
    }

    private static TarinRaccoonMotion.Input input(int frameCounter, int linkX, int linkY,
                                                  int linkDirection, boolean actionHeld,
                                                  boolean dialogActive) {
        return new TarinRaccoonMotion.Input(frameCounter, linkX, linkY, linkDirection,
            actionHeld, dialogActive, false);
    }

    private static TarinRaccoonMotion.Input inputWithCountdowns(int frameCounter,
            int linkX, int linkY, int linkDirection, int slowCountdown,
            int transitionCountdown) {
        return new TarinRaccoonMotion.Input(frameCounter, linkX, linkY, linkDirection,
            false, false, false, 0, false, false, 0, 0x80,
            slowCountdown, transitionCountdown);
    }

    private static RoomEntity withPosition(RoomEntity source, int x, int y, int z) {
        return new RoomEntity(source.slot(), source.sourceLoadOrder(), source.type(), x, y,
            source.status(), source.spriteDefinition(), source.spriteVariant(),
            source.entityFlipAttribute(), source.spriteTileOffset(), z);
    }

    private static RoomEntity raccoon() {
        return new RoomEntity(SLOT, 0, 0x3F, 0x78, 0x40, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x3F), 0);
    }
}
