package linksawakening.dialog;

import java.util.ArrayList;
import java.util.List;

/** DialogDrawNextCharacterHandler: ordered opening, reveal, paging, choice and closing transitions. */
public final class DialogController {

    private static final int LINK_Y_BOX_THRESHOLD = 0x48;
    private static final int DIALOG_COOLDOWN = 0x18;
    private static final char ASK_MARKER = '\u00FE';

    private final int lineWidth;
    private boolean romTimed;
    private int openingAge;
    private int closingAge = -1;
    private int scrollAge = -1;
    private int scrollBase;
    private String scrollPrevious = "";
    private String retainedLine = "";
    private boolean shiftedPage;
    private String text = "";
    private int pageStart;
    private int visibleCharactersOnPage;
    private int visibleNonSpaceCharactersOnPage;
    private int revealTicks;
    private int dialogCooldown;
    private boolean active;
    private boolean interactionLocked;
    private boolean waitingForPageAdvance;
    private int pageWaitAge;
    private boolean choicePrompt;
    private int choiceSelectionIndex;
    private BoxPosition boxPosition = BoxPosition.BOTTOM;
    private final List<SoundEvent> soundEvents = new ArrayList<>();

    public DialogController(int lineWidth) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("lineWidth must be positive");
        }
        this.lineWidth = lineWidth;
        this.textPreparation = new DialogText(lineWidth);
    }

    /** Gameplay timing includes the source BG save/setup and five row writes. */
    public static DialogController forGameplay() {
        DialogController dialog = new DialogController(16);
        dialog.romTimed = true;
        return dialog;
    }

    public boolean isBoxRowVisible(int row) {
        if (!romTimed) return true;
        if (closingAge >= 0 && closingAge >= DialogRomLayout.closingFrameForRow(row)) return false;
        return openingAge >= DialogRomLayout.openingFrameForRow(row);
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
        DialogText.ParsedText parsedText = textPreparation.parse(text == null ? "" : text);
        this.text = preformatted
                ? textPreparation.formatPreformatted(parsedText.text())
                : textPreparation.wrap(parsedText.text());
        this.choicePrompt = false;
        this.choiceSelectionIndex = 0;
        this.boxPosition = boxPosition == null ? BoxPosition.BOTTOM : boxPosition;
        openingAge = 0;
        closingAge = -1;
        scrollAge = -1;
        scrollBase = 0;
        shiftedPage = false;
        retainedLine = "";
        pageStart = 0;
        visibleCharactersOnPage = 0;
        visibleNonSpaceCharactersOnPage = 0;
        revealTicks = 0;
        waitingForPageAdvance = false;
        soundEvents.clear();
        active = true;
    }

    public void tick() {
        if (active && closingAge >= 0) {
            if (++closingAge >= 8) finishClosing();
            return;
        }
        if (active && romTimed && openingAge < 10) {
            openingAge++;
            return;
        }
        // DialogBeginScrolling shifts eight pixels; DialogFinishScrolling
        // waits eight frames, then shifts the remaining eight pixels.
        if (active && scrollAge >= 0) {
            if (++scrollAge >= 10) scrollAge = -1;
            return;
        }
        if (active && waitingForPageAdvance) pageWaitAge++;
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
            int soundClock = romTimed ? visibleCharactersOnPage : visibleNonSpaceCharactersOnPage;
            if ((soundClock & 0x01) == 1) {
                soundEvents.add(SoundEvent.TEXT_PRINT);
            }
        }
        // The ROM also scrolls a full odd final line before checking its terminator.
        if (romTimed && shiftedPage && visibleCharactersOnPage == lineWidth && currentPageLength() >= lineWidth) {
            beginScroll(visibleText());
            scrollBase = lineWidth;
        }
        enterPageWaitIfNeeded();
    }

    private void beginScroll(String previous) {
        scrollPrevious = previous;
        retainedLine = previous.substring(previous.lastIndexOf('\n') + 1);
        scrollAge = 0;
        revealTicks = 0;
    }

    public int textScrollOffset() {
        return scrollAge > 0 ? 8 : 0;
    }

    public int characterIndex() {
        return pageStart + visibleCharactersOnPage;
    }

    public void setInteractionLocked(boolean locked) {
        interactionLocked = locked;
    }

    public void advance() {
        if (interactionLocked) return;
        if (romTimed && (openingAge < 10 || scrollAge >= 0 || closingAge >= 0)) return;
        if (!active) {
            return;
        }
        if (choicePrompt) {
            closeDialog();
            return;
        }
        if (waitingForPageAdvance) {
            if (romTimed) {
                beginScroll(visibleText());
                shiftedPage = true;
                scrollBase = 0;
            }
            pageStart = nextPageStart();
            visibleCharactersOnPage = 0;
            visibleNonSpaceCharactersOnPage = 0;
            revealTicks = 0;
            waitingForPageAdvance = false;
        } else if (visibleCharactersOnPage < currentPageLength()) {
            if (romTimed) return;
            visibleCharactersOnPage = currentPageLength();
            enterPageWaitIfNeeded();
        } else if (isAtChoicePromptMarker()) {
            enterChoicePrompt();
        } else {
            closeDialog();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isWaitingForPageAdvance() {
        return waitingForPageAdvance && (!romTimed || pageWaitAge > 0);
    }

    public boolean isChoicePrompt() {
        return choicePrompt;
    }

    public int choiceSelectionIndex() {
        return choiceSelectionIndex;
    }

    public void moveChoiceSelection(int delta) {
        if (!choicePrompt || delta == 0) {
            return;
        }
        choiceSelectionIndex ^= 0x01;
        soundEvents.add(SoundEvent.MOVE_SELECTION);
    }

    public int dialogCooldown() {
        return dialogCooldown;
    }

    public void tickGameplayFrameCooldown() {
        if (dialogCooldown > 0) {
            dialogCooldown--;
        }
    }

    public void openForLinkY(String text, int linkY) {
        open(text, linkY < LINK_Y_BOX_THRESHOLD ? BoxPosition.BOTTOM : BoxPosition.TOP);
    }

    private static final List<SoundEvent> TEXT_PRINT = List.of(SoundEvent.TEXT_PRINT);
    private static final List<SoundEvent> DIALOG_BREAK = List.of(SoundEvent.DIALOG_BREAK);
    private static final List<SoundEvent> MOVE_SELECTION = List.of(SoundEvent.MOVE_SELECTION);

    public BoxPosition boxPosition() {
        return boxPosition;
    }

    public int boxY() {
        return boxPosition.y();
    }

    public List<SoundEvent> consumeSoundEvents() {
        List<SoundEvent> events = soundEvents.size() == 1
                ? switch (soundEvents.getFirst()) {
                    case TEXT_PRINT -> TEXT_PRINT;
                    case DIALOG_BREAK -> DIALOG_BREAK;
                    case MOVE_SELECTION -> MOVE_SELECTION;
                }
                : List.copyOf(soundEvents);
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

    private void enterPageWaitIfNeeded() {
        if (!choicePrompt && visibleCharactersOnPage >= currentPageLength() && hasNextPage()) {
            waitingForPageAdvance = true;
            pageWaitAge = 0;
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

    private void closeDialog() {
        if (romTimed) {
            closingAge = 0;
            return;
        }
        finishClosing();
    }

    private void finishClosing() {
        active = false;
        choicePrompt = false;
        dialogCooldown = DIALOG_COOLDOWN;
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
    /** Named sound event values. */
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
    /** Named box position values. */
    public enum BoxPosition {
        TOP(8),
        // DialogOpenAnimationEnd writes rows $0A..$0E, above the HUD.
        BOTTOM(80);

        private final int y;

        BoxPosition(int y) {
            this.y = y;
        }

        public int y() {
            return y;
        }
    }

    private final DialogText textPreparation;

    public void setPlayerName(String playerName) {
        textPreparation.setPlayerName(playerName);
    }
}
