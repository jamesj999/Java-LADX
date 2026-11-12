package linksawakening.dialog;

import static linksawakening.dialog.DialogController.*;

/** Dialog text, timing and presentation used by ROM {@code DialogDrawNextCharacterHandler}. */
abstract class DialogTextFormatting extends DialogPaging {
    protected DialogTextFormatting(int lineWidth) {
        super(lineWidth);
    }

    protected String formatPreformatted(String value) {
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

    protected int appendWord(StringBuilder wrapped, String word, int lineLength) {
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

    protected ParsedText parse(String rawText) {
        String normalized = normalizeAskMarkers(rawText);
        String terminated = stripAfterTerminator(normalized);
        return new ParsedText(substituteNameTokens(terminated));
    }

    protected String substituteNameTokens(String value) {
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

    protected char nameCharacter(int index) {
        return index < playerName.length() ? playerName.charAt(index) : ' ';
    }
}
