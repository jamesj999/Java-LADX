package linksawakening.dialog;

import java.util.ArrayList;
import java.util.List;

public final class DialogController {

    private static final int LINK_Y_BOX_THRESHOLD = 0x48;
    private static final String DEFAULT_PLAYER_NAME = "LINK";
    private static final int NAME_LENGTH = 5;
    private static final char ASK_MARKER = '\u00FE';

    private final int lineWidth;
    private String text = "";
    private String playerName = DEFAULT_PLAYER_NAME;
    private int pageStart;
    private int visibleCharactersOnPage;
    private int visibleNonSpaceCharactersOnPage;
    private int revealTicks;
    private boolean active;
    private boolean waitingForPageAdvance;
    private boolean choicePrompt;
    private BoxPosition boxPosition = BoxPosition.BOTTOM;
    private final List<SoundEvent> soundEvents = new ArrayList<>();

    public enum SoundEvent {
        TEXT_PRINT(0x0F),
        DIALOG_BREAK(0x15),
        MOVE_SELECTION(0x0A);

        private final int code;

        SoundEvent(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    public enum BoxPosition {
        TOP(8),
        BOTTOM(96);

        private final int y;

        BoxPosition(int y) {
            this.y = y;
        }

        public int y() {
            return y;
        }
    }

    public DialogController(int lineWidth) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("lineWidth must be positive");
        }
        this.lineWidth = lineWidth;
    }

    public void open(String text) {
        open(text, BoxPosition.BOTTOM, false);
    }

    public void open(String text, BoxPosition boxPosition) {
        open(text, boxPosition, false);
    }

    public void openPreformatted(String text) {
        open(text, BoxPosition.BOTTOM, true);
    }

    public void openPreformatted(String text, BoxPosition boxPosition) {
        open(text, boxPosition, true);
    }

    public void openPreformattedForLinkY(String text, int linkY) {
        openPreformatted(text, linkY < LINK_Y_BOX_THRESHOLD ? BoxPosition.BOTTOM : BoxPosition.TOP);
    }

    private void open(String text, BoxPosition boxPosition, boolean preformatted) {
        ParsedText parsedText = parse(text == null ? "" : text);
        this.text = preformatted ? formatPreformatted(parsedText.text()) : wrap(parsedText.text());
        this.choicePrompt = false;
        this.boxPosition = boxPosition == null ? BoxPosition.BOTTOM : boxPosition;
        pageStart = 0;
        visibleCharactersOnPage = 0;
        visibleNonSpaceCharactersOnPage = 0;
        revealTicks = 0;
        waitingForPageAdvance = false;
        soundEvents.clear();
        active = true;
    }

    public void tick() {
        if (!active || waitingForPageAdvance) {
            return;
        }
        if (visibleCharactersOnPage >= currentPageLength()) {
            if (isAtChoicePromptMarker()) {
                enterChoicePrompt();
            }
            return;
        }
        revealTicks++;
        if (revealTicks % 2 != 0) {
            return;
        }
        char revealed = charAtVisibleCell(pageStart, visibleCharactersOnPage);
        if (revealed == ASK_MARKER) {
            enterChoicePrompt();
            return;
        }
        visibleCharactersOnPage++;
        if (!Character.isWhitespace(revealed)) {
            visibleNonSpaceCharactersOnPage++;
            if ((visibleNonSpaceCharactersOnPage & 0x01) == 1) {
                soundEvents.add(SoundEvent.TEXT_PRINT);
            }
        }
        enterPageWaitIfNeeded();
    }

    public void advance() {
        if (!active) {
            return;
        }
        if (choicePrompt) {
            active = false;
            choicePrompt = false;
            return;
        }
        if (waitingForPageAdvance) {
            pageStart = nextPageStart();
            visibleCharactersOnPage = 0;
            visibleNonSpaceCharactersOnPage = 0;
            revealTicks = 0;
            waitingForPageAdvance = false;
        } else if (visibleCharactersOnPage < currentPageLength()) {
            visibleCharactersOnPage = currentPageLength();
            enterPageWaitIfNeeded();
        } else if (isAtChoicePromptMarker()) {
            enterChoicePrompt();
        } else {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isWaitingForPageAdvance() {
        return waitingForPageAdvance;
    }

    public boolean isChoicePrompt() {
        return choicePrompt;
    }

    public void openForLinkY(String text, int linkY) {
        open(text, linkY < LINK_Y_BOX_THRESHOLD ? BoxPosition.BOTTOM : BoxPosition.TOP);
    }

    public BoxPosition boxPosition() {
        return boxPosition;
    }

    public int boxY() {
        return boxPosition.y();
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName == null || playerName.isBlank() ? DEFAULT_PLAYER_NAME : playerName;
    }

    public List<SoundEvent> consumeSoundEvents() {
        List<SoundEvent> events = List.copyOf(soundEvents);
        soundEvents.clear();
        return events;
    }

    public String visibleText() {
        int end = visibleEnd();
        if (pageStart >= end) {
            return "";
        }
        return text.substring(pageStart, end);
    }

    private void enterPageWaitIfNeeded() {
        if (!choicePrompt && visibleCharactersOnPage >= currentPageLength() && hasNextPage()) {
            waitingForPageAdvance = true;
            soundEvents.add(SoundEvent.DIALOG_BREAK);
        }
    }

    private int currentPageLength() {
        int end = pageEnd(pageStart);
        int visibleCells = 0;
        for (int i = pageStart; i < end; i++) {
            char current = text.charAt(i);
            if (current == ASK_MARKER) {
                return visibleCells;
            }
            if (current != '\n') {
                visibleCells++;
            }
        }
        return visibleCells;
    }

    private boolean hasNextPage() {
        int next = nextPageStart();
        return next < text.length() && text.charAt(next) != ASK_MARKER;
    }

    private int visibleEnd() {
        int pageEnd = pageEnd(pageStart);
        int visibleCells = 0;
        int index = pageStart;
        while (index < pageEnd && visibleCells < visibleCharactersOnPage) {
            char current = text.charAt(index);
            if (current == ASK_MARKER) {
                break;
            }
            if (current != '\n') {
                visibleCells++;
            }
            index++;
        }
        return index;
    }

    private char charAtVisibleCell(int start, int cellOffset) {
        int cell = 0;
        int end = pageEnd(start);
        for (int i = start; i < end; i++) {
            char current = text.charAt(i);
            if (current == '\n') {
                continue;
            }
            if (cell == cellOffset) {
                return current;
            }
            cell++;
        }
        return ' ';
    }

    private boolean isAtChoicePromptMarker() {
        int end = pageEnd(pageStart);
        int visibleCells = 0;
        for (int i = pageStart; i < end; i++) {
            char current = text.charAt(i);
            if (current == ASK_MARKER) {
                return visibleCells == visibleCharactersOnPage;
            }
            if (current != '\n') {
                visibleCells++;
            }
        }
        return false;
    }

    private void enterChoicePrompt() {
        choicePrompt = true;
        waitingForPageAdvance = false;
        soundEvents.add(SoundEvent.DIALOG_BREAK);
    }

    private int nextPageStart() {
        int next = pageEnd(pageStart);
        while (next < text.length() && text.charAt(next) == '\n') {
            next++;
        }
        return next;
    }

    private int pageEnd(int start) {
        int lines = 1;
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
                if (lines > 2) {
                    return i;
                }
            }
        }
        return text.length();
    }

    private String wrap(String value) {
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

    private String formatPreformatted(String value) {
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

    private static void appendSpace(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
            builder.append(' ');
        }
    }

    private ParsedText parse(String rawText) {
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

    private record ParsedText(String text) {
    }
}
