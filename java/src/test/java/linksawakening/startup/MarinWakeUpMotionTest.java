package linksawakening.startup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarinWakeUpMotionTest {
    @Test
    void slowTossingCountdownDecrementsOnlyEveryFourthUnpausedFrame() {
        MarinWakeUpMotion motion = new MarinWakeUpMotion();

        for (int frame = 1; frame < 508; frame++) {
            assertFalse(motion.tick(frame, false, false).openWakeDialog());
        }

        MarinWakeUpMotion.Update update = motion.tick(508, false, false);
        assertTrue(update.openWakeDialog());
        assertEquals(0x01, update.dialogLowId());
        assertEquals(3, update.bedSpriteVariant());
    }

    @Test
    void firstDirectionAfterTheWakeDialogImmediatelyLeavesTheBed() {
        MarinWakeUpMotion motion = awakeMotion();

        for (int frame = 509; frame < 709; frame++) {
            assertFalse(motion.tick(frame, true, true).leaveBed());
        }

        MarinWakeUpMotion.Update update = motion.tick(709, false, true);
        assertTrue(update.leaveBed());
        assertFalse(update.linkMotionBlocked());
        assertTrue(motion.complete());
    }

    @Test
    void exposesRomBedOamVariantsAndCoordinates() {
        MarinWakeUpMotion motion = new MarinWakeUpMotion();

        MarinWakeUpMotion.Update tossing = motion.tick(1, false, false);
        assertEquals(1, tossing.bedSpriteVariant());
        assertTrue(tossing.linkMotionBlocked());
        assertEquals(0x38, tossing.linkRomX());
        assertEquals(0x34, tossing.linkRomY());

        MarinWakeUpMotion.Update awake = null;
        for (int frame = 2; frame <= 508; frame++) awake = motion.tick(frame, false, false);
        assertEquals(3, awake.bedSpriteVariant());
    }

    @Test
    void shiftsMarinOnceAndUpdatesHerFacingEveryThirtyTwoFrames() {
        MarinWakeUpMotion motion = new MarinWakeUpMotion();

        MarinWakeUpMotion.MarinPresentation initial = motion.tickMarinPresentation(
            1, 0x58, 0x60, 0x38, 0x34);
        assertEquals(0x50, initial.romX());
        assertEquals(0x58, initial.romY());
        assertEquals(1, initial.romDirection());
        assertEquals(4, initial.spriteVariant());

        MarinWakeUpMotion.MarinPresentation facing = null;
        for (int frame = 2; frame <= 32; frame++) {
            facing = motion.tickMarinPresentation(frame, 0x50, 0x58, 0x70, 0x58);
        }
        assertEquals(0, facing.romDirection());
        assertEquals(7, facing.spriteVariant());

        MarinWakeUpMotion.MarinPresentation frameBoundaryInitial =
            new MarinWakeUpMotion().tickMarinPresentation(32, 0x58, 0x60, 0x70, 0x60);
        assertEquals(1, frameBoundaryInitial.romDirection());

        MarinWakeUpMotion inertiaMotion = new MarinWakeUpMotion();
        inertiaMotion.tickMarinPresentation(1, 0x58, 0x60, 0x38, 0x34);
        MarinWakeUpMotion.MarinPresentation beforeToggle = null;
        for (int frame = 2; frame <= 16; frame++) {
            beforeToggle = inertiaMotion.tickMarinPresentation(frame, 0x50, 0x58, 0x38, 0x34);
        }
        assertEquals(4, beforeToggle.spriteVariant());
        assertEquals(5, inertiaMotion.tickMarinPresentation(
            17, 0x50, 0x58, 0x38, 0x34).spriteVariant());
    }

    @Test
    void ordinaryMarinTalkRejectsAirborneLinkAndUsesSourceFacingWindow() {
        MarinWakeUpMotion motion = awakeMotion();
        for (int frame = 509; frame <= 572; frame++) motion.tick(frame, false, false);
        motion.tick(573, false, true);

        assertTrue(motion.canOpenFollowUpDialog(0x40, 0x50,
            0x40, 0x60, 2, false, true, false));
        assertFalse(motion.canOpenFollowUpDialog(0x40, 0x50,
            0x40, 0x60, 2, true, true, false));
        assertFalse(motion.canOpenFollowUpDialog(0x40, 0x50,
            0x40, 0x60, 3, false, true, false));
        assertEquals(0x02, motion.followUpDialogLowId());
    }

    @Test
    void restoredHouseStartsMarinTalkableWithoutReplayingWakeUp() {
        MarinWakeUpMotion motion = MarinWakeUpMotion.postWake();

        MarinWakeUpMotion.Update update = motion.tick(1, false, false);

        assertTrue(motion.complete());
        assertFalse(update.openWakeDialog());
        assertFalse(update.linkMotionBlocked());
        assertTrue(motion.canOpenFollowUpDialog(0x40, 0x50,
            0x40, 0x60, 2, false, true, false));
    }

    private static MarinWakeUpMotion awakeMotion() {
        MarinWakeUpMotion motion = new MarinWakeUpMotion();
        for (int frame = 1; frame <= 508; frame++) motion.tick(frame, false, false);
        return motion;
    }
}
