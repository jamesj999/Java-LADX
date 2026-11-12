# Composition redesign

Field-use inventories below describe the pre-refactor chains. Method names are lexical references reviewed together with their bodies; parameter names may overlap field names.

## Dialog

Original chain: DialogController → DialogTextFormatting → DialogPaging → DialogLifecycle → DialogState.

DialogText owns name substitution and width-dependent formatting; DialogController owns the single reveal/scroll/choice state machine and queued sound requests. No collaborator receives the controller or its whole state.

| State/constant | Referencing methods |
| --- | --- |
| `LINK_Y_BOX_THRESHOLD` | `openPreformattedForLinkY`, `openForLinkY` |
| `DEFAULT_PLAYER_NAME` | `setPlayerName` |
| `NAME_LENGTH` | `substituteNameTokens` |
| `DIALOG_COOLDOWN` | `finishClosing` |
| `ASK_MARKER` | `normalizeAskMarkers`, `tick`, `currentPageLength`, `hasNextPage`, `visibleEnd`, `isAtChoicePromptMarker`, `formatPreformatted` |
| `lineWidth` | `DialogState`, `DialogLifecycle`, `tick`, `DialogPaging`, `wrap`, `DialogTextFormatting`, `formatPreformatted`, `appendWord`, `DialogController` |
| `romTimed` | `forGameplay`, `isBoxRowVisible`, `tick`, `advance`, `isWaitingForPageAdvance`, `visibleText`, `closeDialog` |
| `openingAge` | `isBoxRowVisible`, `open`, `tick`, `advance` |
| `closingAge` | `isBoxRowVisible`, `open`, `tick`, `advance`, `closeDialog` |
| `scrollAge` | `open`, `tick`, `beginScroll`, `textScrollOffset`, `advance`, `visibleText` |
| `scrollBase` | `open`, `tick`, `advance`, `visibleText` |
| `scrollPrevious` | `beginScroll`, `visibleText` |
| `retainedLine` | `open`, `beginScroll`, `visibleText` |
| `shiftedPage` | `open`, `tick`, `advance`, `visibleText` |
| `text` | `open`, `open`, `openPreformatted`, `openPreformatted`, `openPreformattedForLinkY`, `open`, `openForLinkY`, `visibleText`, `currentPageLength`, `hasNextPage`, `visibleEnd`, `charAtVisibleCell`, `isAtChoicePromptMarker`, `nextPageStart`, `pageEnd` |
| `playerName` | `setPlayerName`, `nameCharacter` |
| `pageStart` | `open`, `tick`, `characterIndex`, `advance`, `visibleText`, `currentPageLength`, `visibleEnd`, `isAtChoicePromptMarker`, `nextPageStart` |
| `visibleCharactersOnPage` | `open`, `tick`, `characterIndex`, `advance`, `enterPageWaitIfNeeded`, `visibleEnd`, `isAtChoicePromptMarker` |
| `visibleNonSpaceCharactersOnPage` | `open`, `tick`, `advance` |
| `revealTicks` | `open`, `tick`, `beginScroll`, `advance` |
| `dialogCooldown` | `dialogCooldown`, `tickGameplayFrameCooldown`, `finishClosing` |
| `active` | `open`, `tick`, `advance`, `isActive`, `finishClosing` |
| `interactionLocked` | `setInteractionLocked`, `advance` |
| `waitingForPageAdvance` | `open`, `tick`, `advance`, `isWaitingForPageAdvance`, `enterPageWaitIfNeeded`, `enterChoicePrompt` |
| `pageWaitAge` | `tick`, `isWaitingForPageAdvance`, `enterPageWaitIfNeeded` |
| `choicePrompt` | `open`, `advance`, `isChoicePrompt`, `moveChoiceSelection`, `enterPageWaitIfNeeded`, `enterChoicePrompt`, `finishClosing` |
| `choiceSelectionIndex` | `open`, `choiceSelectionIndex`, `moveChoiceSelection` |
| `boxPosition` | `open`, `openPreformatted`, `open`, `boxPosition`, `boxY` |
| `soundEvents` | `open`, `tick`, `moveChoiceSelection`, `consumeSoundEvents`, `enterPageWaitIfNeeded`, `enterChoicePrompt` |
| `TEXT_PRINT` | `tick`, `consumeSoundEvents` |
| `DIALOG_BREAK` | `consumeSoundEvents`, `enterPageWaitIfNeeded`, `enterChoicePrompt` |
| `MOVE_SELECTION` | `moveChoiceSelection`, `consumeSoundEvents` |
