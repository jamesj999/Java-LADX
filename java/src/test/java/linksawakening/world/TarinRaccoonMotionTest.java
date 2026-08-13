package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TarinRaccoonMotionTest {
    private static final int SLOT = 0;

    @Test
    void stateZeroUsesTheSourceAnimationAndLostWoodsVariants() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();

        TarinRaccoonMotion.Update normal = motion.advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x50, 0x60, false, false, false));

        assertEquals(0, normal.state());
        assertEquals(0, normal.spriteVariant());
        assertEquals(0, normal.entity().spriteVariant());
        assertFalse(normal.shouldGetLost());

        TarinRaccoonMotion.Update blocked = motion.advance(normal.entity(),
            new TarinRaccoonMotion.Input(1, 0x50, 0x1F, false, false, false));

        assertTrue(blocked.shouldGetLost());
        assertEquals(2, blocked.spriteVariant());

        TarinRaccoonMotion.Update laterFrame = motion.advance(blocked.entity(),
            new TarinRaccoonMotion.Input(8, 0x50, 0x2F, false, false, false));
        assertEquals(3, laterFrame.spriteVariant());

        TarinRaccoonMotion.Update normalLaterFrame = motion.advance(laterFrame.entity(),
            new TarinRaccoonMotion.Input(0x10, 0x50, 0x30, false, false, false));
        assertEquals(1, normalLaterFrame.spriteVariant());
        assertFalse(normalLaterFrame.shouldGetLost());
    }

    @Test
    void crossingAboveY20OpensDialog021OnlyOnceUntilTheLatchResets() {
        TarinRaccoonMotion motion = new TarinRaccoonMotion();

        TarinRaccoonMotion.Update firstCrossing = motion.advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x78, 0x1F, false, false, false));
        assertEquals(0x021, firstCrossing.dialogGlobalId());

        TarinRaccoonMotion.Update stillAbove = motion.advance(firstCrossing.entity(),
            new TarinRaccoonMotion.Input(1, 0x78, 0x1E, false, false, false));
        assertEquals(-1, stillAbove.dialogGlobalId());

        motion.advance(stillAbove.entity(),
            new TarinRaccoonMotion.Input(2, 0x78, 0x30, false, false, false));
        TarinRaccoonMotion.Update secondCrossing = motion.advance(stillAbove.entity(),
            new TarinRaccoonMotion.Input(3, 0x78, 0x1F, false, false, false));
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
    void stateZeroLeavesLaterTransformationEventsInactive() {
        TarinRaccoonMotion.Update update = new TarinRaccoonMotion().advance(raccoon(),
            new TarinRaccoonMotion.Input(0, 0x50, 0x60, false, false, true));

        assertEquals(0, update.state());
        assertFalse(update.linkMotionBlocked());
        assertFalse(update.roomChanged());
        assertFalse(update.tarinFlag());
        assertEquals(-1, update.soundChannel());
        assertEquals(-1, update.soundId());
        assertFalse(update.spawnBomb());
    }

    private static TarinRaccoonMotion.Input input(int frameCounter, int linkX, int linkY,
                                                  int linkDirection, boolean actionHeld,
                                                  boolean dialogActive) {
        return new TarinRaccoonMotion.Input(frameCounter, linkX, linkY, linkDirection,
            actionHeld, dialogActive, false);
    }

    private static RoomEntity raccoon() {
        return new RoomEntity(SLOT, 0, 0x3F, 0x78, 0x40, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x3F), 0);
    }
}
