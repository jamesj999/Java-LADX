package linksawakening.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Strict, dependency-free JSON reader. Errors report the zero-based source position. */
public final class MiniJson {
    private final String source;
    private int position;

    private MiniJson(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static Object parse(String source) {
        MiniJson reader = new MiniJson(source);
        Object value = reader.value(0);
        reader.space();
        if (reader.position != source.length()) throw reader.error("trailing content");
        return value;
    }

    private Object value(int depth) {
        space();
        if (depth > 128) throw error("nesting exceeds 128 levels");
        if (position == source.length()) throw error("expected value");
        return switch (source.charAt(position)) {
            case '{' -> object(depth + 1);
            case '[' -> array(depth + 1);
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object(int depth) {
        position++;
        Map<String, Object> result = new LinkedHashMap<>();
        space();
        if (take('}')) return result;
        do {
            space();
            if (position == source.length() || source.charAt(position) != '"') throw error("expected object key");
            String key = string();
            space();
            expect(':');
            Object value = value(depth);
            if (result.containsKey(key)) throw error("duplicate object key: " + key);
            result.put(key, value);
            space();
            if (take('}')) return result;
            expect(',');
        } while (true);
    }

    private List<Object> array(int depth) {
        position++;
        List<Object> result = new ArrayList<>();
        space();
        if (take(']')) return result;
        do {
            result.add(value(depth));
            space();
            if (take(']')) return result;
            expect(',');
        } while (true);
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < source.length()) {
            char ch = source.charAt(position++);
            if (ch == '"') return result.toString();
            if (ch < 0x20) throw error("unescaped control character");
            if (ch == '\\') {
                if (position == source.length()) throw error("incomplete escape");
                ch = source.charAt(position++);
                ch = switch (ch) {
                    case '"', '\\', '/' -> ch;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> unicode();
                    default -> throw error("invalid escape");
                };
            }
            result.append(ch);
        }
        throw error("unterminated string");
    }

    private char unicode() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            if (position == source.length()) throw error("incomplete Unicode escape");
            char ch = source.charAt(position++);
            int digit = ch >= '0' && ch <= '9'
                    ? ch - '0'
                    : ch >= 'a' && ch <= 'f' ? ch - 'a' + 10 : ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : -1;
            if (digit < 0) throw error("invalid Unicode escape");
            value = value * 16 + digit;
        }
        return (char) value;
    }

    private BigDecimal number() {
        int start = position;
        take('-');
        if (!take('0')) digits();
        if (take('.')) digits();
        if (take('e') || take('E')) {
            if (!take('+')) take('-');
            digits();
        }
        try {
            return new BigDecimal(source.substring(start, position));
        } catch (NumberFormatException invalid) {
            throw error("invalid number");
        }
    }

    private void digits() {
        int start = position;
        while (position < source.length() && source.charAt(position) >= '0' && source.charAt(position) <= '9')
            position++;
        if (position == start) throw error("expected digit");
    }

    private Object literal(String text, Object value) {
        if (!source.startsWith(text, position)) throw error("expected " + text);
        position += text.length();
        return value;
    }

    private void space() {
        while (position < source.length()) {
            char ch = source.charAt(position);
            if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t') return;
            position++;
        }
    }

    private boolean take(char expected) {
        if (position < source.length() && source.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!take(expected)) throw error("expected '" + expected + "'");
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Invalid JSON at position " + position + ": " + message);
    }
}
