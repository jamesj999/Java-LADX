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
        link.animation().presentToadstool();
        assertTrue(link.animation().dialogGotItemActive());
        assertEquals(0x2A, link.animation().dialogGotItemCountdown());
        assertEquals(0, link.animation().presentedDialogGotItem());
        assertEquals(0, link.airborne().tickDialogGotItem(false));
        assertEquals(0x6B, link.animation().romAnimationState());
        assertEquals(0x29, link.animation().dialogGotItemCountdown());
        assertTrue(link.status().isCollisionDamageImmune());
    }

    @Test
    void nonzeroZRejectsUseAndHeldPoseUsesRomFractionalZGravity() {
        var link = link();
        link.linkAirborne.zSubPixels = 16;
        link.animation().presentToadstool();
        assertFalse(link.animation().dialogGotItemActive());
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0x12;
        link.animation().presentToadstool();
        link.airborne().tickDialogGotItem(false);
        assertEquals(0x12, link.linkAirborne.zSubPixels);
        assertEquals(0x10, link.airborne().zVelocity());
        assertEquals(0, link.linkAnimator.bedJumpRenderZ);
        link.animation().clearDialogGotItem();
        assertFalse(link.animation().dialogGotItemActive());
        assertEquals(2, link.animation().presentedDialogGotItem());
        link.motion().resetTransientStateForDebug();
        assertEquals(0, link.animation().presentedDialogGotItem());
    }

    private static Link link() {
        return new LinkTestBuilder()
                .input(new linksawakening.input.InputState())
                .config(new linksawakening.input.InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new linksawakening.state.PlayerState())
                .items(new linksawakening.equipment.ItemRegistry())
                .build();
    }
}
