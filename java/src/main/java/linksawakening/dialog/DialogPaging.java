package linksawakening.dialog;

import static linksawakening.dialog.DialogController.*;

import java.util.List;

/** DialogPaging behavior on the shared DialogController state. */
abstract class DialogPaging extends DialogLifecycle {
    protected DialogPaging(int lineWidth) {
        super(lineWidth);
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
        if (scrollAge >= 0) return scrollAge == 0 ? scrollPrevious : retainedLine;
        int end = visibleEnd();
        if (romTimed && shiftedPage) {
            String revealed = text.substring(pageStart, end).replace("\n", "");
            return retainedLine + "\n" + revealed.substring(Math.min(scrollBase, revealed.length()));
        }
        if (pageStart >= end) {
            return "";
        }
        return text.substring(pageStart, end);
    }

    protected void enterPageWaitIfNeeded() {
        if (!choicePrompt && visibleCharactersOnPage >= currentPageLength() && hasNextPage()) {
            waitingForPageAdvance = true;
            pageWaitAge = 0;
            soundEvents.add(SoundEvent.DIALOG_BREAK);
        }
    }

    protected int currentPageLength() {
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

    protected boolean hasNextPage() {
        int next = nextPageStart();
        return next < text.length() && text.charAt(next) != ASK_MARKER;
    }

    protected int visibleEnd() {
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

    protected char charAtVisibleCell(int start, int cellOffset) {
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

    protected boolean isAtChoicePromptMarker() {
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

    protected void enterChoicePrompt() {
        choicePrompt = true;
        waitingForPageAdvance = false;
        soundEvents.add(SoundEvent.DIALOG_BREAK);
    }

    protected void closeDialog() {
        if (romTimed) {
            closingAge = 0;
            return;
        }
        finishClosing();
    }

    protected void finishClosing() {
        active = false;
        choicePrompt = false;
        dialogCooldown = DIALOG_COOLDOWN;
    }

    protected int nextPageStart() {
        int next = pageEnd(pageStart);
        while (next < text.length() && text.charAt(next) == '\n') {
            next++;
        }
        return next;
    }

    protected int pageEnd(int start) {
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

    protected String wrap(String value) {
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
}
