package linksawakening.dialog;

import static linksawakening.dialog.DialogController.*;

import java.util.ArrayList;
import java.util.List;

/** Shared state and dependency ownership for DialogController. */
abstract class DialogState {

    protected static final int LINK_Y_BOX_THRESHOLD = 0x48;
    protected static final String DEFAULT_PLAYER_NAME = "LINK";
    protected static final int NAME_LENGTH = 5;
    protected static final int DIALOG_COOLDOWN = 0x18;
    protected static final char ASK_MARKER = '\u00FE';

    protected final int lineWidth;
    protected boolean romTimed;
    protected int openingAge;
    protected int closingAge = -1;
    protected int scrollAge = -1;
    protected int scrollBase;
    protected String scrollPrevious = "";
    protected String retainedLine = "";
    protected boolean shiftedPage;
    protected String text = "";
    protected String playerName = DEFAULT_PLAYER_NAME;
    protected int pageStart;
    protected int visibleCharactersOnPage;
    protected int visibleNonSpaceCharactersOnPage;
    protected int revealTicks;
    protected int dialogCooldown;
    protected boolean active;
    protected boolean interactionLocked;
    protected boolean waitingForPageAdvance;
    protected int pageWaitAge;
    protected boolean choicePrompt;
    protected int choiceSelectionIndex;
    protected BoxPosition boxPosition = BoxPosition.BOTTOM;
    protected final List<SoundEvent> soundEvents = new ArrayList<>();

    protected record ParsedText(String text) {}

    public DialogState(int lineWidth) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("lineWidth must be positive");
        }
        this.lineWidth = lineWidth;
    }

    /** Gameplay timing includes the source BG save/setup and five row writes. */
    public static DialogController forGameplay() {
        DialogController dialog = new DialogController(16);
        dialog.romTimed = true;
        return dialog;
    }

    protected static void appendSpace(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
            builder.append(' ');
        }
    }

    protected static String normalizeAskMarkers(String value) {
        return value.replace("<ask>", String.valueOf(ASK_MARKER));
    }

    protected static String stripAfterTerminator(String value) {
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

    public abstract boolean isBoxRowVisible(int row);

    public abstract void open(String text);

    public abstract void open(String text, BoxPosition boxPosition);

    public abstract void openPreformatted(String text);

    public abstract void openPreformatted(String text, BoxPosition boxPosition);

    public abstract void openPreformattedForLinkY(String text, int linkY);

    protected abstract void open(String text, BoxPosition boxPosition, boolean preformatted);

    public abstract void tick();

    protected abstract void beginScroll(String previous);

    public abstract int textScrollOffset();

    public abstract int characterIndex();

    public abstract void setInteractionLocked(boolean locked);

    public abstract void advance();

    public abstract boolean isActive();

    public abstract boolean isWaitingForPageAdvance();

    public abstract boolean isChoicePrompt();

    public abstract int choiceSelectionIndex();

    public abstract void moveChoiceSelection(int delta);

    public abstract int dialogCooldown();

    public abstract void tickGameplayFrameCooldown();

    public abstract void openForLinkY(String text, int linkY);

    public abstract BoxPosition boxPosition();

    public abstract int boxY();

    public abstract void setPlayerName(String playerName);

    public abstract List<SoundEvent> consumeSoundEvents();

    public abstract String visibleText();

    protected abstract void enterPageWaitIfNeeded();

    protected abstract int currentPageLength();

    protected abstract boolean hasNextPage();

    protected abstract int visibleEnd();

    protected abstract char charAtVisibleCell(int start, int cellOffset);

    protected abstract boolean isAtChoicePromptMarker();

    protected abstract void enterChoicePrompt();

    protected abstract void closeDialog();

    protected abstract void finishClosing();

    protected abstract int nextPageStart();

    protected abstract int pageEnd(int start);

    protected abstract String wrap(String value);

    protected abstract String formatPreformatted(String value);

    protected abstract int appendWord(StringBuilder wrapped, String word, int lineLength);

    protected abstract ParsedText parse(String rawText);

    protected abstract String substituteNameTokens(String value);

    protected abstract char nameCharacter(int index);
}
