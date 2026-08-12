package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WitchMotionTest {
    private static final int SLOT = 0;

    @Test
    void animatesAtTheRomCadenceAndForcesTheCauldronYPosition() {
        WitchMotion motion = new WitchMotion();
        RoomEntity witch = witch();

        WitchMotion.Update update = motion.advance(witch, 0, inputs(false));

        assertEquals(0x40, update.entity().y());
        assertEquals(1, update.inertia());
        assertEquals(0, update.entity().spriteVariant());

        motion.setInertiaForTest(SLOT, 0x1F);
        update = motion.advance(witch, 0, inputs(false));
        assertEquals(0x20, update.inertia());
        assertEquals(1, update.entity().spriteVariant());
    }

    @Test
    void talkingWithoutToadstoolOpensDialog00cAcrossTheCauldron() {
        WitchMotion motion = new WitchMotion();
        WitchMotion.Input input = new WitchMotion.Input(
            false, 0, 0, true, false, false, false, 0x48, 0x56, 2,
            0x04, 0, 0, 0, 0x07);

        WitchMotion.Update update = motion.advance(witch(), 0, input);

        assertEquals(0x00C, update.dialogGlobalId());
        assertFalse(update.exchangeStarted());
    }

    @Test
    void airborneLinkCannotTalkOrStartTheExchange() {
        WitchMotion motion = new WitchMotion();
        WitchMotion.Input input = new WitchMotion.Input(
            true, 0x0C, 0, false, true, false, true, 0x48, 0x56, 2,
            0x04, 0, 0, 0, 0x07);

        WitchMotion.Update update = motion.advance(witch(), 0, input);

        assertFalse(update.exchangeStarted());
        assertEquals(-1, update.dialogGlobalId());
    }

    @Test
    void exchangeRequiresTheButtonMatchingTheEquippedEmptyPowderBag() {
        WitchMotion motion = new WitchMotion();
        WitchMotion.Input wrongButton = new WitchMotion.Input(
            true, 0x0C, 0, true, false, false, false, 0x48, 0x56, 2,
            0x04, 0, 0, 0, 0x07);

        WitchMotion.Update rejected = motion.advance(witch(), 0, wrongButton);
        assertFalse(rejected.exchangeStarted());
        assertEquals(-1, rejected.dialogGlobalId());

        WitchMotion.Input bButton = new WitchMotion.Input(
            true, 0x0C, 0, false, true, false, false, 0x48, 0x56, 2,
            0x04, 0, 0, 0, 0x07);
        WitchMotion.Update accepted = motion.advance(witch(), 0, bButton);
        assertTrue(accepted.exchangeStarted());
        assertEquals(WitchMotion.InventorySlot.B, accepted.clearedSlot());
        assertEquals(0x08, accepted.transitionCountdown());
        assertEquals(1, motion.stateForTest(SLOT));

        WitchMotion aMotion = new WitchMotion();
        WitchMotion.Input aButton = new WitchMotion.Input(
            true, 0, 0x0C, true, false, false, false, 0x48, 0x56, 2,
            0x04, 0, 0, 0, 0x07);
        WitchMotion.Update acceptedA = aMotion.advance(witch(), 0, aButton);
        assertEquals(WitchMotion.InventorySlot.A, acceptedA.clearedSlot());
    }

    @Test
    void runsTheExactDialogTimingRewardAndTorchTutorialSequence() {
        WitchMotion motion = new WitchMotion();
        WitchMotion.Update update = motion.advance(witch(), 0,
            new WitchMotion.Input(true, 0x0C, 0, false, true, false, false,
                0x48, 0x56, 2, 0x04, 0, 0, 0, 0x07));

        update = motion.advance(update.entity(), 7, inputs(false));
        assertTrue(update.blockLink());
        assertEquals(1, motion.stateForTest(SLOT));
        update = motion.advance(update.entity(), 0, inputs(false));
        assertEquals(2, motion.stateForTest(SLOT));
        update = motion.advance(update.entity(), 0, inputs(false));
        assertEquals(0x009, update.dialogGlobalId());
        assertEquals(0xC0, update.transitionCountdown());
        assertEquals(3, motion.stateForTest(SLOT));

        update = motion.advance(update.entity(), 0xBF, inputs(true));
        assertFalse(update.blockLink());
        update = motion.advance(update.entity(), 0xBF, inputs(false));
        assertTrue(update.blockLink());
        assertEquals(0x07, update.musicTrack());
        update = motion.advance(update.entity(), 0, inputs(false));
        assertEquals(0x0FE, update.dialogGlobalId());
        assertEquals(0x07, update.musicTrack());
        assertEquals(4, motion.stateForTest(SLOT));

        update = motion.advance(update.entity(), 0, inputs(true));
        assertFalse(update.grantMagicPowder());
        update = motion.advance(update.entity(), 0, inputs(false));
        assertTrue(update.grantMagicPowder());
        assertEquals(0x01, update.jingleId());
        assertEquals(5, motion.stateForTest(SLOT));

        update = motion.advance(update.entity(), 0, inputs(false));
        assertEquals(5, motion.stateForTest(SLOT));
        WitchMotion.Input torchLit = new WitchMotion.Input(
            false, 0, 0, false, false, false, false, 0x48, 0x56, 2,
            0x04, 1, 0, 0, 0x07);
        update = motion.advance(update.entity(), 0, torchLit);
        assertEquals(6, motion.stateForTest(SLOT));
        update = motion.advance(update.entity(), 0, torchLit);
        assertEquals(0x17E, update.dialogGlobalId());
        assertEquals(7, motion.stateForTest(SLOT));
    }

    @Test
    void waitsForPaletteWorkToSettleAfterTheTutorialTorchIsLit() {
        WitchMotion motion = new WitchMotion();
        motion.setStateForTest(SLOT, 5);

        WitchMotion.Input changing = new WitchMotion.Input(
            false, 0, 0, false, false, false, false, 0, 0, 0,
            0x04, 1, 2, 0, 0x07);
        motion.advance(witch(), 0, changing);
        assertEquals(5, motion.stateForTest(SLOT));

        WitchMotion.Input loading = new WitchMotion.Input(
            false, 0, 0, false, false, false, false, 0, 0, 0,
            0x04, 1, 0, 0x81, 0x07);
        motion.advance(witch(), 0, loading);
        assertEquals(5, motion.stateForTest(SLOT));
    }

    private static WitchMotion.Input inputs(boolean dialogActive) {
        return new WitchMotion.Input(false, 0, 0, false, false, dialogActive, false,
            0, 0, 0, 0x04, 0, 0, 0, 0x07);
    }

    private static RoomEntity witch() {
        return new RoomEntity(SLOT, 0, 0x40, 0x48, 0x40, EntityStatus.ACTIVE,
            linksawakening.entity.EntitySpriteDefinition.unsupported(0x40), 0);
    }
}
