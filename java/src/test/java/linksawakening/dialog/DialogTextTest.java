package linksawakening.dialog;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DialogTextTest {
    @Test
    void substitutionTerminatorsAndAskMarkersAreIndependentOfFrameTiming() {
        var text = new DialogText(4);
        text.setPlayerName("AB");
        assertEquals("AB   AB\u00FE", text.parse("#######<ask>@ignored").text());
        text.setPlayerName(" ");
        assertEquals("LINK ", text.parse("#####\u00FFignored").text());
    }

    @Test
    void wrappingAndPreformattedBreaksPreserveTheirDifferentSpaceRules() {
        var text = new DialogText(4);
        assertEquals("AB\nCDEF", text.wrap("AB CDEF"));
        assertEquals("AB C\nDEF", text.formatPreformatted("AB CDEF"));
        assertEquals("AB\nCD", text.formatPreformatted("AB\nCD"));
    }
}
