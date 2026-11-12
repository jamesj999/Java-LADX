package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class DialogGotItemSequenceTest {
    @Test
    void opensAtTwoPausesDuringDialogAndClearsAtZero() {
        var sequence = new DialogGotItemSequence();
        sequence.start(2, 0x2A);
        assertEquals(0, sequence.presentedItem());
        for (int frame = 1; frame <= 40; frame++) {
            assertEquals(frame == 40 ? 2 : 0, sequence.advance(false));
            assertEquals(0x2A - frame, sequence.countdown());
            assertEquals(2, sequence.item());
            assertEquals(2, sequence.presentedItem());
        }
        for (int frame = 0; frame < 100; frame++) {
            assertEquals(0, sequence.advance(true));
            assertEquals(2, sequence.countdown());
        }
        sequence.advance(false);
        assertEquals(1, sequence.countdown());
        assertEquals(2, sequence.item());
        sequence.advance(false);
        assertEquals(0, sequence.item());
        assertEquals(0, sequence.presentedItem());
    }

    @Test
    void byteCountdownWrapsAndExplicitClearPreservesAlreadySubmittedPresentation() {
        var sequence = new DialogGotItemSequence();
        sequence.start(2, 0);
        sequence.advance(false);
        assertEquals(0xFF, sequence.countdown());
        sequence.clear();
        assertEquals(0, sequence.item());
        assertEquals(2, sequence.presentedItem());
        sequence.advance(false);
        assertEquals(0, sequence.presentedItem());
    }

    @Test
    void groundedUseStartsStateButDoesNotConsumeOwnershipOrChangePoseUntilNextPass() {
        var link = link();
        link.presentToadstool();
        assertTrue(link.dialogGotItemActive());
        assertEquals(0x2A, link.dialogGotItemCountdown());
        assertEquals(0, link.presentedDialogGotItem());
        assertEquals(0, link.tickDialogGotItem(false));
        assertEquals(0x6B, link.romAnimationState());
        assertEquals(0x29, link.dialogGotItemCountdown());
        assertTrue(link.isCollisionDamageImmune());
    }

    @Test
    void nonzeroZRejectsUseAndHeldPoseUsesRomFractionalZGravity() {
        var link = link();
        link.linkAirborne.zSubPixels = 16;
        link.presentToadstool();
        assertFalse(link.dialogGotItemActive());
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0x12;
        link.presentToadstool();
        link.tickDialogGotItem(false);
        assertEquals(0x12, link.linkAirborne.zSubPixels);
        assertEquals(0x10, link.zVelocity());
        assertEquals(0, link.linkAnimator.bedJumpRenderZ);
        link.clearDialogGotItem();
        assertFalse(link.dialogGotItemActive());
        assertEquals(2, link.presentedDialogGotItem());
        link.resetTransientStateForDebug();
        assertEquals(0, link.presentedDialogGotItem());
    }

    private static Link link() {
        return new Link(
                new linksawakening.input.InputState(),
                new linksawakening.input.InputConfig(1, 2, 3, 4, 5, 6, 7),
                null,
                null,
                null,
                new linksawakening.state.PlayerState(),
                new linksawakening.equipment.ItemRegistry());
    }
}
