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

## Entity sprite catalog

Original chain: EntitySpriteHandlerCatalog → EntitySpriteDecodeGeometry → EntitySpriteRomDecoding → EntitySpriteDynamicComposites → EntitySpriteBossComposites → EntitySpriteLookup → EntitySpriteDecodeState.

Immutable registration recipes are selected without a ROM or catalog reference. EntitySpriteDecoder owns ROM reads and validation; boss, object-state and effect composers receive that decoder only (object states also receive registration data). The public catalog composes these pieces.

| State/constant | Referencing methods |
| --- | --- |
| `ENTITY_ARROW` | `forBombArrow` |
| `ENTITY_BOOMERANG` |  |
| `ENTITY_MAGIC_ROD_FIREBALL` | `forMagicRodFireState` |
| `ENTITY_MAGIC_POWDER_SPRINKLE` | `forMagicPowderTorchState` |
| `ENTITY_MUSICAL_NOTE` |  |
| `ENTITY_SWORD_BEAM` |  |
| `ENTITY_BOMB` | `forBombRightBeforeExploding`, `forBombExplosion`, `forBombArrow` |
| `ENTITY_BUTTERFLY` |  |
| `ENTITY_HOOKSHOT_CHAIN` |  |
| `ENTITY_CRYSTAL_SWITCH` |  |
| `ENTITY_OCTOROK` |  |
| `ENTITY_OCTOROK_ROCK` |  |
| `ENTITY_MOBLIN` |  |
| `ENTITY_IRON_MASK` | `forIronMaskState` |
| `ENTITY_MOBLIN_ARROW` |  |
| `ENTITY_BOUNCING_BOMBITE` |  |
| `ENTITY_TIMER_BOMBITE` |  |
| `ENTITY_MAD_BOMBER` |  |
| `ENTITY_BOMBER` |  |
| `ENTITY_MOBLIN_SWORD` |  |
| `ENTITY_TEKTITE` |  |
| `ENTITY_LEEVER` |  |
| `ENTITY_ANTI_FAIRY` |  |
| `ENTITY_SPARK_COUNTER_CLOCKWISE` |  |
| `ENTITY_SPARK_CLOCKWISE` |  |
| `ENTITY_ZOL` |  |
| `ENTITY_GEL` |  |
| `ENTITY_HIDING_ZOL` |  |
| `ENTITY_STALFOS_AGGRESSIVE` |  |
| `ENTITY_STALFOS_EVASIVE` | `forStalfosEvasiveState` |
| `ENTITY_GIBDO` |  |
| `ENTITY_LIKE_LIKE` |  |
| `ENTITY_PEAHAT` |  |
| `ENTITY_WIZROBE` |  |
| `ENTITY_WIZROBE_PROJECTILE` |  |
| `ENTITY_ARMOS_STATUE` |  |
| `ENTITY_ARMOS_KNIGHT` |  |
| `ENTITY_HIDING_GHINI` |  |
| `ENTITY_GIANT_GHINI` |  |
| `ENTITY_GHINI` |  |
| `ENTITY_POLS_VOICE` |  |
| `ENTITY_KEESE` |  |
| `ENTITY_GOOMBA` |  |
| `ENTITY_SNAKE` |  |
| `ENTITY_HARDHAT_BEETLE` |  |
| `ENTITY_SPIKED_BEETLE` |  |
| `ENTITY_LASER` |  |
| `ENTITY_LASER_BEAM` |  |
| `ENTITY_SPIKE_TRAP` |  |
| `ENTITY_PAIRODD` |  |
| `ENTITY_PAIRODD_PROJECTILE` |  |
| `ENTITY_WATER_TEKTITE` |  |
| `ENTITY_FISH` |  |
| `ENTITY_BOW_WOW` | `forFollowerEntityType` |
| `ENTITY_DOG` |  |
| `ENTITY_KID_70` |  |
| `ENTITY_KID_71` |  |
| `ENTITY_KID_72` |  |
| `ENTITY_KID_73` |  |
| `ENTITY_MADAM_MEOWMEOW` |  |
| `ENTITY_CROW` |  |
| `ENTITY_BOO_BUDDY` |  |
| `ENTITY_DROPPABLE_HEART` |  |
| `ENTITY_DROPPABLE_RUPEE` |  |
| `ENTITY_DROPPABLE_FAIRY` |  |
| `ENTITY_SWORD_SHIELD_PICKUP` | `forSwordShieldPickup` |
| `ENTITY_IRON_MASKS_MASK` |  |
| `ENTITY_PIECE_OF_POWER` |  |
| `ENTITY_GUARDIAN_ACORN` |  |
| `ENTITY_HEART_PIECE` |  |
| `ENTITY_HEART_CONTAINER` |  |
| `ENTITY_DROPPABLE_ARROWS` |  |
| `ENTITY_DROPPABLE_BOMBS` |  |
| `ENTITY_SLEEPY_TOADSTOOL` |  |
| `ENTITY_DROPPABLE_MAGIC_POWDER` |  |
| `ENTITY_HIDING_SLIME_KEY` |  |
| `ENTITY_DROPPABLE_SECRET_SEASHELL` |  |
| `ENTITY_FLOATING_ITEM` | `forFloatingItemOverlay` |
| `ENTITY_FLOATING_ITEM_2` | `decodeFloatingItemMain` |
| `ENTITY_MARIN` |  |
| `ENTITY_TARIN` |  |
| `ENTITY_GRANDPA_ULRIRA` |  |
| `ENTITY_MARIN_AT_THE_SHORE` | `forFollowerEntityType` |
| `ENTITY_MARIN_AT_TAL_TAL_HEIGHTS` |  |
| `ENTITY_COLOR_SHELL_RED` | `isColorShellType`, `forColorShellState` |
| `ENTITY_COLOR_SHELL_BLUE` | `isColorShellType` |
| `COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES` | `forColorShellState` |
| `COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES` | `forColorShellState` |
| `romData` | `EntitySpriteDecodeState`, `EntitySpriteLookup`, `EntitySpriteBossComposites`, `EntitySpriteDynamicComposites`, `EntitySpriteRomDecoding`, `decodePair`, `decodeSingle`, `EntitySpriteDecodeGeometry`, `decodeRectangleVariants`, `validateDisplayList`, `readByte`, `EntitySpriteHandlerCatalog`, `EntitySpriteHandlerCatalog` |
| `ENTITY_LIFTABLE_ROCK` | `decodeLiftableRock`, `appendDynamicRectangleVariants` |
| `ENTITY_CHEST_WITH_ITEM` | `forChestState` |
| `ENTITY_KEY_DROP_POINT` |  |
| `ENTITY_STAR` |  |
| `ENTITY_BLOOPER` |  |
| `ENTITY_WINGED_OCTOROK` |  |
| `ENTITY_PINCER` |  |
| `ENTITY_BUSH_CRAWLER` | `forBushCrawlerState`, `forBushCrawlerCrawlState` |
| `ENTITY_MIMIC` |  |
| `ENTITY_MINI_MOLDORM` | `forMiniMoldormState` |
| `ENTITY_MOLDORM` | `forMoldormPresentation` |
| `ENTITY_ROLLING_BONES` |  |
| `ENTITY_ROLLING_BONES_BAR` | `forRollingBonesBar` |
| `ENTITY_THREE_OF_A_KIND` |  |
| `ENTITY_MASKED_MIMIC_GORIYA` |  |
| `ENTITY_HINOX` |  |
| `ENTITY_CUCCO` |  |
| `ENTITY_GOPONGA_FLOWER` |  |
| `ENTITY_GIANT_GOPONGA_FLOWER` |  |
| `ENTITY_GOPONGA_FLOWER_PROJECTILE` |  |
| `ENTITY_POKEY` | `forPokeyState`, `forPokeySegment` |
| `ENTITY_PIRANHA_PLANT` |  |
| `ENTITY_ZORA` |  |
| `ENTITY_ZOMBIE` |  |
| `ENTITY_BUZZ_BLOB` |  |
| `ENTITY_SAND_CRAB` |  |
| `ENTITY_URCHIN` | `forUrchinState` |
| `ENTITY_WITCH_RAT` |  |
| `ENTITY_WITCH` |  |
| `LIFTABLE_ROCK_INTACT_ROCK_VARIANT` | `decodeLiftableRock` |
| `LIFTABLE_ROCK_INTACT_BUSH_VARIANT` | `decodeLiftableRock` |
| `LIFTABLE_ROCK_SMASHED_ROCK_VARIANT_BASE` |  |
| `LIFTABLE_ROCK_CUT_LEAVES_VARIANT_BASE` |  |
| `LIFTABLE_ROCK_CUT_LEAVES_SWAMP_VARIANT_BASE` |  |
| `ENTITY_INSTRUMENT_OF_THE_SIRENS` | `forInstrumentShard` |

Result: project inheritance depth 6 → 0; catalog facade 411 lines. Immutable family recipes and their indexed registration data are independent of ROM decoding. EntitySpriteDecoder owns the ROM; EntitySpriteStates, BossSpriteComposer and EntityEffectSprites compose that decoder for state selection, boss geometry and transient effects respectively. EntitySpriteGeometry contains only coordinate algorithms. Two ROM-free tests cover registration priority/arbitrary map selectors and bounded OAM decoding. Full suite: 2,888 tests, zero failures/errors, one skipped.

## File menu

Original chain: FileMenuController → FileMenuEditing → FileMenuFlow → FileMenuState.

Name-entry character selection, insertion position and five-byte buffer belong to FileNameEntry. FileMenuController owns slot/copy/erase transitions and snapshot publication; those operations are cohesive and are merged rather than keeping the former flow/editing slices.

| State/constant | Referencing methods |
| --- | --- |
| `snapshotBuilder` | `refreshSnapshot` |
| `spriteScratch` | `buildSprites` |
| `NAME_LENGTH` | `copySavedNames`, `tickSelection`, `tickCreation`, `drawSaveSlotName` |
| `NAME_ENTRY_CHARACTER_COUNT` | `tickCreation` |
| `BG_MAP_BASE` |  |
| `FILE_NEW_SAVE_SLOT_INDEX_OFFSET` | `refreshSnapshot` |
| `FILE_NEW_NAME_OFFSET` | `refreshSnapshot` |
| `SAVE_NAME_OFFSETS` | `refreshSnapshot` |
| `COPY_SOURCE_NAME_OFFSETS` | `refreshSnapshot` |
| `COPY_TARGET_NAME_OFFSETS` | `refreshSnapshot` |
| `QUIT_OK_OFFSET` | `refreshSnapshot` |
| `FILE_NEW_SAVE_SLOT_1_TILE` | `refreshSnapshot` |
| `DARK_BACKGROUND_TILE` | `drawSaveSlotName`, `tileForStoredName` |
| `sceneProvider` | `FileMenuState`, `FileMenuState`, `FileMenuFlow`, `FileMenuFlow`, `FileMenuEditing`, `FileMenuEditing`, `refreshSnapshot`, `FileMenuController`, `FileMenuController` |
| `sceneCache` | `refreshSnapshot` |
| `selectionCursorYPositions` | `FileMenuState`, `buildSprites`, `addCursorPair` |
| `nameEntryCharacterTable` | `FileMenuState`, `tickCreation` |
| `nameCursorYPositions` | `FileMenuState`, `buildSprites` |
| `nameCursorXPositions` | `FileMenuState`, `buildSprites` |
| `namePositionXPositions` | `FileMenuState`, `buildSprites` |
| `codepointToTileMap` | `FileMenuState`, `tileForStoredName` |
| `saveFilesMask` | `FileMenuState`, `FileMenuState`, `FileMenuFlow`, `FileMenuFlow`, `FileMenuEditing`, `FileMenuEditing`, `refreshSnapshot`, `hasSavedFile`, `FileMenuController`, `FileMenuController` |
| `savedNames` | `FileMenuState`, `FileMenuState`, `FileMenuFlow`, `FileMenuFlow`, `tickSelection`, `tickErasePick`, `tickCopySource`, `FileMenuEditing`, `FileMenuEditing`, `refreshSnapshot`, `FileMenuController`, `FileMenuController` |
| `maxHearts` | `FileMenuState`, `FileMenuFlow`, `FileMenuEditing`, `refreshSnapshot`, `FileMenuController` |
| `deathCountsBcd` | `FileMenuState`, `FileMenuFlow`, `FileMenuEditing`, `refreshSnapshot`, `FileMenuController` |
| `deathDigitTiles` | `FileMenuState`, `refreshSnapshot` |
| `quitOkTiles` | `FileMenuState`, `refreshSnapshot` |
| `selectedSlot` | `tickSelection`, `tickErasePick`, `tickEraseConfirm`, `tickCopySource`, `tickCopyTarget`, `tickCopyConfirm`, `moveFourRowCursor`, `returnToSelection`, `tickCreation`, `refreshSnapshot`, `buildSprites` |
| `commandRowArrowShifted` | `tickSelection`, `refreshSnapshot`, `buildSprites` |
| `mode` | `mode`, `tick`, `tickSelection`, `tickErasePick`, `tickEraseConfirm`, `tickCopySource`, `tickCopyTarget`, `tickCopyConfirm`, `returnToSelection`, `refreshSnapshot`, `buildSprites` |
| `sourceSlot` | `tickErasePick`, `tickEraseConfirm`, `tickCopySource`, `tickCopyTarget`, `tickCopyConfirm`, `refreshSnapshot`, `buildSprites` |
| `targetSlot` | `tickCopySource`, `tickCopyTarget`, `tickCopyConfirm`, `refreshSnapshot`, `buildSprites` |
| `confirmationOk` | `tickErasePick`, `tickEraseConfirm`, `tickCopyTarget`, `tickCopyConfirm`, `toggleConfirmation`, `buildSprites` |
| `selectedCharacter` | `tickSelection`, `tickCreation`, `refreshSnapshot`, `buildSprites` |
| `namePosition` | `tickSelection`, `tickCreation`, `refreshSnapshot`, `buildSprites` |
| `frameCounter` | `tick`, `refreshSnapshot`, `buildSprites`, `addCursorPair` |
| `currentName` | `tickSelection`, `tickCreation`, `refreshSnapshot` |
| `snapshot` | `snapshot`, `refreshSnapshot` |

Result: depth 3 → 0; four chain files / 606 lines → two final classes / 543 lines (controller 475, name editor 68). All state is private. Two independent name-entry tests cover wrap/reset, START priority and action-copy ownership. Full suite: 2,890 tests, zero failures/errors, one skipped.

## Link motion dispatch

Original chain: LinkStateMachine → LinkBootsMotion → LinkRoomMotion → LinkFrameMotion → LinkMotionLifecycle.

Frame dispatch remains in LinkStateMachine. Pegasus charge/collision/shake state belongs to LinkDash; room identity/physics belongs to LinkRoomPhysics; revolving-door animation/transition state belongs to LinkDoorTraversal. LinkMotionStatus is the explicit small shared state for motion mode, recovery and frame gates. Existing airborne, swimming, equipment and animation components remain separate collaborators.

| State/constant | Referencing methods |
| --- | --- |
| `dialogGotItemSequence` | `LinkMotionLifecycle`, `resetTransientStateForDebug` |
| `linkAirborne` | `LinkMotionLifecycle`, `beginFrame`, `advanceSpecialMotion`, `advanceHazardMotion`, `prepareGroundMotion`, `resolveSpeeds`, `applyMotion`, `updatePegasusBootsUse`, `handlePegasusBootsCollision`, `setDebugNoClip`, `resetTransientStateForDebug`, `finishRoomScroll` |
| `linkAnimationState` | `LinkMotionLifecycle`, `beginFrame`, `advanceSpecialMotion`, `advanceHazardMotion`, `prepareGroundMotion`, `resolveSpeeds`, `applyMotion`, `tickRevolvingDoor`, `updateDebugNoClip`, `pegasusRunningSpeedX`, `pegasusRunningSpeedY`, `handlePegasusBootsCollision`, `resetTransientStateForDebug` |
| `linkAnimator` | `LinkMotionLifecycle`, `beginFrame`, `prepareGroundMotion`, `applyMotion`, `tickRevolvingDoor`, `resetTransientStateForDebug` |
| `linkControls` | `LinkMotionLifecycle`, `prepareGroundMotion`, `updateDebugNoClip` |
| `linkDamage` | `LinkMotionLifecycle`, `beginFrame`, `advanceHazardMotion`, `prepareGroundMotion`, `applyMotion`, `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom` |
| `linkEntityEffects` | `LinkMotionLifecycle`, `advanceHazardMotion`, `prepareGroundMotion`, `resolveSpeeds`, `applyMotion`, `updatePegasusBootsUse`, `resetTransientStateForDebug` |
| `linkEquipment` | `LinkMotionLifecycle`, `beginFrame`, `prepareGroundMotion` |
| `linkGroundMotion` | `LinkMotionLifecycle`, `applyMotion`, `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom`, `accessSetGelClinging` |
| `linkItemState` | `LinkMotionLifecycle`, `resetTransientStateForDebug` |
| `linkMotion` | `LinkMotionLifecycle`, `beginFrame`, `advanceSpecialMotion`, `advanceHazardMotion`, `resolveSpeeds`, `applyMotion`, `tickRevolvingDoor`, `updateDebugNoClip`, `updatePegasusBootsUse`, `handlePegasusBootsCollision`, `resetTransientStateForDebug`, `finishRoomScroll` |
| `linkServices` | `LinkMotionLifecycle`, `advanceSpecialMotion`, `resolveSpeeds`, `tickRevolvingDoor`, `groundSpeedIndex`, `updateDebugNoClip`, `updatePegasusBootsUse`, `handlePegasusBootsCollision`, `finishRoomScroll`, `resetPegasusBoots` |
| `linkSideView` | `LinkMotionLifecycle`, `beginFrame`, `advanceSpecialMotion`, `resetTransientStateForDebug` |
| `linkSignals` | `LinkMotionLifecycle`, `advanceHazardMotion`, `resetTransientStateForDebug` |
| `linkSwimming` | `LinkMotionLifecycle`, `advanceSpecialMotion`, `advanceHazardMotion`, `applyMotion` |
| `motionState` | `beginFrame`, `advanceSpecialMotion`, `advanceHazardMotion`, `tickRevolvingDoor`, `updatePegasusBootsUse`, `resetTransientStateForDebug`, `finishRoomScroll`, `accessIsSwimming` |
| `physicsModifier` | `advanceHazardMotion`, `resetTransientStateForDebug`, `finishRoomScroll`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom`, `setRomPhysicsModifier`, `accessIsDiving`, `accessRomPhysicsModifier` |
| `recoverCountdown` | `resetTransientStateForDebug` |
| `recoveringInWater` | `resetTransientStateForDebug` |
| `slowWalkingSpeed` | `accessRomSlowWalkingSpeed` |
| `itemUsageContext` | `accessRomItemUsageContext` |
| `revolvingDoorFrame` | `tickRevolvingDoor`, `resetTransientStateForDebug` |
| `revolvingDoorTransitionPending` | `tickRevolvingDoor`, `resetTransientStateForDebug`, `accessConsumeRevolvingDoorTransition` |
| `revolvingDoorPaletteVariant` | `tickRevolvingDoor`, `resetTransientStateForDebug` |
| `romInteractiveMotionBlocked` | `beginFrame`, `updatePegasusBootsUse`, `resetTransientStateForDebug`, `writeRomInteractiveMotionBlock` |
| `romInteractiveMotionBlockValue` | `beginFrame`, `resetTransientStateForDebug`, `writeRomInteractiveMotionBlock`, `accessRomInteractiveMotionBlockValue` |
| `physicsRoomLoadToken` | `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom` |
| `physicsRoomMapId` | `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom` |
| `physicsRoomId` | `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom` |
| `physicsRoomCategory` | `resetTransientStateForDebug`, `initializeRoomPhysicsForRoom`, `setRoomPhysicsForRoom` |
| `debugNoClip` | `advanceSpecialMotion`, `setDebugNoClip`, `resetTransientStateForDebug`, `finishRoomScroll`, `accessDebugNoClip` |
| `frameCounter` | `beginFrame`, `advanceSpecialMotion` |
| `gelClinging` | `applyMotion`, `accessSetGelClinging` |
| `gelMotionFrame` | `applyMotion`, `accessSetGelClinging` |
| `pegasusBootsChargeMeter` | `resolveSpeeds`, `updatePegasusBootsUse`, `resetPegasusBoots`, `accessPegasusBootsChargeMeter` |
| `pegasusBootsCollisionCountdown` | `beginFrame`, `handlePegasusBootsCollision`, `accessPegasusBootsCollisionCountdown` |
| `pegasusBootsCollisionPosX` | `handlePegasusBootsCollision`, `accessPegasusBootsCollisionPosX` |
| `pegasusBootsCollisionPosY` | `handlePegasusBootsCollision`, `accessPegasusBootsCollisionPosY` |
| `pendingPegasusScreenShakeCountdown` | `handlePegasusBootsCollision`, `consumePegasusScreenShakeRequest` |
| `pendingPegasusScreenShakePhase` | `handlePegasusBootsCollision`, `consumePegasusScreenShakeRequest` |
| `frame` | `beginFrame`, `advanceSpecialMotion`, `advanceHazardMotion`, `prepareGroundMotion`, `resolveSpeeds`, `applyMotion`, `blockNextRomMotionFrame`, `consumePegasusScreenShakeRequest` |

Result: depth 4 → 0; LinkStateMachine is a 489-line dispatcher. LinkDash owns charge/rebound/shake, LinkRoomPhysics owns room-identity defaults and scroll completion, LinkDoorTraversal owns door timing/palette/transition, and LinkMotionStatus holds explicit shared mode/recovery/frame gates. Every state field in these components is private; no component receives LinkStateMachine or Link. Assembly-only dependency wiring lives in LinkAssembly. Full suite: 2,891 tests, zero failures/errors, one skipped; one independent interaction-byte validation test added.

## Link airborne motion

Original chain: LinkAirborne → LinkJumpMotion → LinkAirborneState.

Jump steering, gravity, landing and scripted jump commands are one responsibility in LinkAirborne. Pit probing/slip targets/fall duration/warp-or-damage decisions belong to LinkPitFall. LinkElevation is the small explicit airborne/Z/speed state shared by both; ledge steering stays private to LinkAirborne.

| State/constant | Referencing methods |
| --- | --- |
| `dialogGotItemSequence` | `LinkAirborneState`, `accessTickDialogGotItem` |
| `linkAnimationState` | `LinkAirborneState`, `startPitFall`, `accessUseRocsFeather`, `accessTickDialogGotItem` |
| `linkAnimator` | `LinkAirborneState`, `tickJump`, `tickJumpAnimation`, `accessUseRocsFeather`, `accessTickDialogGotItem` |
| `linkControls` | `LinkAirborneState`, `tickJump` |
| `linkDamage` | `LinkAirborneState`, `tickPitFall`, `accessTickDialogGotItem` |
| `linkEntityEffects` | `LinkAirborneState`, `tickJump` |
| `linkItemState` | `LinkAirborneState`, `accessTickDialogGotItem` |
| `linkMotion` | `LinkAirborneState`, `tickJump`, `playLandingSound`, `linkOverPit`, `refreshGroundStatus`, `enterPitGroundState`, `slipTowardPitCenter`, `startPitFall`, `tickPitFall`, `updateLastSafePositionIfPossible`, `accessTickDialogGotItem` |
| `linkServices` | `LinkAirborneState`, `tickJump`, `playLandingSound`, `swordSuppressesJumpAnimation`, `linkOverPit`, `refreshGroundStatus`, `enterPitGroundState`, `startPitFall`, `tickPitFall`, `accessUseRocsFeather`, `accessTickDialogGotItem` |
| `linkSideView` | `LinkAirborneState`, `tickJump`, `accessUseRocsFeather` |
| `linkSignals` | `LinkAirborneState`, `tickPitFall` |
| `linkStateMachine` | `LinkAirborneState`, `tickJump`, `tickOverworldLedgeFall`, `accessSetPitWarpArrival`, `accessRomMotionState`, `accessUseRocsFeather` |
| `linkSwimming` | `LinkAirborneState`, `accessBounceFromGoomba` |
| `airborne` | `tickJump`, `tickOverworldLedgeFall`, `startPitFall`, `updateLastSafePositionIfPossible`, `accessSetPitWarpArrival`, `accessIsAirborne`, `accessUseRocsFeather`, `accessBounceFromGoomba` |
| `spinningPitWarpArrival` | `tickJump`, `accessSetPitWarpArrival` |
| `zSubPixels` | `tickJump`, `startPitFall`, `accessSetPitWarpArrival`, `accessUseRocsFeather`, `accessZPixels`, `accessTickDialogGotItem` |
| `zVelocity` | `tickJump`, `startPitFall`, `accessSetPitWarpArrival`, `accessZVelocity`, `accessUseRocsFeather`, `accessBounceFromGoomba`, `accessTickDialogGotItem` |
| `fallingIntoPit` | `refreshGroundStatus`, `startPitFall`, `tickPitFall`, `updateLastSafePositionIfPossible`, `accessRomMotionState`, `accessIsFallingIntoPit`, `accessUseRocsFeather`, `accessBounceFromGoomba` |
| `groundStatus` | `refreshGroundStatus`, `enterPitGroundState`, `slipTowardPitCenter`, `startPitFall`, `tickPitFall`, `updateLastSafePositionIfPossible`, `accessRomMotionState`, `accessUseRocsFeather`, `accessBounceFromGoomba` |
| `pitSlippingCounter` | `enterPitGroundState`, `slipTowardPitCenter`, `tickPitFall` |
| `fallingFrameCounter` | `startPitFall`, `tickPitFall` |
| `pitSlipTargetTopLeftX` | `enterPitGroundState`, `slipTowardPitCenter` |
| `pitSlipTargetTopLeftY` | `enterPitGroundState`, `slipTowardPitCenter` |
| `pitSlipPhysicsFlag` | `refreshGroundStatus`, `enterPitGroundState`, `tickPitFall` |
| `hasPitSlipTarget` | `refreshGroundStatus`, `enterPitGroundState`, `slipTowardPitCenter`, `tickPitFall` |
| `overworldLedgeFallActive` | `tickJump`, `tickJumpAnimation` |
| `directionalLedgeJumpMotionLocked` | `tickJump` |
| `directionalLedgeJumpSpeedX` | `tickJump` |
| `directionalLedgeJumpSpeedY` | `tickJump` |
| `directionalLedgeCollisionCounter` |  |

Result: depth 2 → 0; original three chain files (508 lines) → LinkAirborne (331), LinkPitFall (257), LinkElevation (34). State is private; pit and jump callers use their respective components, with the existing fall, jump, ledge and sound assertions unchanged. Full suite: 2,891 tests, zero failures/errors, one skipped.

## Link animation

Original chain: LinkAnimator → LinkAnimationSelection → LinkSpriteRendering → LinkPoseState.

Pose selection, overrides, jump/push animation counters and scripted presentations form LinkAnimator. LinkSpriteComposer owns the reusable decoded tile buffer and body/equipment drawing; it receives selected pose values rather than an animator/back-reference. LinkPoseTables holds immutable fallback animation table data, separate from selection and drawing.

| State/constant | Referencing methods |
| --- | --- |
| `dialogGotItemSequence` | `LinkPoseState`, `accessDialogGotItemActive`, `accessDialogGotItem`, `accessDialogGotItemCountdown`, `accessPresentedDialogGotItem`, `accessPresentToadstool`, `accessClearDialogGotItem` |
| `linkAirborne` | `LinkPoseState`, `renderEquippedItems`, `resolveAnimationState`, `resolveNormalAnimation`, `submitEquippedOam`, `renderY` |
| `linkAnimationState` | `LinkPoseState`, `renderEquippedItems`, `resolveAnimationState`, `resolveNormalAnimation`, `cachePushingAnimation`, `queryOverride`, `showRomFallenPose`, `showHeldItemPose`, `showPowerUpPickupPose`, `showStandingShieldDownPose`, `showSwordAcquisitionSpinPose`, `showSwordAcquisitionFinalPose`, `submitEquippedOam`, `accessPresentToadstool` |
| `linkDamage` | `LinkPoseState`, `showPowerUpPickupPose` |
| `linkEntityEffects` | `LinkPoseState`, `render`, `resolveAnimationState`, `resolveNormalAnimation`, `submitOam` |
| `linkEquipment` | `LinkPoseState`, `resolveNormalAnimation` |
| `linkItemState` | `LinkPoseState`, `cachePushingAnimation`, `showHeldItemPose`, `showPowerUpPickupPose` |
| `linkMotion` | `LinkPoseState`, `render`, `renderEquippedItems`, `cachePushingAnimation`, `showPowerUpPickupPose`, `submitOam`, `submitEquippedOam`, `renderY` |
| `linkServices` | `LinkPoseState`, `render`, `renderEquippedItems`, `resolveNormalAnimation`, `cachePushingAnimation`, `showSwordAcquisitionSpinPose`, `submitOam`, `submitEquippedOam` |
| `linkStateMachine` | `LinkPoseState`, `render`, `resolveAnimationState`, `resolveNormalAnimation`, `showRomFallenPose`, `showHeldItemPose`, `showPowerUpPickupPose`, `showStandingShieldDownPose`, `showSwordAcquisitionSpinPose`, `showSwordAcquisitionFinalPose`, `submitOam` |
| `ANIMATION_STATE` | `resolveNormalAnimation` |
| `SHIELD_USE_ANIMATION_STATE` | `resolveNormalAnimation` |
| `MIRROR_SHIELD_USE_ANIMATION_STATE` | `resolveNormalAnimation` |
| `LIFTING_ANIMATION_STATE` | `resolveNormalAnimation` |
| `SWIMMING_ANIMATION_STATE` | `resolveAnimationState` |
| `DIVING_ANIMATION_STATE` | `resolveAnimationState` |
| `JUMP_ANIMATION_STATE` | `resolveNormalAnimation` |
| `FALL_ANIMATION_STATE` | `resolveAnimationState` |
| `composedTiles` | `render`, `submitOam` |
| `tarinShieldPresentation` | `showTarinShieldPresentation`, `clearTarinShieldPresentation`, `showStandingShieldDownPose`, `accessIsTarinShieldPresentationVisible` |
| `tarinShieldPalette` | `showTarinShieldPresentation`, `accessTarinShieldPalette` |
| `romAnimationStateOverride` | `resolveAnimationState`, `showRomFallenPose`, `showHeldItemPose`, `showPowerUpPickupPose`, `showStandingShieldDownPose`, `showSwordAcquisitionSpinPose`, `showSwordAcquisitionFinalPose` |
| `swordAcquisitionBaseRomDirection` | `showSwordAcquisitionSpinPose`, `showSwordAcquisitionFinalPose` |
| `swordAcquisitionAnimationState` | `renderEquippedItems`, `resolveAnimationState`, `showSwordAcquisitionSpinPose`, `showSwordAcquisitionFinalPose`, `submitEquippedOam` |
| `marinWakeUpBedVariant` | `showMarinWakeUpBed`, `accessIsMarinWakeUpBedVisible`, `accessMarinWakeUpBedVariant` |
| `bedJumpRenderZ` | `renderY` |
| `ledgeJumpRenderY` | `resolveNormalAnimation`, `renderY` |
| `jumpAnimationCounter` |  |
| `jumpAnimationFrame` | `resolveNormalAnimation` |
| `pushingAnimationState` | `resolveNormalAnimation`, `cachePushingAnimation` |
| `UNSHIELDED_WALK_BASE` | `cachePushingAnimation` |

Result: depth 3 → 0; LinkAnimator 449 lines, LinkSpriteComposer 284, LinkPoseTables 52. Pose state and tile buffers are private, and drawing receives selected pose values rather than the animator. Aggregate debug reset moved from frame dispatch to the existing motion command role; game-over completion invokes it before clearing recovery counters in the same order. Full suite: 2,891 tests, zero failures/errors, one skipped.
