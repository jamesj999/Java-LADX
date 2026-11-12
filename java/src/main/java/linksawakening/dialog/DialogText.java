package linksawakening.dialog;

/** ROM dialog name tokens, terminators and width-dependent text preparation, independent of frame timing. */
final class DialogText {

    private static final String DEFAULT_PLAYER_NAME = "LINK";
    private static final int NAME_LENGTH = 5;
    private String playerName = DEFAULT_PLAYER_NAME;

    record ParsedText(String text) {}

    private static void appendSpace(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
            builder.append(' ');
        }
    }

    private static String normalizeAskMarkers(String value) {
        return value.replace("<ask>", String.valueOf(ASK_MARKER));
    }

    private static String stripAfterTerminator(String value) {
        int atTerminator = value.indexOf('@');
        int byteTerminator = value.indexOf('\u00FF');
        int terminator = Math.min(
                atTerminator < 0 ? Integer.MAX_VALUE : atTerminator,
                byteTerminator < 0 ? Integer.MAX_VALUE : byteTerminator);
        if (terminator == Integer.MAX_VALUE) {
            return value;
        }
        return value.substring(0, terminator);
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName == null || playerName.isBlank() ? DEFAULT_PLAYER_NAME : playerName;
    }

    String wrap(String value) {
        StringBuilder wrapped = new StringBuilder();
        int lineLength = 0;
        for (String word : value.split(" ", -1)) {
            if (word.isEmpty()) {
                if (lineLength < lineWidth) {
                    appendSpace(wrapped);
                    lineLength++;
                }
                continue;
            }
            if (lineLength > 0 && lineLength + 1 + word.length() > lineWidth) {
                wrapped.append('\n');
                lineLength = 0;
            } else if (lineLength > 0) {
                appendSpace(wrapped);
                lineLength++;
            }
            lineLength = appendWord(wrapped, word, lineLength);
        }
        return wrapped.toString();
    }

    String formatPreformatted(String value) {
        StringBuilder formatted = new StringBuilder(value.length() + value.length() / lineWidth);
        int lineLength = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\n') {
                formatted.append(current);
                lineLength = 0;
                continue;
            }
            if (current == ASK_MARKER) {
                formatted.append(current);
                continue;
            }
            if (lineLength == lineWidth) {
                formatted.append('\n');
                lineLength = 0;
            }
            formatted.append(current);
            lineLength++;
        }
        return formatted.toString();
    }

    private int appendWord(StringBuilder wrapped, String word, int lineLength) {
        int remaining = word.length();
        int offset = 0;
        while (remaining > 0) {
            int available = lineWidth - lineLength;
            if (available == 0) {
                wrapped.append('\n');
                lineLength = 0;
                available = lineWidth;
            }
            int count = Math.min(available, remaining);
            wrapped.append(word, offset, offset + count);
            offset += count;
            remaining -= count;
            lineLength += count;
        }
        return lineLength;
    }

    ParsedText parse(String rawText) {
        String normalized = normalizeAskMarkers(rawText);
        String terminated = stripAfterTerminator(normalized);
        return new ParsedText(substituteNameTokens(terminated));
    }

    private String substituteNameTokens(String value) {
        StringBuilder substituted = new StringBuilder(value.length());
        int nameIndex = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '#') {
                substituted.append(nameCharacter(nameIndex));
                nameIndex = (nameIndex + 1) % NAME_LENGTH;
            } else {
                substituted.append(current);
            }
        }
        return substituted.toString();
    }

    private char nameCharacter(int index) {
        return index < playerName.length() ? playerName.charAt(index) : ' ';
    }

    private final int lineWidth;
    private static final char ASK_MARKER = '\u00FE';

    DialogText(int lineWidth) {
        this.lineWidth = lineWidth;
    }
}
