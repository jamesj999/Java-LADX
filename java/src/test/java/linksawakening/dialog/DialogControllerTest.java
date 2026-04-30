package linksawakening.dialog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DialogControllerTest {

    @Test
    void revealsOneVisibleCharacterEveryTwoTicks() {
        DialogController dialog = new DialogController(16);

        dialog.open("Wake");

        assertTrue(dialog.isActive());
        assertEquals("", dialog.visibleText());

        dialog.tick();
        assertEquals("", dialog.visibleText());

        dialog.tick();
        assertEquals("W", dialog.visibleText());

        dialog.tick();
        assertEquals("W", dialog.visibleText());

        dialog.tick();
        assertEquals("Wa", dialog.visibleText());
    }

    @Test
    void emitsTextPrintSoundOnlyForNonSpaceCharactersOnRevealCadence() {
        DialogController dialog = new DialogController(16);

        dialog.open("ABC D");

        dialog.tick();
        assertEquals(List.of(), dialog.consumeSoundEvents());

        dialog.tick();
        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), dialog.consumeSoundEvents());

        dialog.tick();
        dialog.tick();
        assertEquals(List.of(), dialog.consumeSoundEvents());

        dialog.tick();
        dialog.tick();
        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), dialog.consumeSoundEvents());

        dialog.tick();
        dialog.tick();
        assertEquals(List.of(), dialog.consumeSoundEvents());

        dialog.tick();
        dialog.tick();
        assertEquals(List.of(), dialog.consumeSoundEvents());
    }

    @Test
    void textPrintCadenceIgnoresSpacesWhenCountingVisibleCharacters() {
        DialogController dialog = new DialogController(16);

        dialog.open("AB CD");

        revealNextCharacter(dialog);
        assertEquals("A", dialog.visibleText());
        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), dialog.consumeSoundEvents());

        revealNextCharacter(dialog);
        assertEquals("AB", dialog.visibleText());
        assertEquals(List.of(), dialog.consumeSoundEvents());

        revealNextCharacter(dialog);
        assertEquals("AB ", dialog.visibleText());
        assertEquals(List.of(), dialog.consumeSoundEvents());

        revealNextCharacter(dialog);
        assertEquals("AB C", dialog.visibleText());
        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), dialog.consumeSoundEvents());
    }

    @Test
    void waitsAfterTwoRowsOfSixteenCharactersAndEmitsDialogBreak() {
        DialogController dialog = new DialogController(16);

        dialog.open("12345678901234567890123456789012NEXT");

        tick(dialog, 62);
        dialog.consumeSoundEvents();
        tick(dialog, 2);

        assertEquals("1234567890123456\n7890123456789012", dialog.visibleText());
        assertTrue(dialog.isWaitingForPageAdvance());
        assertEquals(List.of(DialogController.SoundEvent.DIALOG_BREAK), dialog.consumeSoundEvents());

        dialog.tick();
        assertEquals("1234567890123456\n7890123456789012", dialog.visibleText());
    }

    @Test
    void advanceDuringRevealCompletesOnlyCurrentPage() {
        DialogController dialog = new DialogController(16);

        dialog.open("12345678901234567890123456789012NEXT");

        dialog.tick();
        dialog.tick();

        dialog.advance();

        assertEquals("1234567890123456\n7890123456789012", dialog.visibleText());
        assertTrue(dialog.isActive());
        assertTrue(dialog.isWaitingForPageAdvance());
    }

    @Test
    void advanceAtPageBreakMovesToNextPageAndAdvanceAtEndCloses() {
        DialogController dialog = new DialogController(16);

        dialog.open("12345678901234567890123456789012NEXT");
        dialog.advance();

        dialog.advance();
        assertEquals("", dialog.visibleText());
        assertTrue(dialog.isActive());

        dialog.advance();
        assertEquals("NEXT", dialog.visibleText());
        assertTrue(dialog.isActive());

        dialog.advance();
        assertFalse(dialog.isActive());
    }

    @Test
    void wrapsVisibleTextToConfiguredLineWidth() {
        DialogController dialog = new DialogController(5);

        dialog.open("MARIN BEACH");
        dialog.advance();

        assertEquals("MARIN\nBEACH", dialog.visibleText());
    }

    @Test
    void visiblePageIsLimitedToTwoLines() {
        DialogController dialog = new DialogController(5);

        dialog.open("ONE TWO THREE");
        dialog.advance();

        assertEquals("ONE\nTWO", dialog.visibleText());
    }

    @Test
    void selectsBottomPositionWhenLinkIsAboveThreshold() {
        DialogController dialog = new DialogController(16);

        dialog.openForLinkY("Hi", 0x47);

        assertEquals(DialogController.BoxPosition.BOTTOM, dialog.boxPosition());
        assertEquals(96, dialog.boxY());
    }

    @Test
    void selectsTopPositionWhenLinkIsAtOrBelowThreshold() {
        DialogController dialog = new DialogController(16);

        dialog.openForLinkY("Hi", 0x48);

        assertEquals(DialogController.BoxPosition.TOP, dialog.boxPosition());
        assertEquals(8, dialog.boxY());
    }

    @Test
    void openPreformattedPreservesSpacesAndUsesFixedSixteenCharacterRows() {
        DialogController dialog = new DialogController(16);

        dialog.openPreformatted("ABCDEFGHIJKLMNO  SECOND ROW TEXT");
        dialog.advance();

        assertEquals("ABCDEFGHIJKLMNO \n SECOND ROW TEXT", dialog.visibleText());
    }

    @Test
    void openPreformattedPagesAtThirtyTwoCharacters() {
        DialogController dialog = new DialogController(16);

        dialog.openPreformatted("12345678901234567890123456789012NEXT");
        dialog.advance();

        assertEquals("1234567890123456\n7890123456789012", dialog.visibleText());
        assertTrue(dialog.isWaitingForPageAdvance());

        dialog.advance();
        dialog.advance();

        assertEquals("NEXT", dialog.visibleText());
    }

    @Test
    void openPreformattedForLinkYSelectsBoxPositionFromThreshold() {
        DialogController dialog = new DialogController(16);

        dialog.openPreformattedForLinkY("Hi", 0x47);
        assertEquals(DialogController.BoxPosition.BOTTOM, dialog.boxPosition());

        dialog.openPreformattedForLinkY("Hi", 0x48);
        assertEquals(DialogController.BoxPosition.TOP, dialog.boxPosition());
    }

    @Test
    void terminatorStopsDialogTextInsteadOfRenderingTerminator() {
        DialogController dialog = new DialogController(16);

        dialog.open("Hi@Ignored");
        dialog.advance();

        assertEquals("Hi", dialog.visibleText());
    }

    @Test
    void byteTerminatorStopsDialogTextInsteadOfRenderingTerminator() {
        DialogController dialog = new DialogController(16);

        dialog.open("Hi\u00FFIgnored");
        dialog.advance();

        assertEquals("Hi", dialog.visibleText());
    }

    @Test
    void soundEventCodesMatchLadxJingles() {
        assertEquals(0x0F, DialogController.SoundEvent.TEXT_PRINT.code());
        assertEquals(0x15, DialogController.SoundEvent.DIALOG_BREAK.code());
        assertEquals(0x0A, DialogController.SoundEvent.MOVE_SELECTION.code());
    }

    @Test
    void substitutesOnePlayerNameCharacterPerTokenAndResetsOnOpen() {
        DialogController dialog = new DialogController(16);

        dialog.setPlayerName("MARIN");
        dialog.openPreformatted("Hi #####@Ignored");
        dialog.advance();

        assertEquals("Hi MARIN", dialog.visibleText());

        dialog.openPreformatted("##@Ignored");
        dialog.advance();

        assertEquals("MA", dialog.visibleText());
    }

    @Test
    void playerNameTokenWrapsAfterFiveCharacters() {
        DialogController dialog = new DialogController(16);

        dialog.setPlayerName("MARIN");
        dialog.openPreformatted("##### #####@Ignored");
        dialog.advance();

        assertEquals("MARIN MARIN", dialog.visibleText());
    }

    @Test
    void askMarkerTerminatesTextWhenRevealReachesMarkerAndQueuesDialogBreak() {
        DialogController dialog = new DialogController(16);

        dialog.openPreformatted("Take it?<ask>Ignored");

        dialog.advance();

        assertFalse(dialog.isChoicePrompt());
        assertEquals(List.of(), dialog.consumeSoundEvents());

        dialog.advance();

        assertTrue(dialog.isChoicePrompt());
        assertEquals("Take it?", dialog.visibleText());
        assertEquals(List.of(DialogController.SoundEvent.DIALOG_BREAK), dialog.consumeSoundEvents());

        dialog.advance();

        assertFalse(dialog.isActive());
        assertEquals(List.of(), dialog.consumeSoundEvents());
    }

    @Test
    void byteAskMarkerTerminatesTextWhenRevealReachesMarkerAndQueuesDialogBreak() {
        DialogController dialog = new DialogController(16);

        dialog.openPreformatted("Take it?\u00FEIgnored");

        dialog.advance();

        assertFalse(dialog.isChoicePrompt());
        assertEquals(List.of(), dialog.consumeSoundEvents());

        dialog.advance();

        assertTrue(dialog.isChoicePrompt());
        assertEquals("Take it?", dialog.visibleText());
        assertEquals(List.of(DialogController.SoundEvent.DIALOG_BREAK), dialog.consumeSoundEvents());

        dialog.advance();

        assertFalse(dialog.isActive());
        assertEquals(List.of(), dialog.consumeSoundEvents());
    }

    private static void tick(DialogController dialog, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dialog.tick();
        }
    }

    private static void revealNextCharacter(DialogController dialog) {
        tick(dialog, 2);
    }
}
