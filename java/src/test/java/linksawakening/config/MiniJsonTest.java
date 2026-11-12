package linksawakening.config;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class MiniJsonTest {
    @Test
    void readsNestedValuesAndAllLiterals() {
        Map<?, ?> root =
                (Map<?, ?>) MiniJson.parse(" {\"nested\": [true, false, null, {\"n\":-1.25e+2}], \"empty\":{}} \r\n\t");
        List<?> nested = (List<?>) root.get("nested");
        assertEquals(true, nested.get(0));
        assertEquals(false, nested.get(1));
        assertNull(nested.get(2));
        assertEquals(new BigDecimal("-1.25e+2"), ((Map<?, ?>) nested.get(3)).get("n"));
        assertEquals(Map.of(), root.get("empty"));
        assertEquals(List.of(), MiniJson.parse("[]"));
    }

    @Test
    void readsEveryStringEscape() {
        assertEquals("\"\\/\b\f\n\r\tAZ", MiniJson.parse("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041Z\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0", "123", "-42", "0.5", "1e3", "1E-2", "1e+2"})
    void readsNumbers(String source) {
        assertEquals(new BigDecimal(source), MiniJson.parse(source));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "{",
                "[",
                "tru",
                "False",
                "+1",
                "01",
                "-01",
                ".5",
                "1.",
                "1e",
                "1e+",
                "--1",
                "NaN",
                "Infinity",
                "null false",
                "[1,]",
                "[,1]",
                "{\"x\":1,}",
                "{x:1}",
                "{\"x\" 1}",
                "{\"x\":1 \"y\":2}",
                "{\"x\":1,\"x\":2}",
                "\"unterminated",
                "\"\\q\"",
                "\"\\u12\"",
                "\"\\uXXXX\"",
                "\"line\nbreak\"",
                "\u00a0null",
                "1 2"
            })
    void rejectsMalformedJsonWithPosition(String source) {
        var error = assertThrows(IllegalArgumentException.class, () -> MiniJson.parse(source));
        assertTrue(error.getMessage().contains("position"), error.getMessage());
    }

    @Test
    void rejectsExcessiveNesting() {
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("[".repeat(130) + "0" + "]".repeat(130)));
    }

    @Test
    void configReadsOnlyItsOwnObjectAndHandlesEscapedKeys() {
        var config = AppConfig.parse(
                "{\"nested\":{\"showTitleScreen\":false},\"showTitleScree\\u006e\":true,\"controls\":{\"aKey\":\"J\"}}");
        assertTrue(config.showTitleScreen());
        assertEquals(74, config.inputConfig().aKey());
    }

    @Test
    void configRejectsMalformedInputAndWrongRoot() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.parse("{garbage}"));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.parse("[]"));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.parse("{\"debugEnabled\":0}"));
    }
}
