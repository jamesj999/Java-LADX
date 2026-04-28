package linksawakening.dialog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DialogControllerTest {

    @Test
    void revealsOneCharacterPerTickAndCompletesAfterAdvance() {
        DialogController dialog = new DialogController(16);

        dialog.open("Wake up");

        assertTrue(dialog.isActive());
        assertEquals("", dialog.visibleText());

        dialog.tick();
        dialog.tick();

        assertEquals("Wa", dialog.visibleText());

        dialog.advance();

        assertEquals("Wake up", dialog.visibleText());
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
}
