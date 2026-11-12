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

## Intro

Original chain: IntroSequence → IntroSceneRendering → IntroSceneTransitions → IntroBeachMotion → IntroSceneMotion → IntroLifecycle → IntroSequenceState.

IntroSequence coordinates stage transitions; beach actor simulation owns its actor/timer state; palette processing owns palette sources/fades; sprite composition owns decoded tables, scratch and RNG. Hardware scroll/map writes are an explicit shared video boundary. Preserve pre-update snapshots and RNG call order.

| State/constant | Referencing methods |
| --- | --- |
| `snapshotBuilder` | `refreshSnapshot` |
| `renderSpriteScratch` | `renderSprites` |
| `correctionSpriteScratch` | `tickBeach` |
| `INITIAL_SHIP_X` |  |
| `INITIAL_SHIP_Y` |  |
| `LINK_FACE_SCREAM_FRAME` | `tickLinkFace`, `substate` |
| `LINK_FACE_LIGHTNING_FRAME` | `tickLinkFace`, `substate` |
| `BEACH_MARIN_START_X` | `tickBeachFade` |
| `BEACH_LINK_START_X` | `tickMarin` |
| `BEACH_LINK_Y` |  |
| `SEA_LIGHTNING_SCROLLS` | `triggerSeaLightningIfNeeded` |
| `rom` | `IntroSequenceState`, `loadBundledRom`, `tickLinkFace`, `tickInertLink`, `patchMountains`, `animateWaves`, `transitionTo`, `renderTitleSparkle`, `lineScrollYMap` |
| `backgroundProvider` | `IntroSequenceState`, `IntroSequenceState`, `IntroLifecycle`, `IntroSceneMotion`, `IntroBeachMotion`, `IntroSceneTransitions`, `setScene`, `IntroSceneRendering`, `IntroSequence` |
| `random` | `IntroSequenceState`, `tick`, `renderRain` |
| `titleRows` | `IntroSequenceState`, `skipToTitle`, `tickTitleReveal` |
| `marinVariants` | `IntroSequenceState`, `tickBeach`, `renderBeachEntities` |
| `inertLinkVariants` | `IntroSequenceState`, `renderBeachEntities` |
| `sparkleVariants` | `IntroSequenceState`, `renderTitleSparkle` |
| `lightningTiles` | `IntroSequenceState`, `renderLightning` |
| `lightningPositions` | `IntroSequenceState`, `triggerLightning` |
| `lightningStatuses` | `IntroSequenceState`, `triggerLightning` |
| `shipHeave` | `IntroSequenceState`, `renderShip` |
| `verticalOffsets` | `IntroSequenceState`, `transitionTo`, `updateWaveOffsets` |
| `colorModifiers` | `IntroSequenceState`, `dynamicBgPalettes` |
| `dxFadeInPalette` | `IntroSequenceState`, `dynamicObjPalettes` |
| `shipTiles` | `IntroSequenceState`, `renderShip` |
| `additionalShipTiles` | `IntroSequenceState`, `renderShip` |
| `stage` | `tickSeaWaveSound`, `tick`, `isActive`, `skipToTitle`, `sceneId`, `tickBeach`, `tickTitleDx`, `transitionTo`, `refreshSnapshot`, `substate`, `renderSprites`, `renderShip`, `lineScrollMap`, `lineScrollYMap`, `dynamicBgPalettes`, `dynamicObjPalettes` |
| `frameCounter` | `tick`, `tickSea`, `refreshSnapshot` |
| `seaWaveCounter` | `tickSeaWaveSound` |
| `noiseSound` | `setNoiseSound`, `tickSeaWaveSound`, `playThunder` |
| `stageFrame` | `tick`, `skipToTitle`, `sceneId`, `tickTitleReveal`, `transitionTo`, `refreshSnapshot` |
| `clock` | `tick`, `tickSea`, `tickLinkFace`, `tickBeachFade`, `tickMarin`, `tickInertLink`, `updateBeachOffsets`, `animateWaves`, `updateWaveOffsets`, `renderShip`, `renderTitleSparkle` |
| `loadDelay` | `tick`, `tickSea`, `tickLinkFace`, `tickSeaFade` |
| `fractionD` | `updateBeachOffsets` |
| `linkVariant` | `tickMarin`, `renderBeachEntities` |
| `marinY` | `tickBeach`, `tickInertLink`, `renderBeachEntities` |
| `panRows` | `tickInertLink` |
| `waveTiles` | `IntroSequenceState`, `tick`, `animateWaves`, `transitionTo`, `refreshSnapshot` |
| `waveDestination` | `transitionTo`, `refreshSnapshot` |
| `skipped` | `tick`, `skipToTitle`, `refreshSnapshot`, `renderSprites` |
| `fadeSteps` | `tickSeaFade`, `tickBeachFade`, `transitionTo` |
| `fadeObj` | `tickSeaFade`, `tickBeachFade`, `transitionTo`, `dynamicObjPalettes` |
| `visibleObj` | `tick`, `skipToTitle`, `refreshSnapshot` |
| `visibleWave` | `tick`, `transitionTo`, `refreshSnapshot` |
| `scrollX` | `tick`, `skipToTitle`, `scrollX`, `scrollXForLine`, `tickSea`, `transitionTo`, `triggerSeaLightningIfNeeded`, `refreshSnapshot`, `lineScrollMap` |
| `scrollY` | `tick`, `skipToTitle`, `scrollY`, `tickInertLink`, `refreshSnapshot` |
| `verticalWaveOffset` | `skipToTitle`, `transitionTo`, `updateWaveOffsets`, `refreshSnapshot`, `lineScrollMap`, `lineScrollYMap` |
| `shipX` | `shipX`, `tickSea`, `renderShip` |
| `shipY` | `renderShip` |
| `seaFadeTimer` | `tickLinkFace`, `tickSeaFade`, `renderShip` |
| `beachFadeTimer` | `tickSeaFade`, `tickBeachFade` |
| `linkFaceTimer` | `tickLinkFace`, `transitionTo`, `substate` |
| `marinState` | `tickBeachFade`, `tickMarin`, `substate`, `lineScrollMap` |
| `marinX` | `tickBeachFade`, `tickBeach`, `tickMarin`, `renderBeachEntities` |
| `marinInertia` | `tickBeachFade`, `tickMarin` |
| `marinTimer` | `tickBeachFade`, `tickMarin` |
| `linkX` | `tickMarin`, `renderBeachEntities` |
| `linkVisible` | `skipToTitle`, `tickBeachFade`, `tickBeach`, `tickMarin`, `renderBeachEntities` |
| `beachScrollX` | `skipToTitle`, `scrollX`, `tickMarin`, `patchMountains`, `transitionTo`, `lineScrollMap` |
| `titleRowIndex` | `skipToTitle`, `tickTitleReveal`, `transitionTo` |
| `titleRevealRows` | `skipToTitle`, `titleRevealRows`, `tickTitleReveal`, `transitionTo`, `refreshSnapshot` |
| `titleHoldFrame` | `tickTitleDx`, `transitionTo`, `renderTitleSparkle`, `dynamicObjPalettes` |
| `titleEffectFrame` | `tick`, `lineScrollYMap` |
| `visibleScrollY` | `tick`, `skipToTitle`, `refreshSnapshot`, `lineScrollYMap` |
| `topLineScroll` | `tick`, `lineScrollMap` |
| `drawCommandQueued` | `tick`, `tickInertLink`, `animateWaves` |
| `beachScrollStoppedThisFrame` | `tick`, `tickMarin`, `lineScrollMap` |
| `paletteLightningCountdown` | `decrementLightning`, `dynamicBgPalettes` |
| `visibleAttrmap` | `tick`, `refreshSnapshot`, `setScene` |
| `sectionScrollOffsets` | `tick`, `updateBeachOffsets`, `transitionTo`, `updateWaveOffsets`, `refreshSnapshot`, `lineScrollMap` |
| `lightning` | `skipToTitle`, `tickSea`, `tickLinkFace`, `decrementLightning`, `triggerLightning`, `renderLightning` |
| `lightningVisibleCountdown` | `decrementLightning`, `playThunder` |
| `tilemap` | `tick`, `tickLinkFace`, `tickInertLink`, `patchMountains`, `transitionTo`, `refreshSnapshot`, `setScene`, `applyTitleRow` |
| `attrmap` | `tick`, `tickInertLink`, `transitionTo`, `refreshSnapshot`, `setScene`, `applyTitleRow` |
| `bgPalettes` | `tickSeaFade`, `tickBeachFade`, `transitionTo`, `refreshSnapshot`, `setScene`, `dynamicBgPalettes` |
| `objPalettes` | `tickSeaFade`, `tickBeachFade`, `transitionTo`, `refreshSnapshot`, `setScene`, `dynamicObjPalettes` |
| `sprites` | `tick`, `skipToTitle`, `sprites`, `refreshSnapshot` |
| `pendingSprites` | `tick`, `tickBeach` |
| `snapshot` | `scrollXForLine`, `lineScrollX`, `sprites`, `snapshot`, `transitionTo`, `refreshSnapshot` |
| `transitionFrame` | `transitionTo`, `refreshSnapshot` |
| `SEA_SCROLL_FRAMES` |  |
| `LINK_FACE_FRAMES` | `tickLinkFace` |
| `SEA_FADE_FRAMES` |  |
| `BEACH_FADE_FRAMES` | `tickSeaFade` |
| `BEACH_MARIN_WALK_FRAMES` |  |
| `BEACH_MARIN_WAIT_FRAMES` |  |
| `TITLE_REVEAL_STEP_FRAMES` | `tickTitleReveal` |
| `TITLE_HOLD_FRAMES` | `tickTitleDx` |

Intro result: project inheritance depth 6 → 0. IntroSequence (484 lines) coordinates
stages; IntroBeachActors owns Marin/Link state machines; IntroWater owns fractional
scroll and animated tiles; IntroSpriteComposer owns OAM tables, scratch and rain RNG;
IntroFramePublisher owns palette sources and delayed immutable snapshots. IntroClock,
IntroPhaseState and IntroVideoMemory are explicit small shared state objects with
private fields. IntroPaletteMath implements RGB555 algorithms on supplied color data;
IntroResources supplies the compatibility resource-loading entry point. None receives
IntroSequence or a whole-owner state object. New ROM-free actor tests exercise cadence,
clock reset and the title-transition gate independently. Default suite: 2,884 → 2,886
tests, zero failures/errors, one existing skip.
