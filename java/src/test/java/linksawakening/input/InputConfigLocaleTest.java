package linksawakening.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Key names in the user's config must resolve the same way on every system locale. */
final class InputConfigLocaleTest {
    @Test
    void lowerCaseKeyNamesContainingIResolveUnderTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish upper-cases 'i' to a dotted capital I, which previously broke "right", "left_shift" and similar.
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            InputConfig config = InputConfig.fromKeyNames(Map.of("rightKey", "right", "aKey", "left_shift"));
            assertEquals(KeyCodes.KEY_RIGHT, config.rightKey());
            assertEquals(KeyCodes.KEY_LEFT_SHIFT, config.aKey());
        } finally {
            Locale.setDefault(original);
        }
    }
}
