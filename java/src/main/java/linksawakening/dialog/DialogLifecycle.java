package linksawakening.dialog;

import static linksawakening.dialog.DialogController.*;

/** Dialog text, timing and presentation used by ROM {@code DialogDrawNextCharacterHandler}. */
abstract class DialogLifecycle extends DialogState {
    protected DialogLifecycle(int lineWidth) {
        super(lineWidth);
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

    protected void open(String text, BoxPosition boxPosition, boolean preformatted) {
        ParsedText parsedText = parse(text == null ? "" : text);
        this.text = preformatted ? formatPreformatted(parsedText.text()) : wrap(parsedText.text());
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

    protected void beginScroll(String previous) {
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
}
