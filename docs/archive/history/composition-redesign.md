# Composition redesign

Field-use inventories describe baseline `c122c0d8`. They were extracted from Java compiler-resolved field symbols, including nested frame/dependency fields and inherited references; the listed methods are main-source consumers in owned packages. Empty rows have no method reference (for example declaration-only constants/initializers). Per-chain result paragraphs record the chain commit; final metrics below include subsequent cleanup.

## Dialog

Original chain: DialogController → DialogTextFormatting → DialogPaging → DialogLifecycle → DialogState.

DialogText owns name substitution and width-dependent formatting; DialogController owns the single reveal/scroll/choice state machine and queued sound requests. No collaborator receives the controller or its whole state.

| State/constant | Referencing methods |
| --- | --- |
| `DialogState.LINK_Y_BOX_THRESHOLD` | `DialogLifecycle.openForLinkY`, `DialogLifecycle.openPreformattedForLinkY` |
| `DialogState.DEFAULT_PLAYER_NAME` | `DialogPaging.setPlayerName` |
| `DialogState.NAME_LENGTH` | `DialogTextFormatting.substituteNameTokens` |
| `DialogState.DIALOG_COOLDOWN` | `DialogPaging.finishClosing` |
| `DialogState.ASK_MARKER` | `DialogLifecycle.tick`, `DialogPaging.currentPageLength`, `DialogPaging.hasNextPage`, `DialogPaging.isAtChoicePromptMarker`, `DialogPaging.visibleEnd`, `DialogState.normalizeAskMarkers`, `DialogTextFormatting.formatPreformatted` |
| `DialogState.lineWidth` | `DialogLifecycle.tick`, `DialogPaging.wrap`, `DialogState.constructor`, `DialogTextFormatting.appendWord`, `DialogTextFormatting.formatPreformatted` |
| `DialogState.romTimed` | `DialogLifecycle.advance`, `DialogLifecycle.isBoxRowVisible`, `DialogLifecycle.isWaitingForPageAdvance`, `DialogLifecycle.tick`, `DialogPaging.closeDialog`, `DialogPaging.visibleText`, `DialogState.forGameplay` |
| `DialogState.openingAge` | `DialogLifecycle.advance`, `DialogLifecycle.isBoxRowVisible`, `DialogLifecycle.open`, `DialogLifecycle.tick` |
| `DialogState.closingAge` | `DialogLifecycle.advance`, `DialogLifecycle.isBoxRowVisible`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.closeDialog` |
| `DialogState.scrollAge` | `DialogLifecycle.advance`, `DialogLifecycle.beginScroll`, `DialogLifecycle.open`, `DialogLifecycle.textScrollOffset`, `DialogLifecycle.tick`, `DialogPaging.visibleText` |
| `DialogState.scrollBase` | `DialogLifecycle.advance`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.visibleText` |
| `DialogState.scrollPrevious` | `DialogLifecycle.beginScroll`, `DialogPaging.visibleText` |
| `DialogState.retainedLine` | `DialogLifecycle.beginScroll`, `DialogLifecycle.open`, `DialogPaging.visibleText` |
| `DialogState.shiftedPage` | `DialogLifecycle.advance`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.visibleText` |
| `DialogState.text` | `DialogLifecycle.open`, `DialogPaging.charAtVisibleCell`, `DialogPaging.currentPageLength`, `DialogPaging.hasNextPage`, `DialogPaging.isAtChoicePromptMarker`, `DialogPaging.nextPageStart`, `DialogPaging.pageEnd`, `DialogPaging.visibleEnd`, `DialogPaging.visibleText` |
| `DialogState.playerName` | `DialogPaging.setPlayerName`, `DialogTextFormatting.nameCharacter` |
| `DialogState.pageStart` | `DialogLifecycle.advance`, `DialogLifecycle.characterIndex`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.currentPageLength`, `DialogPaging.isAtChoicePromptMarker`, `DialogPaging.nextPageStart`, `DialogPaging.visibleEnd`, `DialogPaging.visibleText` |
| `DialogState.visibleCharactersOnPage` | `DialogLifecycle.advance`, `DialogLifecycle.characterIndex`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.enterPageWaitIfNeeded`, `DialogPaging.isAtChoicePromptMarker`, `DialogPaging.visibleEnd` |
| `DialogState.visibleNonSpaceCharactersOnPage` | `DialogLifecycle.advance`, `DialogLifecycle.open`, `DialogLifecycle.tick` |
| `DialogState.revealTicks` | `DialogLifecycle.advance`, `DialogLifecycle.beginScroll`, `DialogLifecycle.open`, `DialogLifecycle.tick` |
| `DialogState.dialogCooldown` | `DialogLifecycle.dialogCooldown`, `DialogLifecycle.tickGameplayFrameCooldown`, `DialogPaging.finishClosing` |
| `DialogState.active` | `DialogLifecycle.advance`, `DialogLifecycle.isActive`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.finishClosing` |
| `DialogState.interactionLocked` | `DialogLifecycle.advance`, `DialogLifecycle.setInteractionLocked` |
| `DialogState.waitingForPageAdvance` | `DialogLifecycle.advance`, `DialogLifecycle.isWaitingForPageAdvance`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.enterChoicePrompt`, `DialogPaging.enterPageWaitIfNeeded` |
| `DialogState.pageWaitAge` | `DialogLifecycle.isWaitingForPageAdvance`, `DialogLifecycle.tick`, `DialogPaging.enterPageWaitIfNeeded` |
| `DialogState.choicePrompt` | `DialogLifecycle.advance`, `DialogLifecycle.isChoicePrompt`, `DialogLifecycle.moveChoiceSelection`, `DialogLifecycle.open`, `DialogPaging.enterChoicePrompt`, `DialogPaging.enterPageWaitIfNeeded`, `DialogPaging.finishClosing` |
| `DialogState.choiceSelectionIndex` | `DialogLifecycle.choiceSelectionIndex`, `DialogLifecycle.moveChoiceSelection`, `DialogLifecycle.open` |
| `DialogState.boxPosition` | `DialogLifecycle.open`, `DialogPaging.boxPosition`, `DialogPaging.boxY` |
| `DialogState.soundEvents` | `DialogLifecycle.moveChoiceSelection`, `DialogLifecycle.open`, `DialogLifecycle.tick`, `DialogPaging.consumeSoundEvents`, `DialogPaging.enterChoicePrompt`, `DialogPaging.enterPageWaitIfNeeded` |
| `ParsedText.text` |  |
| `SoundEvent.code` | `SoundEvent.code`, `SoundEvent.constructor` |
| `BoxPosition.y` | `BoxPosition.constructor`, `BoxPosition.y` |
| `DialogPaging.TEXT_PRINT` | `DialogPaging.consumeSoundEvents` |
| `DialogPaging.DIALOG_BREAK` | `DialogPaging.consumeSoundEvents` |
| `DialogPaging.MOVE_SELECTION` | `DialogPaging.consumeSoundEvents` |

## Intro

Original chain: IntroSequence → IntroSceneRendering → IntroSceneTransitions → IntroBeachMotion → IntroSceneMotion → IntroLifecycle → IntroSequenceState.

IntroSequence coordinates stage transitions; beach actor simulation owns its actor/timer state; palette processing owns palette sources/fades; sprite composition owns decoded tables, scratch and RNG. Hardware scroll/map writes are an explicit shared video boundary. Preserve pre-update snapshots and RNG call order.

| State/constant | Referencing methods |
| --- | --- |
| `IntroSequence.SEA_SCROLL_FRAMES` |  |
| `IntroSequence.LINK_FACE_FRAMES` | `IntroLifecycle.tickLinkFace` |
| `IntroSequence.SEA_FADE_FRAMES` |  |
| `IntroSequence.BEACH_FADE_FRAMES` | `IntroSceneMotion.tickSeaFade` |
| `IntroSequence.BEACH_MARIN_WALK_FRAMES` |  |
| `IntroSequence.BEACH_MARIN_WAIT_FRAMES` |  |
| `IntroSequence.TITLE_REVEAL_STEP_FRAMES` | `IntroBeachMotion.tickTitleReveal` |
| `IntroSequence.TITLE_HOLD_FRAMES` | `IntroBeachMotion.tickTitleDx` |
| `IntroSequenceState.snapshotBuilder` | `IntroBeachMotion.refreshSnapshot` |
| `IntroSequenceState.renderSpriteScratch` | `IntroSceneTransitions.renderSprites` |
| `IntroSequenceState.correctionSpriteScratch` | `IntroSceneMotion.tickBeach` |
| `IntroSequenceState.INITIAL_SHIP_X` |  |
| `IntroSequenceState.INITIAL_SHIP_Y` |  |
| `IntroSequenceState.LINK_FACE_SCREAM_FRAME` | `IntroBeachMotion.substate`, `IntroLifecycle.tickLinkFace` |
| `IntroSequenceState.LINK_FACE_LIGHTNING_FRAME` | `IntroBeachMotion.substate`, `IntroLifecycle.tickLinkFace` |
| `IntroSequenceState.BEACH_MARIN_START_X` | `IntroSceneMotion.tickBeachFade` |
| `IntroSequenceState.BEACH_LINK_START_X` | `IntroSceneMotion.tickMarin` |
| `IntroSequenceState.BEACH_LINK_Y` |  |
| `IntroSequenceState.SEA_LIGHTNING_SCROLLS` | `IntroBeachMotion.triggerSeaLightningIfNeeded` |
| `Lightning.variant` |  |
| `Lightning.x` |  |
| `Lightning.y` |  |
| `Lightning.remaining` |  |
| `IntroSequenceState.rom` | `IntroBeachMotion.transitionTo`, `IntroLifecycle.tickLinkFace`, `IntroSceneMotion.animateWaves`, `IntroSceneMotion.patchMountains`, `IntroSceneMotion.tickInertLink`, `IntroSceneTransitions.lineScrollYMap`, `IntroSceneTransitions.renderTitleSparkle`, `IntroSequenceState.constructor` |
| `IntroSequenceState.backgroundProvider` | `IntroSceneTransitions.setScene`, `IntroSequenceState.constructor` |
| `IntroSequenceState.random` | `IntroLifecycle.tick`, `IntroSceneTransitions.renderRain`, `IntroSequenceState.constructor` |
| `IntroSequenceState.titleRows` | `IntroBeachMotion.tickTitleReveal`, `IntroLifecycle.skipToTitle`, `IntroSequenceState.constructor` |
| `IntroSequenceState.marinVariants` | `IntroSceneMotion.tickBeach`, `IntroSceneTransitions.renderBeachEntities`, `IntroSequenceState.constructor` |
| `IntroSequenceState.inertLinkVariants` | `IntroSceneTransitions.renderBeachEntities`, `IntroSequenceState.constructor` |
| `IntroSequenceState.sparkleVariants` | `IntroSceneTransitions.renderTitleSparkle`, `IntroSequenceState.constructor` |
| `IntroSequenceState.lightningTiles` | `IntroSceneTransitions.renderLightning`, `IntroSequenceState.constructor` |
| `IntroSequenceState.lightningPositions` | `IntroBeachMotion.triggerLightning`, `IntroSequenceState.constructor` |
| `IntroSequenceState.lightningStatuses` | `IntroBeachMotion.triggerLightning`, `IntroSequenceState.constructor` |
| `IntroSequenceState.shipHeave` | `IntroSceneTransitions.renderShip`, `IntroSequenceState.constructor` |
| `IntroSequenceState.verticalOffsets` | `IntroBeachMotion.transitionTo`, `IntroBeachMotion.updateWaveOffsets`, `IntroSequenceState.constructor` |
| `IntroSequenceState.colorModifiers` | `IntroSceneTransitions.dynamicBgPalettes`, `IntroSequenceState.constructor` |
| `IntroSequenceState.dxFadeInPalette` | `IntroSceneTransitions.dynamicObjPalettes`, `IntroSequenceState.constructor` |
| `IntroSequenceState.shipTiles` | `IntroSceneTransitions.renderShip`, `IntroSequenceState.constructor` |
| `IntroSequenceState.additionalShipTiles` | `IntroSceneTransitions.renderShip`, `IntroSequenceState.constructor` |
| `IntroSequenceState.stage` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.substate`, `IntroBeachMotion.tickTitleDx`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.isActive`, `IntroLifecycle.sceneId`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick`, `IntroSceneMotion.tickBeach`, `IntroSceneTransitions.dynamicBgPalettes`, `IntroSceneTransitions.dynamicObjPalettes`, `IntroSceneTransitions.lineScrollMap`, `IntroSceneTransitions.lineScrollYMap`, `IntroSceneTransitions.renderShip`, `IntroSceneTransitions.renderSprites` |
| `IntroSequenceState.frameCounter` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.tick`, `IntroLifecycle.tickSea` |
| `IntroSequenceState.seaWaveCounter` | `IntroLifecycle.tickSeaWaveSound` |
| `IntroSequenceState.noiseSound` | `IntroBeachMotion.playThunder`, `IntroLifecycle.setNoiseSound`, `IntroLifecycle.tickSeaWaveSound` |
| `IntroSequenceState.stageFrame` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.tickTitleReveal`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.sceneId`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick` |
| `IntroSequenceState.clock` | `IntroBeachMotion.updateWaveOffsets`, `IntroLifecycle.tick`, `IntroLifecycle.tickLinkFace`, `IntroLifecycle.tickSea`, `IntroSceneMotion.animateWaves`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickInertLink`, `IntroSceneMotion.tickMarin`, `IntroSceneMotion.updateBeachOffsets`, `IntroSceneTransitions.renderShip`, `IntroSceneTransitions.renderTitleSparkle` |
| `IntroSequenceState.loadDelay` | `IntroLifecycle.tick`, `IntroLifecycle.tickLinkFace`, `IntroLifecycle.tickSea`, `IntroSceneMotion.tickSeaFade` |
| `IntroSequenceState.fraction4` | `IntroBeachMotion.updateWaveOffsets`, `IntroSceneMotion.updateBeachOffsets` |
| `IntroSequenceState.fraction5` | `IntroSceneMotion.updateBeachOffsets` |
| `IntroSequenceState.fractionD` | `IntroSceneMotion.updateBeachOffsets` |
| `IntroSequenceState.marinVariant` | `IntroSceneMotion.tickBeach`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.linkVariant` | `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.marinY` | `IntroSceneMotion.tickBeach`, `IntroSceneMotion.tickInertLink`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.linkY` | `IntroSceneMotion.tickInertLink`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.linkState` | `IntroLifecycle.tickSeaWaveSound`, `IntroSceneMotion.tickInertLink`, `IntroSceneMotion.tickMarin` |
| `IntroSequenceState.linkTimer` | `IntroSceneMotion.tickInertLink`, `IntroSceneMotion.tickMarin` |
| `IntroSequenceState.panCounter` | `IntroSceneMotion.tickInertLink` |
| `IntroSequenceState.panRows` | `IntroSceneMotion.tickInertLink` |
| `IntroSequenceState.waveTiles` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.tick`, `IntroSceneMotion.animateWaves`, `IntroSequenceState.constructor` |
| `IntroSequenceState.waveDestination` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo` |
| `IntroSequenceState.skipped` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick`, `IntroSceneTransitions.renderSprites` |
| `IntroSequenceState.fadeSteps` | `IntroBeachMotion.transitionTo`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade` |
| `IntroSequenceState.fadeBg` | `IntroBeachMotion.transitionTo`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade`, `IntroSceneTransitions.dynamicBgPalettes` |
| `IntroSequenceState.fadeObj` | `IntroBeachMotion.transitionTo`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade`, `IntroSceneTransitions.dynamicObjPalettes` |
| `IntroSequenceState.visibleBg` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick` |
| `IntroSequenceState.visibleObj` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick` |
| `IntroSequenceState.visibleWave` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.tick` |
| `IntroSequenceState.scrollX` | `IntroBeachMotion.transitionTo`, `IntroBeachMotion.triggerSeaLightningIfNeeded`, `IntroLifecycle.scrollX`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tickSea` |
| `IntroSequenceState.scrollY` | `IntroLifecycle.scrollY`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick`, `IntroSceneMotion.tickInertLink` |
| `IntroSequenceState.verticalWaveOffset` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroBeachMotion.updateWaveOffsets`, `IntroLifecycle.skipToTitle`, `IntroSceneTransitions.lineScrollMap`, `IntroSceneTransitions.lineScrollYMap` |
| `IntroSequenceState.shipX` | `IntroLifecycle.shipX`, `IntroLifecycle.tickSea`, `IntroSceneTransitions.renderShip` |
| `IntroSequenceState.shipY` | `IntroSceneTransitions.renderShip` |
| `IntroSequenceState.seaFadeTimer` | `IntroLifecycle.tickLinkFace`, `IntroSceneMotion.tickSeaFade`, `IntroSceneTransitions.renderShip` |
| `IntroSequenceState.beachFadeTimer` | `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade` |
| `IntroSequenceState.linkFaceTimer` | `IntroBeachMotion.substate`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.tickLinkFace` |
| `IntroSequenceState.marinState` | `IntroBeachMotion.substate`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.lineScrollMap` |
| `IntroSequenceState.marinX` | `IntroSceneMotion.tickBeach`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.marinInertia` | `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickMarin` |
| `IntroSequenceState.marinTimer` | `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickMarin` |
| `IntroSequenceState.linkX` | `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.linkVisible` | `IntroLifecycle.skipToTitle`, `IntroSceneMotion.tickBeach`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.renderBeachEntities` |
| `IntroSequenceState.beachScrollX` | `IntroBeachMotion.transitionTo`, `IntroLifecycle.scrollX`, `IntroLifecycle.skipToTitle`, `IntroSceneMotion.patchMountains`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.lineScrollMap` |
| `IntroSequenceState.titleRowIndex` | `IntroBeachMotion.tickTitleReveal`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.skipToTitle` |
| `IntroSequenceState.titleRevealRows` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.tickTitleReveal`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.titleRevealRows` |
| `IntroSequenceState.titleHoldFrame` | `IntroBeachMotion.tickTitleDx`, `IntroBeachMotion.transitionTo`, `IntroSceneTransitions.dynamicObjPalettes`, `IntroSceneTransitions.renderTitleSparkle` |
| `IntroSequenceState.titleEffectFrame` | `IntroLifecycle.tick`, `IntroSceneTransitions.lineScrollYMap` |
| `IntroSequenceState.visibleScrollY` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick`, `IntroSceneTransitions.lineScrollYMap` |
| `IntroSequenceState.topLineScroll` | `IntroLifecycle.tick`, `IntroSceneTransitions.lineScrollMap` |
| `IntroSequenceState.drawCommandQueued` | `IntroLifecycle.tick`, `IntroSceneMotion.animateWaves`, `IntroSceneMotion.tickInertLink` |
| `IntroSequenceState.beachScrollStoppedThisFrame` | `IntroLifecycle.tick`, `IntroSceneMotion.tickMarin`, `IntroSceneTransitions.lineScrollMap` |
| `IntroSequenceState.paletteLightningCountdown` | `IntroBeachMotion.decrementLightning`, `IntroSceneTransitions.dynamicBgPalettes` |
| `IntroSequenceState.visibleTilemap` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.tick`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.visibleAttrmap` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.tick`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.sectionScrollOffsets` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroBeachMotion.updateWaveOffsets`, `IntroLifecycle.tick`, `IntroSceneMotion.updateBeachOffsets`, `IntroSceneTransitions.lineScrollMap` |
| `IntroSequenceState.lightning` | `IntroBeachMotion.decrementLightning`, `IntroBeachMotion.triggerLightning`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tickLinkFace`, `IntroLifecycle.tickSea`, `IntroSceneTransitions.renderLightning` |
| `IntroSequenceState.lightningVisibleCountdown` | `IntroBeachMotion.decrementLightning`, `IntroBeachMotion.playThunder` |
| `IntroSequenceState.tilemap` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.tick`, `IntroLifecycle.tickLinkFace`, `IntroSceneMotion.patchMountains`, `IntroSceneMotion.tickInertLink`, `IntroSceneRendering.applyTitleRow`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.attrmap` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.tick`, `IntroSceneMotion.tickInertLink`, `IntroSceneRendering.applyTitleRow`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.bgPalettes` | `IntroBeachMotion.transitionTo`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade`, `IntroSceneTransitions.dynamicBgPalettes`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.objPalettes` | `IntroBeachMotion.transitionTo`, `IntroSceneMotion.tickBeachFade`, `IntroSceneMotion.tickSeaFade`, `IntroSceneTransitions.dynamicObjPalettes`, `IntroSceneTransitions.setScene` |
| `IntroSequenceState.sprites` | `IntroBeachMotion.refreshSnapshot`, `IntroLifecycle.skipToTitle`, `IntroLifecycle.tick` |
| `IntroSequenceState.pendingSprites` | `IntroLifecycle.tick`, `IntroSceneMotion.tickBeach` |
| `IntroSequenceState.snapshot` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo`, `IntroLifecycle.lineScrollX`, `IntroLifecycle.scrollXForLine`, `IntroLifecycle.snapshot`, `IntroLifecycle.sprites` |
| `IntroSequenceState.transitionFrame` | `IntroBeachMotion.refreshSnapshot`, `IntroBeachMotion.transitionTo` |
| `DefaultResources.romData` |  |
| `DefaultResources.backgroundProvider` |  |

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
| `EntitySpriteDecodeState.ENTITY_ARROW` | `EntitySpritePlayerProjectileRegistrations.lookup`, `EntitySpriteRomDecoding.forBombArrow` |
| `EntitySpriteDecodeState.ENTITY_BOOMERANG` | `EntitySpritePlayerProjectileRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MAGIC_ROD_FIREBALL` | `EntitySpritePlayerProjectileRegistrations.lookup`, `EntitySpriteRomDecoding.forMagicRodFireState` |
| `EntitySpriteDecodeState.ENTITY_MAGIC_POWDER_SPRINKLE` | `EntitySpritePlayerProjectileRegistrations.lookup`, `EntitySpriteRomDecoding.forMagicPowderTorchState` |
| `EntitySpriteDecodeState.ENTITY_MUSICAL_NOTE` | `EntitySpritePlayerProjectileRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SWORD_BEAM` | `EntitySpritePlayerProjectileRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_BOMB` | `EntitySpriteDynamicComposites.forBombExplosion`, `EntitySpriteDynamicComposites.forBombRightBeforeExploding`, `EntitySpritePlayerProjectileRegistrations.lookup`, `EntitySpriteRomDecoding.forBombArrow` |
| `EntitySpriteDecodeState.ENTITY_BUTTERFLY` | `EntitySpriteCompanionRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HOOKSHOT_CHAIN` | `EntitySpritePlayerProjectileRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_CRYSTAL_SWITCH` | `EntitySpriteRoomObjectRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_OCTOROK` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_OCTOROK_ROCK` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MOBLIN` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_IRON_MASK` | `EntitySpriteArmoredEnemyRegistrations.lookup`, `EntitySpriteLookup.forIronMaskState` |
| `EntitySpriteDecodeState.ENTITY_MOBLIN_ARROW` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_BOUNCING_BOMBITE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_TIMER_BOMBITE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MAD_BOMBER` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_BOMBER` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MOBLIN_SWORD` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_TEKTITE` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_LEEVER` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_ANTI_FAIRY` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SPARK_COUNTER_CLOCKWISE` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SPARK_CLOCKWISE` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_ZOL` | `EntitySpriteSlimeEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GEL` | `EntitySpriteSlimeEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HIDING_ZOL` | `EntitySpriteSlimeEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_STALFOS_AGGRESSIVE` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_STALFOS_EVASIVE` | `EntitySpriteLookup.forStalfosEvasiveState`, `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GIBDO` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_LIKE_LIKE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_PEAHAT` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_WIZROBE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_WIZROBE_PROJECTILE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_ARMOS_STATUE` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_ARMOS_KNIGHT` | `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HIDING_GHINI` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GIANT_GHINI` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GHINI` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_POLS_VOICE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_KEESE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GOOMBA` | `EntitySpriteSideViewRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SNAKE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HARDHAT_BEETLE` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SPIKED_BEETLE` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_LASER` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_LASER_BEAM` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SPIKE_TRAP` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_PAIRODD` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_PAIRODD_PROJECTILE` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_WATER_TEKTITE` | `EntitySpriteAquaticRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_FISH` | `EntitySpriteAquaticRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_BOW_WOW` | `EntitySpriteCompanionRegistrations.lookup`, `EntitySpriteDynamicComposites.forFollowerEntityType` |
| `EntitySpriteDecodeState.ENTITY_DOG` | `EntitySpriteCompanionRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_KID_70` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_KID_71` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_KID_72` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_KID_73` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MADAM_MEOWMEOW` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_CROW` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_BOO_BUDDY` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_HEART` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_RUPEE` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_FAIRY` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SWORD_SHIELD_PICKUP` | `EntitySpriteLookup.forSwordShieldPickup`, `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_IRON_MASKS_MASK` | `EntitySpriteArmoredEnemyRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_PIECE_OF_POWER` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GUARDIAN_ACORN` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HEART_PIECE` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HEART_CONTAINER` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_ARROWS` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_BOMBS` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_SLEEPY_TOADSTOOL` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_MAGIC_POWDER` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_HIDING_SLIME_KEY` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_DROPPABLE_SECRET_SEASHELL` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_FLOATING_ITEM` | `EntitySpriteContextualObjectRegistrations.lookupFloatingItem`, `EntitySpriteDynamicComposites.forFloatingItemOverlay` |
| `EntitySpriteDecodeState.ENTITY_FLOATING_ITEM_2` | `EntitySpriteContextualObjectRegistrations.lookupFloatingItem`, `EntitySpriteDynamicComposites.decodeFloatingItemMain` |
| `EntitySpriteDecodeState.ENTITY_MARIN` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_TARIN` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_GRANDPA_ULRIRA` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MARIN_AT_THE_SHORE` | `EntitySpriteDynamicComposites.forFollowerEntityType`, `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_MARIN_AT_TAL_TAL_HEIGHTS` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteDecodeState.ENTITY_COLOR_SHELL_RED` | `EntitySpriteBossComposites.forColorShellState`, `EntitySpriteDecodeState.isColorShellType` |
| `EntitySpriteDecodeState.ENTITY_COLOR_SHELL_BLUE` | `EntitySpriteDecodeState.isColorShellType` |
| `EntitySpriteDecodeState.COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES` | `EntitySpriteBossComposites.forColorShellState` |
| `EntitySpriteDecodeState.COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES` | `EntitySpriteBossComposites.forColorShellState` |
| `EntitySpriteDecodeState.romData` | `EntitySpriteDecodeGeometry.decodeRectangleVariants`, `EntitySpriteDecodeGeometry.readByte`, `EntitySpriteDecodeGeometry.validateDisplayList`, `EntitySpriteDecodeState.constructor`, `EntitySpriteRomDecoding.decodePair`, `EntitySpriteRomDecoding.decodeSingle` |
| `EntitySpriteHandlerCatalog.ENTITY_LIFTABLE_ROCK` | `EntitySpriteContextualObjectRegistrations.lookupFloatingItem`, `EntitySpriteRomDecoding.appendDynamicRectangleVariants`, `EntitySpriteRomDecoding.decodeLiftableRock` |
| `EntitySpriteHandlerCatalog.ENTITY_CHEST_WITH_ITEM` | `EntitySpriteLookup.forChestState`, `EntitySpriteRoomObjectRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_KEY_DROP_POINT` | `EntitySpritePickupRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_STAR` | `EntitySpriteSlimeEnemyRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_BLOOPER` | `EntitySpriteAquaticRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_WINGED_OCTOROK` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_PINCER` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_BUSH_CRAWLER` | `EntitySpriteLookup.forBushCrawlerCrawlState`, `EntitySpriteLookup.forBushCrawlerState`, `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_MIMIC` | `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM` | `EntitySpriteBossComposites.forMiniMoldormState`, `EntitySpriteDungeonCreatureRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_MOLDORM` | `EntitySpriteBossComposites.forMoldormPresentation`, `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES` | `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES_BAR` | `EntitySpriteBossComposites.forRollingBonesBar`, `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND` | `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA` | `EntitySpriteHazardRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_HINOX` | `EntitySpriteBossRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_CUCCO` | `EntitySpriteCompanionRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_GOPONGA_FLOWER` | `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_GIANT_GOPONGA_FLOWER` | `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_GOPONGA_FLOWER_PROJECTILE` | `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_POKEY` | `EntitySpriteLookup.forPokeySegment`, `EntitySpriteLookup.forPokeyState`, `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_PIRANHA_PLANT` | `EntitySpritePlantRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_ZORA` | `EntitySpriteAquaticRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_ZOMBIE` | `EntitySpriteUndeadEnemyRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_BUZZ_BLOB` | `EntitySpriteOverworldCreatureRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_SAND_CRAB` | `EntitySpriteAquaticRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_URCHIN` | `EntitySpriteAquaticRegistrations.lookup`, `EntitySpriteLookup.forUrchinState` |
| `EntitySpriteHandlerCatalog.ENTITY_WITCH_RAT` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.ENTITY_WITCH` | `EntitySpriteVillageNpcRegistrations.lookup` |
| `EntitySpriteHandlerCatalog.LIFTABLE_ROCK_INTACT_ROCK_VARIANT` | `EntitySpriteRomDecoding.decodeLiftableRock` |
| `EntitySpriteHandlerCatalog.LIFTABLE_ROCK_INTACT_BUSH_VARIANT` | `EntitySpriteRomDecoding.decodeLiftableRock` |
| `EntitySpriteHandlerCatalog.LIFTABLE_ROCK_SMASHED_ROCK_VARIANT_BASE` |  |
| `EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_VARIANT_BASE` |  |
| `EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_SWAMP_VARIANT_BASE` |  |
| `EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS` | `EntitySpriteLookup.forInstrumentShard`, `EntitySpritePickupRegistrations.lookup` |

Result: project inheritance depth 6 → 0; catalog facade 411 lines. Immutable family recipes and their indexed registration data are independent of ROM decoding. EntitySpriteDecoder owns the ROM; EntitySpriteStates, BossSpriteComposer and EntityEffectSprites compose that decoder for state selection, boss geometry and transient effects respectively. EntitySpriteGeometry contains only coordinate algorithms. Two ROM-free tests cover registration priority/arbitrary map selectors and bounded OAM decoding. Full suite: 2,888 tests, zero failures/errors, one skipped.

## File menu

Original chain: FileMenuController → FileMenuEditing → FileMenuFlow → FileMenuState.

Name-entry character selection, insertion position and five-byte buffer belong to FileNameEntry. FileMenuController owns slot/copy/erase transitions and snapshot publication; those operations are cohesive and are merged rather than keeping the former flow/editing slices.

| State/constant | Referencing methods |
| --- | --- |
| `FileMenuState.snapshotBuilder` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.spriteScratch` | `FileMenuEditing.buildSprites` |
| `FileMenuState.NAME_LENGTH` | `FileMenuEditing.drawSaveSlotName`, `FileMenuFlow.tickCreation`, `FileMenuFlow.tickSelection`, `FileMenuState.copySavedNames` |
| `FileMenuState.NAME_ENTRY_CHARACTER_COUNT` | `FileMenuFlow.tickCreation` |
| `FileMenuState.BG_MAP_BASE` |  |
| `FileMenuState.FILE_NEW_SAVE_SLOT_INDEX_OFFSET` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.FILE_NEW_NAME_OFFSET` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.SAVE_NAME_OFFSETS` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.COPY_SOURCE_NAME_OFFSETS` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.COPY_TARGET_NAME_OFFSETS` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.QUIT_OK_OFFSET` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.FILE_NEW_SAVE_SLOT_1_TILE` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.DARK_BACKGROUND_TILE` | `FileMenuEditing.drawSaveSlotName`, `FileMenuEditing.tileForStoredName` |
| `FileMenuState.sceneProvider` | `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.sceneCache` | `FileMenuEditing.refreshSnapshot` |
| `FileMenuState.selectionCursorYPositions` | `FileMenuEditing.addCursorPair`, `FileMenuEditing.buildSprites`, `FileMenuState.constructor` |
| `FileMenuState.nameEntryCharacterTable` | `FileMenuFlow.tickCreation`, `FileMenuState.constructor` |
| `FileMenuState.nameCursorYPositions` | `FileMenuEditing.buildSprites`, `FileMenuState.constructor` |
| `FileMenuState.nameCursorXPositions` | `FileMenuEditing.buildSprites`, `FileMenuState.constructor` |
| `FileMenuState.namePositionXPositions` | `FileMenuEditing.buildSprites`, `FileMenuState.constructor` |
| `FileMenuState.codepointToTileMap` | `FileMenuEditing.tileForStoredName`, `FileMenuState.constructor` |
| `FileMenuState.saveFilesMask` | `FileMenuEditing.hasSavedFile`, `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.savedNames` | `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCopySource`, `FileMenuFlow.tickErasePick`, `FileMenuFlow.tickSelection`, `FileMenuState.constructor` |
| `FileMenuState.maxHearts` | `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.deathCountsBcd` | `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.deathDigitTiles` | `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.quitOkTiles` | `FileMenuEditing.refreshSnapshot`, `FileMenuState.constructor` |
| `FileMenuState.selectedSlot` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.moveFourRowCursor`, `FileMenuFlow.returnToSelection`, `FileMenuFlow.tickCopyConfirm`, `FileMenuFlow.tickCopySource`, `FileMenuFlow.tickCopyTarget`, `FileMenuFlow.tickCreation`, `FileMenuFlow.tickEraseConfirm`, `FileMenuFlow.tickErasePick`, `FileMenuFlow.tickSelection` |
| `FileMenuState.commandRowArrowShifted` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickSelection` |
| `FileMenuState.mode` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.mode`, `FileMenuFlow.returnToSelection`, `FileMenuFlow.tick`, `FileMenuFlow.tickCopyConfirm`, `FileMenuFlow.tickCopySource`, `FileMenuFlow.tickCopyTarget`, `FileMenuFlow.tickEraseConfirm`, `FileMenuFlow.tickErasePick`, `FileMenuFlow.tickSelection` |
| `FileMenuState.sourceSlot` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCopyConfirm`, `FileMenuFlow.tickCopySource`, `FileMenuFlow.tickCopyTarget`, `FileMenuFlow.tickEraseConfirm`, `FileMenuFlow.tickErasePick` |
| `FileMenuState.targetSlot` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCopyConfirm`, `FileMenuFlow.tickCopySource`, `FileMenuFlow.tickCopyTarget` |
| `FileMenuState.confirmationOk` | `FileMenuEditing.buildSprites`, `FileMenuFlow.tickCopyConfirm`, `FileMenuFlow.tickCopyTarget`, `FileMenuFlow.tickEraseConfirm`, `FileMenuFlow.tickErasePick`, `FileMenuFlow.toggleConfirmation` |
| `FileMenuState.selectedCharacter` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCreation`, `FileMenuFlow.tickSelection` |
| `FileMenuState.namePosition` | `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCreation`, `FileMenuFlow.tickSelection` |
| `FileMenuState.frameCounter` | `FileMenuEditing.addCursorPair`, `FileMenuEditing.buildSprites`, `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tick` |
| `FileMenuState.currentName` | `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.tickCreation`, `FileMenuFlow.tickSelection` |
| `FileMenuState.snapshot` | `FileMenuEditing.refreshSnapshot`, `FileMenuFlow.snapshot` |

Result: depth 3 → 0; four chain files / 606 lines → two final classes / 543 lines (controller 475, name editor 68). All state is private. Two independent name-entry tests cover wrap/reset, START priority and action-copy ownership. Full suite: 2,890 tests, zero failures/errors, one skipped.

## Link motion dispatch

Original chain: LinkStateMachine → LinkBootsMotion → LinkRoomMotion → LinkFrameMotion → LinkMotionLifecycle.

Frame dispatch remains in LinkStateMachine. Pegasus charge/collision/shake state belongs to LinkDash; room identity/physics belongs to LinkRoomPhysics; revolving-door animation/transition state belongs to LinkDoorTraversal. LinkMotionStatus is the explicit small shared state for motion mode, recovery and frame gates. Existing airborne, swimming, equipment and animation components remain separate collaborators.

| State/constant | Referencing methods |
| --- | --- |
| `LinkFrameMotion.frame` | `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.carriedSpeedX` | `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.carriedSpeedY` | `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.featherLaunchFrame` | `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.platformVerticalSpeedForThisUpdate` | `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.mask` | `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.speedX` | `LinkFrameMotion.applyMotion`, `LinkFrameMotion.resolveSpeeds` |
| `Frame.speedY` | `LinkFrameMotion.applyMotion`, `LinkFrameMotion.resolveSpeeds` |
| `LinkMotionLifecycle.dialogGotItemSequence` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkAirborne` | `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.setDebugNoClip`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.linkAnimationState` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.pegasusRunningSpeedX`, `LinkRoomMotion.pegasusRunningSpeedY`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updateDebugNoClip` |
| `LinkMotionLifecycle.linkAnimator` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.tickRevolvingDoor` |
| `LinkMotionLifecycle.linkControls` | `LinkFrameMotion.prepareGroundMotion`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.updateDebugNoClip` |
| `LinkMotionLifecycle.linkDamage` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkEntityEffects` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.linkEquipment` | `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkGroundMotion` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom`, `LinkFrameMotion.applyMotion`, `LinkMotionLifecycle.constructor`, `LinkStateMachine.accessSetGelClinging` |
| `LinkMotionLifecycle.linkItemState` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkMotion` | `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.resolveSpeeds`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updateDebugNoClip`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.linkServices` | `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetPegasusBoots`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotionLifecycle.constructor`, `LinkRoomMotion.groundSpeedIndex`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updateDebugNoClip`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.linkSideView` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.beginFrame`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkSignals` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.linkSwimming` | `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.applyMotion`, `LinkMotionLifecycle.constructor` |
| `Dependencies.dialogGotItemSequence` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkAirborne` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkAnimationState` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkAnimator` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkControls` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkDamage` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkEntityEffects` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkEquipment` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkGroundMotion` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkItemState` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkMotion` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkServices` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkSideView` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkSignals` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `Dependencies.linkSwimming` | `LinkAssembly.bindGroup0`, `LinkMotionLifecycle.constructor` |
| `LinkMotionLifecycle.motionState` | `LinkAirborne.accessRomMotionState`, `LinkAirborne.accessSetPitWarpArrival`, `LinkBootsMotion.accessIsSwimming`, `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkDamage.applySpikeGroundPhysics`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.beginFrame`, `LinkGroundMotion.accessUpdate`, `LinkJumpMotion.tickJump`, `LinkMotion.applyOverworldLedgeCollision`, `LinkRecovery.accessBeginPassOut`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updatePegasusBootsUse`, `LinkSpriteRendering.resolveAnimationState`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.leaveSwimming`, `LinkSwimming.updateRecover` |
| `LinkMotionLifecycle.physicsModifier` | `LinkBootsMotion.accessIsDiving`, `LinkBootsMotion.accessRomPhysicsModifier`, `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRomPhysicsModifier`, `LinkBootsMotion.setRoomPhysicsForRoom`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.clearRoosterCarryState`, `LinkFrameMotion.advanceHazardMotion`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickOverworldLedgeFall`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkRecovery.accessBeginPassOut`, `LinkRecovery.accessPresentGameOverStage`, `LinkSpriteRendering.resolveAnimationState`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.leaveSwimming`, `LinkSwimming.updateRecover`, `LinkSwimming.updateSwimming` |
| `LinkMotionLifecycle.recoverCountdown` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.updateRecover` |
| `LinkMotionLifecycle.recoveringInWater` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkSignals.accessRecoveryBlocksInput`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.updateRecover` |
| `LinkMotionLifecycle.slowWalkingSpeed` | `LinkStateMachine.accessRomSlowWalkingSpeed`, `LinkSwimming.enterSwimmingIfNeeded` |
| `LinkMotionLifecycle.itemUsageContext` | `LinkStateMachine.accessRomItemUsageContext`, `LinkSwimming.enterSwimmingIfNeeded` |
| `LinkMotionLifecycle.revolvingDoorFrame` | `LinkAnimationSelection.submitOam`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkRoomMotion.tickRevolvingDoor`, `LinkSpriteRendering.render` |
| `LinkMotionLifecycle.revolvingDoorTransitionPending` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkRoomMotion.tickRevolvingDoor`, `LinkStateMachine.accessConsumeRevolvingDoorTransition` |
| `LinkMotionLifecycle.revolvingDoorPaletteVariant` | `LinkAnimationSelection.submitOam`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkRoomMotion.tickRevolvingDoor`, `LinkSpriteRendering.render` |
| `LinkMotionLifecycle.romInteractiveMotionBlocked` | `LinkAnimationSelection.showHeldItemPose`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkAnimationSelection.showRomFallenPose`, `LinkAnimationSelection.showStandingShieldDownPose`, `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.writeRomInteractiveMotionBlock`, `LinkEntityEffects.applyHinoxGrabState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.beginFrame`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.romInteractiveMotionBlockValue` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.writeRomInteractiveMotionBlock`, `LinkFrameMotion.beginFrame`, `LinkStateMachine.accessRomInteractiveMotionBlockValue` |
| `LinkMotionLifecycle.physicsRoomLoadToken` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom` |
| `LinkMotionLifecycle.physicsRoomMapId` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom` |
| `LinkMotionLifecycle.physicsRoomId` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom`, `LinkSwimming.applyWaterCurrent`, `LinkSwimming.updateRecover` |
| `LinkMotionLifecycle.physicsRoomCategory` | `LinkBootsMotion.initializeRoomPhysicsForRoom`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkBootsMotion.setRoomPhysicsForRoom` |
| `LinkMotionLifecycle.debugNoClip` | `LinkBootsMotion.accessDebugNoClip`, `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.tryMoveAxis`, `LinkFrameMotion.advanceSpecialMotion`, `LinkRoomMotion.setDebugNoClip` |
| `LinkMotionLifecycle.frameCounter` | `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.beginFrame`, `LinkSwimming.advanceSwimmingMotion` |
| `LinkMotionLifecycle.gelClinging` | `LinkEquipment.isUsingShield`, `LinkFrameMotion.applyMotion`, `LinkSpriteRendering.resolveNormalAnimation`, `LinkStateMachine.accessSetGelClinging` |
| `LinkMotionLifecycle.gelMotionFrame` | `LinkFrameMotion.applyMotion`, `LinkStateMachine.accessSetGelClinging` |
| `LinkMotionLifecycle.pegasusBootsChargeMeter` | `LinkBootsMotion.accessPegasusBootsChargeMeter`, `LinkBootsMotion.resetPegasusBoots`, `LinkFrameMotion.resolveSpeeds`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkMotionLifecycle.pegasusBootsCollisionCountdown` | `LinkBootsMotion.accessPegasusBootsCollisionCountdown`, `LinkFrameMotion.beginFrame`, `LinkRoomMotion.handlePegasusBootsCollision` |
| `LinkMotionLifecycle.pegasusBootsCollisionPosX` | `LinkBootsMotion.accessPegasusBootsCollisionPosX`, `LinkRoomMotion.handlePegasusBootsCollision` |
| `LinkMotionLifecycle.pegasusBootsCollisionPosY` | `LinkBootsMotion.accessPegasusBootsCollisionPosY`, `LinkRoomMotion.handlePegasusBootsCollision` |
| `LinkMotionLifecycle.pendingPegasusScreenShakeCountdown` | `LinkBootsMotion.consumePegasusScreenShakeRequest`, `LinkRoomMotion.handlePegasusBootsCollision` |
| `LinkMotionLifecycle.pendingPegasusScreenShakePhase` | `LinkBootsMotion.consumePegasusScreenShakeRequest`, `LinkRoomMotion.handlePegasusBootsCollision` |

Result: depth 4 → 0; LinkStateMachine is a 489-line dispatcher. LinkDash owns charge/rebound/shake, LinkRoomPhysics owns room-identity defaults and scroll completion, LinkDoorTraversal owns door timing/palette/transition, and LinkMotionStatus holds explicit shared mode/recovery/frame gates. Every state field in these components is private; no component receives LinkStateMachine or Link. Assembly-only dependency wiring lives in LinkAssembly. Full suite: 2,891 tests, zero failures/errors, one skipped; one independent interaction-byte validation test added.

## Link airborne motion

Original chain: LinkAirborne → LinkJumpMotion → LinkAirborneState.

Jump steering, gravity, landing and scripted jump commands are one responsibility in LinkAirborne. Pit probing/slip targets/fall duration/warp-or-damage decisions belong to LinkPitFall. LinkElevation is the small explicit airborne/Z/speed state shared by both; ledge steering stays private to LinkAirborne.

| State/constant | Referencing methods |
| --- | --- |
| `LinkAirborneState.dialogGotItemSequence` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborneState.constructor` |
| `LinkAirborneState.linkAnimationState` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.startPitFall`, `LinkAirborneState.constructor` |
| `LinkAirborneState.linkAnimator` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborneState.constructor`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickJumpAnimation` |
| `LinkAirborneState.linkControls` | `LinkAirborneState.constructor`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.linkDamage` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.tickPitFall`, `LinkAirborneState.constructor` |
| `LinkAirborneState.linkEntityEffects` | `LinkAirborneState.constructor`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.linkItemState` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborneState.constructor` |
| `LinkAirborneState.linkMotion` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkAirborneState.constructor`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.linkOverPit`, `LinkJumpMotion.playLandingSound`, `LinkJumpMotion.refreshGroundStatus`, `LinkJumpMotion.slipTowardPitCenter`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.linkServices` | `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkAirborneState.constructor`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.linkOverPit`, `LinkJumpMotion.playLandingSound`, `LinkJumpMotion.refreshGroundStatus`, `LinkJumpMotion.swordSuppressesJumpAnimation`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.linkSideView` | `LinkAirborne.accessUseRocsFeather`, `LinkAirborneState.constructor`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.linkSignals` | `LinkAirborne.tickPitFall`, `LinkAirborneState.constructor` |
| `LinkAirborneState.linkStateMachine` | `LinkAirborne.accessRomMotionState`, `LinkAirborne.accessSetPitWarpArrival`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborneState.constructor`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickOverworldLedgeFall` |
| `LinkAirborneState.linkSwimming` | `LinkAirborne.accessBounceFromGoomba`, `LinkAirborneState.constructor` |
| `Dependencies.dialogGotItemSequence` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkAnimationState` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkAnimator` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkControls` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkDamage` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkEntityEffects` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkItemState` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkMotion` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkServices` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkSideView` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkSignals` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkStateMachine` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `Dependencies.linkSwimming` | `LinkAirborneState.constructor`, `LinkAssembly.bindGroup1` |
| `LinkAirborneState.airborne` | `LinkAirborne.accessBounceFromGoomba`, `LinkAirborne.accessIsAirborne`, `LinkAirborne.accessSetPitWarpArrival`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.startPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkDamage.hurtBySpikes`, `LinkEntityEffects.applyHinoxGrabState`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkGroundMotion.accessUpdate`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickOverworldLedgeFall`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.setDebugNoClip`, `LinkRoomMotion.updatePegasusBootsUse`, `LinkSideView.finishSideViewMotion`, `LinkSideView.setSideScrolling`, `LinkSideView.updateSideViewMotion`, `LinkSpriteRendering.resolveNormalAnimation`, `LinkSwimming.enterSwimmingIfNeeded` |
| `LinkAirborneState.spinningPitWarpArrival` | `LinkAirborne.accessSetPitWarpArrival`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceSpecialMotion`, `LinkJumpMotion.tickJump`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkAirborneState.zSubPixels` | `LinkAirborne.accessSetPitWarpArrival`, `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.accessZPixels`, `LinkAirborne.startPitFall`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkDamage.applySpikeGroundPhysics`, `LinkDamage.hurtBySpikes`, `LinkEntityEffects.applyHinoxGrabState`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkJumpMotion.tickJump`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.setDebugNoClip`, `LinkSideView.finishSideViewMotion`, `LinkSideView.setSideScrolling`, `LinkSwimming.enterSwimmingIfNeeded` |
| `LinkAirborneState.zVelocity` | `LinkAirborne.accessBounceFromGoomba`, `LinkAirborne.accessSetPitWarpArrival`, `LinkAirborne.accessTickDialogGotItem`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.accessZVelocity`, `LinkAirborne.startPitFall`, `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkDamage.hurtBySpikes`, `LinkEntityEffects.applyHinoxGrabState`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkJumpMotion.tickJump`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.setDebugNoClip`, `LinkSideView.finishSideViewMotion`, `LinkSideView.setSideScrolling`, `LinkSwimming.enterSwimmingIfNeeded` |
| `LinkAirborneState.fallingIntoPit` | `LinkAirborne.accessBounceFromGoomba`, `LinkAirborne.accessIsFallingIntoPit`, `LinkAirborne.accessRomMotionState`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkGroundMotion.accessUpdate`, `LinkJumpMotion.refreshGroundStatus`, `LinkRoomMotion.setDebugNoClip`, `LinkSignals.accessRecoveryBlocksInput`, `LinkSpriteRendering.resolveAnimationState` |
| `LinkAirborneState.groundStatus` | `LinkAirborne.accessBounceFromGoomba`, `LinkAirborne.accessRomMotionState`, `LinkAirborne.accessUseRocsFeather`, `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.applyRoosterFlightState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.refreshGroundStatus`, `LinkJumpMotion.slipTowardPitCenter`, `LinkRoomMotion.setDebugNoClip`, `LinkRoomMotion.updatePegasusBootsUse` |
| `LinkAirborneState.pitSlippingCounter` | `LinkAirborne.tickPitFall`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.slipTowardPitCenter` |
| `LinkAirborneState.fallingFrameCounter` | `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkSpriteRendering.resolveAnimationState` |
| `LinkAirborneState.pitSlipTargetTopLeftX` | `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.slipTowardPitCenter` |
| `LinkAirborneState.pitSlipTargetTopLeftY` | `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.slipTowardPitCenter` |
| `LinkAirborneState.pitSlipPhysicsFlag` | `LinkAirborne.tickPitFall`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.refreshGroundStatus` |
| `LinkAirborneState.hasPitSlipTarget` | `LinkAirborne.tickPitFall`, `LinkJumpMotion.enterPitGroundState`, `LinkJumpMotion.refreshGroundStatus`, `LinkJumpMotion.slipTowardPitCenter` |
| `LinkAirborneState.overworldLedgeFallActive` | `LinkBootsMotion.finishRoomScroll`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.beginFrame`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickJumpAnimation`, `LinkMotion.applyOverworldLedgeCollision` |
| `LinkAirborneState.directionalLedgeJumpMotionLocked` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.tryMoveAxis`, `LinkFrameMotion.beginFrame`, `LinkFrameMotion.prepareGroundMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.directionalLedgeJumpSpeedX` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkFrameMotion.resolveSpeeds`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.directionalLedgeJumpSpeedY` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkFrameMotion.resolveSpeeds`, `LinkJumpMotion.tickJump` |
| `LinkAirborneState.directionalLedgeCollisionCounter` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkMotion.applyOverworldLedgeCollision` |

Result: depth 2 → 0; original three chain files (508 lines) → LinkAirborne (331), LinkPitFall (257), LinkElevation (34). State is private; pit and jump callers use their respective components, with the existing fall, jump, ledge and sound assertions unchanged. Full suite: 2,891 tests, zero failures/errors, one skipped.

## Link animation

Original chain: LinkAnimator → LinkAnimationSelection → LinkSpriteRendering → LinkPoseState.

Pose selection, overrides, jump/push animation counters and scripted presentations form LinkAnimator. LinkSpriteComposer owns the reusable decoded tile buffer and body/equipment drawing; it receives selected pose values rather than an animator/back-reference. LinkPoseTables holds immutable fallback animation table data, separate from selection and drawing.

| State/constant | Referencing methods |
| --- | --- |
| `LinkAnimationSelection.UNSHIELDED_WALK_BASE` | `LinkAnimationSelection.cachePushingAnimation` |
| `LinkPoseState.dialogGotItemSequence` | `LinkAnimator.accessClearDialogGotItem`, `LinkAnimator.accessDialogGotItem`, `LinkAnimator.accessDialogGotItemActive`, `LinkAnimator.accessDialogGotItemCountdown`, `LinkAnimator.accessPresentToadstool`, `LinkAnimator.accessPresentedDialogGotItem`, `LinkPoseState.constructor` |
| `LinkPoseState.linkAirborne` | `LinkAnimator.renderY`, `LinkAnimator.submitEquippedOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.renderEquippedItems`, `LinkSpriteRendering.resolveAnimationState`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.linkAnimationState` | `LinkAnimationSelection.cachePushingAnimation`, `LinkAnimationSelection.queryOverride`, `LinkAnimationSelection.showHeldItemPose`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkAnimationSelection.showRomFallenPose`, `LinkAnimationSelection.showStandingShieldDownPose`, `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkAnimator.accessPresentToadstool`, `LinkAnimator.submitEquippedOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.renderEquippedItems`, `LinkSpriteRendering.resolveAnimationState`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.linkDamage` | `LinkAnimationSelection.showPowerUpPickupPose`, `LinkPoseState.constructor` |
| `LinkPoseState.linkEntityEffects` | `LinkAnimationSelection.submitOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.render`, `LinkSpriteRendering.resolveAnimationState`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.linkEquipment` | `LinkPoseState.constructor`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.linkItemState` | `LinkAnimationSelection.cachePushingAnimation`, `LinkAnimationSelection.showHeldItemPose`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkPoseState.constructor` |
| `LinkPoseState.linkMotion` | `LinkAnimationSelection.cachePushingAnimation`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkAnimationSelection.submitOam`, `LinkAnimator.renderY`, `LinkAnimator.submitEquippedOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.render`, `LinkSpriteRendering.renderEquippedItems` |
| `LinkPoseState.linkServices` | `LinkAnimationSelection.cachePushingAnimation`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkAnimationSelection.submitOam`, `LinkAnimator.submitEquippedOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.render`, `LinkSpriteRendering.renderEquippedItems`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.linkStateMachine` | `LinkAnimationSelection.showHeldItemPose`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkAnimationSelection.showRomFallenPose`, `LinkAnimationSelection.showStandingShieldDownPose`, `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkAnimationSelection.submitOam`, `LinkPoseState.constructor`, `LinkSpriteRendering.render`, `LinkSpriteRendering.resolveAnimationState`, `LinkSpriteRendering.resolveNormalAnimation` |
| `Dependencies.dialogGotItemSequence` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkAirborne` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkAnimationState` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkDamage` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkEntityEffects` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkEquipment` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkItemState` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkMotion` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkServices` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `Dependencies.linkStateMachine` | `LinkAssembly.bindGroup0`, `LinkPoseState.constructor` |
| `LinkPoseState.ANIMATION_STATE` | `LinkFrameMotion.applyMotion`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.SHIELD_USE_ANIMATION_STATE` | `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.MIRROR_SHIELD_USE_ANIMATION_STATE` | `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.LIFTING_ANIMATION_STATE` | `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.SWIMMING_ANIMATION_STATE` | `LinkSpriteRendering.resolveAnimationState` |
| `LinkPoseState.DIVING_ANIMATION_STATE` | `LinkSpriteRendering.resolveAnimationState` |
| `LinkPoseState.JUMP_ANIMATION_STATE` | `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.FALL_ANIMATION_STATE` | `LinkSpriteRendering.resolveAnimationState` |
| `LinkPoseState.composedTiles` | `LinkSpriteRendering.render` |
| `LinkPoseState.tarinShieldPresentation` | `LinkAnimationSelection.clearTarinShieldPresentation`, `LinkAnimationSelection.showStandingShieldDownPose`, `LinkAnimationSelection.showTarinShieldPresentation`, `LinkAnimator.accessIsTarinShieldPresentationVisible`, `LinkBootsMotion.resetTransientStateForDebug` |
| `LinkPoseState.tarinShieldPalette` | `LinkAnimationSelection.showTarinShieldPresentation`, `LinkAnimator.accessTarinShieldPalette`, `LinkBootsMotion.resetTransientStateForDebug` |
| `LinkPoseState.romAnimationStateOverride` | `LinkAirborne.accessTickDialogGotItem`, `LinkAnimationSelection.showHeldItemPose`, `LinkAnimationSelection.showPowerUpPickupPose`, `LinkAnimationSelection.showRomFallenPose`, `LinkAnimationSelection.showStandingShieldDownPose`, `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkEntityEffects.applyHinoxGrabState`, `LinkEntityEffects.applyWarpState3`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkRecovery.accessPresentPassOut`, `LinkRoomMotion.tickRevolvingDoor`, `LinkSpriteRendering.resolveAnimationState`, `LinkSwimming.updateRecover` |
| `LinkPoseState.swordAcquisitionBaseRomDirection` | `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.beginFrame` |
| `LinkPoseState.swordAcquisitionAnimationState` | `LinkAnimationSelection.showSwordAcquisitionFinalPose`, `LinkAnimationSelection.showSwordAcquisitionSpinPose`, `LinkAnimator.submitEquippedOam`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.beginFrame`, `LinkSpriteRendering.renderEquippedItems`, `LinkSpriteRendering.resolveAnimationState` |
| `LinkPoseState.marinWakeUpBedVariant` | `LinkAnimationSelection.showMarinWakeUpBed`, `LinkAnimator.accessIsMarinWakeUpBedVisible`, `LinkAnimator.accessMarinWakeUpBedVariant`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.applyMotion` |
| `LinkPoseState.bedJumpRenderZ` | `LinkAirborne.accessTickDialogGotItem`, `LinkAnimator.renderY`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame` |
| `LinkPoseState.ledgeJumpRenderY` | `LinkAnimator.renderY`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkFrameMotion.beginFrame`, `LinkMotion.applyOverworldLedgeCollision`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.jumpAnimationCounter` | `LinkAirborne.accessUseRocsFeather`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickJumpAnimation`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkSideView.finishSideViewMotion` |
| `LinkPoseState.jumpAnimationFrame` | `LinkAirborne.accessUseRocsFeather`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkJumpMotion.tickJump`, `LinkJumpMotion.tickJumpAnimation`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkSideView.finishSideViewMotion`, `LinkSpriteRendering.resolveNormalAnimation` |
| `LinkPoseState.pushingAnimationState` | `LinkAnimationSelection.cachePushingAnimation`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkSpriteRendering.resolveNormalAnimation` |

Result: depth 3 → 0; LinkAnimator 449 lines, LinkSpriteComposer 284, LinkPoseTables 52. Pose state and tile buffers are private, and drawing receives selected pose values rather than the animator. Aggregate debug reset moved from frame dispatch to the existing motion command role; game-over completion invokes it before clearing recovery counters in the same order. Full suite: 2,891 tests, zero failures/errors, one skipped.

## Link position and collision

Original chain: LinkMotion → LinkCollisionMotion → LinkPosition.

LinkPosition becomes a final coordinate/speed owner, including room-entry and final-position shadows. LinkCollisionState is the explicit probe/mask/door-warp state shared with room-entry commands. LinkMotion owns collision and ledge orchestration and composes the coordinate owner; no positional arithmetic moves into presentation or public role objects.

| State/constant | Referencing methods |
| --- | --- |
| `LinkPosition.linkAirborne` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.accessRomSwordCollisionY`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkPosition.constructor` |
| `LinkPosition.linkAnimationState` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkMotion.accessRomCollisionProbePixelX`, `LinkMotion.accessRomCollisionProbePixelY`, `LinkPosition.constructor` |
| `LinkPosition.linkAnimator` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkPosition.constructor` |
| `LinkPosition.linkControls` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkMotion.ledgeUnstuckingPositionBlocked`, `LinkPosition.constructor` |
| `LinkPosition.linkDamage` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkPosition.constructor` |
| `LinkPosition.linkEntityEffects` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.applyOverworldLedgeCollision`, `LinkPosition.constructor` |
| `LinkPosition.linkEquipment` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkPosition.constructor` |
| `LinkPosition.linkGroundMotion` | `LinkPosition.constructor` |
| `LinkPosition.linkItemState` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkPosition.constructor` |
| `LinkPosition.linkServices` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.directionalLedgeHalfIsPassable`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.ledgeUnstuckingPositionBlocked`, `LinkPosition.constructor` |
| `LinkPosition.linkSideView` | `LinkMotion.clearRomPositionIncrement`, `LinkMotion.setRoomEntryPixelPosition`, `LinkMotion.setRoomScrollEntryPixelPosition`, `LinkPosition.constructor` |
| `LinkPosition.linkStateMachine` | `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkPosition.constructor` |
| `Dependencies.linkAirborne` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkAnimationState` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkAnimator` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkControls` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkDamage` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkEntityEffects` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkEquipment` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkGroundMotion` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkItemState` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkServices` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkSideView` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `Dependencies.linkStateMachine` | `LinkAssembly.bindGroup1`, `LinkPosition.constructor` |
| `LinkPosition.subX` | `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkBootsMotion.finishRoomScroll`, `LinkCollisionMotion.tryMoveAxis`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkFrameMotion.advanceHazardMotion`, `LinkJumpMotion.slipTowardPitCenter`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.applyRomFinalPosition`, `LinkMotion.captureRomFinalPosition`, `LinkMotion.markRoomEntryPosition`, `LinkMotion.pixelX`, `LinkMotion.restoreRomFinalPosition`, `LinkMotion.restoreRomFinalPositionX`, `LinkMotion.setPixelPosition`, `LinkMotion.setRoomScrollEntryPixelPosition`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updateDebugNoClip`, `LinkSideView.applySideViewPlatformContact`, `LinkSideView.finishSideViewMotion`, `LinkSideView.moveSideViewAndCollide`, `LinkSwimming.updateRecover` |
| `LinkPosition.subY` | `LinkAirborne.startPitFall`, `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible`, `LinkBootsMotion.finishRoomScroll`, `LinkCollisionMotion.tryMoveAxis`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkFrameMotion.advanceHazardMotion`, `LinkJumpMotion.slipTowardPitCenter`, `LinkMotion.applyOverworldLedgeCollision`, `LinkMotion.applyRomFinalPosition`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkMotion.captureRomFinalPosition`, `LinkMotion.markRoomEntryPosition`, `LinkMotion.pixelY`, `LinkMotion.restoreRomFinalPosition`, `LinkMotion.setPixelPosition`, `LinkMotion.setRoomScrollEntryPixelPosition`, `LinkRoomMotion.tickRevolvingDoor`, `LinkRoomMotion.updateDebugNoClip`, `LinkSideView.applySideViewPlatformContact`, `LinkSideView.applySideViewPotContact`, `LinkSideView.finishSideViewMotion`, `LinkSideView.moveSideViewAndCollide`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.updateRecover` |
| `LinkPosition.lastRomSpeedX` | `LinkAirborne.accessTickDialogGotItem`, `LinkBootsMotion.finishRoomScroll`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkDamage.hurtBySpikes`, `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkJumpMotion.tickJump`, `LinkMotion.accessRomSpeedX`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkMotion.clearRomPositionIncrement`, `LinkRecovery.accessBeginPassOut`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.updateDebugNoClip`, `LinkSideView.finishSideViewMotion`, `LinkSwimming.advanceSwimmingMotion`, `LinkSwimming.applyWaterCurrent`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.updateRecover`, `LinkSwimming.updateSwimming` |
| `LinkPosition.lastRomSpeedY` | `LinkAirborne.accessTickDialogGotItem`, `LinkBootsMotion.finishRoomScroll`, `LinkCollisionMotion.applyDirectionalLedgeCollision`, `LinkDamage.hurtBySpikes`, `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceHazardMotion`, `LinkFrameMotion.applyMotion`, `LinkFrameMotion.beginFrame`, `LinkJumpMotion.tickJump`, `LinkMotion.accessRomSpeedY`, `LinkMotion.beginOverworldLedgeUnstuckingMotion`, `LinkMotion.clearRomPositionIncrement`, `LinkRecovery.accessBeginPassOut`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.updateDebugNoClip`, `LinkSideView.applySideViewPlatformContact`, `LinkSideView.applySideViewPotContact`, `LinkSideView.finishSideViewMotion`, `LinkSwimming.advanceSwimmingMotion`, `LinkSwimming.applyWaterCurrent`, `LinkSwimming.enterSwimmingIfNeeded`, `LinkSwimming.updateRecover`, `LinkSwimming.updateSwimming` |
| `LinkPosition.forcedSpeedPending` | `LinkAnimationSelection.showPowerUpPickupPose`, `LinkBootsMotion.resetTransientStateForDebug`, `LinkDamage.hurtBySpikes`, `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotion.applyRomSpeed`, `LinkMotion.applyRomSpeedX`, `LinkMotion.clearRomPositionIncrement`, `LinkRecovery.accessBeginPassOut`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkRoomMotion.updatePegasusBootsUse`, `LinkSideView.updateSideViewMotion`, `LinkSwimming.advanceSwimmingMotion` |
| `LinkPosition.forcedSpeedXOnly` | `LinkBootsMotion.resetTransientStateForDebug`, `LinkDamage.hurtBySpikes`, `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkEntityEffects.clearRoosterCarryState`, `LinkEntityEffects.leaveMarinWakeUpBed`, `LinkFrameMotion.advanceSpecialMotion`, `LinkFrameMotion.resolveSpeeds`, `LinkMotion.applyRomSpeed`, `LinkMotion.applyRomSpeedX`, `LinkMotion.clearRomPositionIncrement`, `LinkRoomMotion.updatePegasusBootsUse`, `LinkSideView.updateSideViewMotion`, `LinkSwimming.advanceSwimmingMotion` |
| `LinkPosition.forcedSpeedX` | `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkFrameMotion.resolveSpeeds`, `LinkMotion.applyRomSpeed`, `LinkMotion.applyRomSpeedX`, `LinkMotion.clearRomPositionIncrement`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkSideView.updateSideViewMotion`, `LinkSwimming.advanceSwimmingMotion` |
| `LinkPosition.forcedSpeedY` | `LinkDamage.setCollisionIgnoreFrames`, `LinkDamage.tickRecoil`, `LinkFrameMotion.resolveSpeeds`, `LinkMotion.applyRomSpeed`, `LinkMotion.clearRomPositionIncrement`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkSideView.updateSideViewMotion`, `LinkSwimming.advanceSwimmingMotion` |
| `LinkPosition.romCollisionType` | `LinkAnimationState.accessHasFacingCollision`, `LinkCollisionMotion.tryMoveAxis`, `LinkDamage.tickRecoil`, `LinkEntityEffects.applyLikeLikeCapture`, `LinkFrameMotion.beginFrame`, `LinkMotion.accessRomCollisionType`, `LinkRoomMotion.handlePegasusBootsCollision`, `LinkSideView.moveSideViewAndCollide` |
| `LinkPosition.doorWarpPending` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `LinkMotion.accessConsumeDoorWarp`, `LinkMotion.markRoomEntryPosition` |
| `LinkPosition.collisionProbePixelX` | `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.accessRomCollisionProbePixelX` |
| `LinkPosition.collisionProbePixelY` | `LinkCollisionMotion.tryMoveAxis`, `LinkMotion.accessRomCollisionProbePixelY` |
| `LinkPosition.lastSafeSubX` | `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible` |
| `LinkPosition.lastSafeSubY` | `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible` |
| `LinkPosition.hasLastSafePosition` | `LinkAirborne.tickPitFall`, `LinkAirborne.updateLastSafePositionIfPossible` |
| `LinkPosition.romFinalSubX` | `LinkMotion.captureRomFinalPosition`, `LinkMotion.restoreRomFinalPosition`, `LinkMotion.restoreRomFinalPositionX`, `LinkMotion.romFinalPositionX` |
| `LinkPosition.romFinalSubY` | `LinkMotion.captureRomFinalPosition`, `LinkMotion.restoreRomFinalPosition` |
| `LinkPosition.hasRomFinalPosition` | `LinkMotion.captureRomFinalPosition`, `LinkMotion.restoreRomFinalPosition`, `LinkMotion.restoreRomFinalPositionX`, `LinkMotion.romFinalPositionX` |
| `LinkPosition.roomEntrySubX` | `LinkAirborne.tickPitFall`, `LinkMotion.markRoomEntryPosition`, `LinkMotion.roomEntryPixelX`, `LinkSwimming.updateRecover` |
| `LinkPosition.roomEntrySubY` | `LinkAirborne.tickPitFall`, `LinkMotion.markRoomEntryPosition`, `LinkMotion.roomEntryPixelY`, `LinkSwimming.updateRecover` |
| `LinkPosition.hasRoomEntryPosition` | `LinkAirborne.tickPitFall`, `LinkMotion.markRoomEntryPosition`, `LinkMotion.roomEntryPixelX`, `LinkMotion.roomEntryPixelY`, `LinkSwimming.updateRecover` |

Result: depth 2 → 0; LinkMotion 389 lines, LinkPosition 347, LinkCollisionState 38. Coordinate and collision fields are private. Two independent position tests check signed-byte integration, fractional final-position shadows and validation before mutation. Full suite: 2,893 tests, zero failures/errors, one skipped.

## Sword swing and spin

Original chain: Sword → SwordChargeMotion → SwordAttackMotion → SwordState.

Sword owns ordinary swing/charge, movement/facing gates and the public equipment API, using the existing SwordRenderer and SwordPalette. SwordSpin owns the queued-start delay, remaining spin frames, sector clock and base direction plus their ROM/fallback lookups; it returns animation values to the swing orchestrator without receiving Sword or its state.

| State/constant | Referencing methods |
| --- | --- |
| `SwordState.DRAW_FRAMES` | `SwordAttackMotion.onPress` |
| `SwordState.SWING_START_FRAMES` | `SwordAttackMotion.tick` |
| `SwordState.SWING_MIDDLE_FRAMES` | `SwordAttackMotion.tick` |
| `SwordState.SPIN_ATTACK_FRAMES` | `SwordAttackMotion.tick`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.SPIN_ANIMATION_STATE_BY_BASE_DIRECTION` | `SwordChargeMotion.spinAnimationState` |
| `SwordState.SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION` | `SwordChargeMotion.effectiveDirection` |
| `SwordState.swordTables` | `SwordAttackMotion.overrideAnimationState`, `SwordChargeMotion.effectiveDirection`, `SwordChargeMotion.enemyCollisionBox`, `SwordChargeMotion.spinAnimationState`, `SwordChargeMotion.staticCollisionMapIndex`, `SwordState.constructor` |
| `SwordState.renderer` | `SwordAttackMotion.render`, `SwordChargeMotion.submitOam`, `SwordState.constructor` |
| `SwordState.soundSink` | `SwordAttackMotion.onPress`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.tick`, `SwordState.constructor` |
| `SwordState.randomByteSupplier` | `SwordAttackMotion.onPress`, `SwordState.constructor` |
| `SwordState.swordPalette` | `SwordAttackMotion.render`, `SwordChargeMotion.submitOam`, `SwordState.constructor` |
| `SwordState.beamTarget` | `SwordAttackMotion.onPress`, `SwordState.constructor` |
| `SwordState.state` | `SwordAttackMotion.blocksMotion`, `SwordAttackMotion.locksFacing`, `SwordAttackMotion.onPress`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.overrideAnimationState`, `SwordAttackMotion.releaseHoldWithoutSpin`, `SwordAttackMotion.render`, `SwordAttackMotion.state`, `SwordAttackMotion.staticCollisionActive`, `SwordAttackMotion.tick`, `SwordChargeMotion.advanceSpinAttackAnimation`, `SwordChargeMotion.chargedFlashActive`, `SwordChargeMotion.effectiveBladeRenderState`, `SwordChargeMotion.effectiveLinkAnimationState`, `SwordChargeMotion.endSpinAttack`, `SwordChargeMotion.enemyCollisionBox`, `SwordChargeMotion.resetAfterGameOver`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin`, `SwordChargeMotion.submitOam` |
| `SwordState.timer` | `SwordAttackMotion.onPress`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.animationMotionBlocked` | `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.motionBlockedBeforeAnimation` | `SwordAttackMotion.blocksTopDownMotion`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.facingLockCountdown` | `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.facingLockedBeforeAnimation` | `SwordAttackMotion.locksTopDownFacing`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.suppressesJumpAnimation` | `SwordAttackMotion.suppressesJumpAnimation`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetAfterGameOver` |
| `SwordState.charge` | `SwordAttackMotion.charge`, `SwordAttackMotion.onPress`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.releaseHoldWithoutSpin`, `SwordAttackMotion.tick`, `SwordChargeMotion.chargedFlashActive`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.frameCounter` | `SwordAttackMotion.tick`, `SwordChargeMotion.chargedFlashActive` |
| `SwordState.spinFramesRemaining` | `SwordAttackMotion.tick`, `SwordChargeMotion.advanceSpinAttackAnimation`, `SwordChargeMotion.endSpinAttack`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.spinAttackActive`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.spinFrameCounter` | `SwordAttackMotion.tick`, `SwordChargeMotion.advanceSpinAttackAnimation`, `SwordChargeMotion.endSpinAttack`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.spinSector` | `SwordChargeMotion.advanceSpinAttackAnimation`, `SwordChargeMotion.endSpinAttack`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.spinSector`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.spinBaseDirection` | `SwordAttackMotion.onRelease`, `SwordChargeMotion.effectiveDirection`, `SwordChargeMotion.spinAnimationState`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.lastKnownDirection` | `SwordAttackMotion.onRelease`, `SwordAttackMotion.overrideAnimationState`, `SwordAttackMotion.render`, `SwordChargeMotion.attackDirection`, `SwordChargeMotion.enemyCollisionBox`, `SwordChargeMotion.staticCollisionMapIndex` |
| `SwordState.spinAttackQueued` | `SwordAttackMotion.locksFacing`, `SwordAttackMotion.locksTopDownFacing`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `SwordState.queuedSpinDelayFrames` | `SwordAttackMotion.onRelease`, `SwordAttackMotion.tick`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin` |
| `CollisionBox.active` |  |
| `CollisionBox.x` |  |
| `CollisionBox.width` |  |
| `CollisionBox.y` |  |
| `CollisionBox.height` |  |
| `Sword.STATE_NONE` | `SwordAttackMotion.blocksMotion`, `SwordAttackMotion.locksFacing`, `SwordAttackMotion.onPress`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.overrideAnimationState`, `SwordAttackMotion.releaseHoldWithoutSpin`, `SwordAttackMotion.render`, `SwordAttackMotion.staticCollisionActive`, `SwordAttackMotion.tick`, `SwordChargeMotion.endSpinAttack`, `SwordChargeMotion.resetAfterGameOver`, `SwordChargeMotion.resetSpinAttack`, `SwordChargeMotion.startSwordAcquisitionSpin`, `SwordChargeMotion.submitOam` |
| `Sword.STATE_DRAW` | `SwordAttackMotion.onPress`, `SwordAttackMotion.tick` |
| `Sword.STATE_SWING_START` | `SwordAttackMotion.tick` |
| `Sword.STATE_SWING_MIDDLE` | `SwordAttackMotion.tick` |
| `Sword.STATE_SWING_END` | `SwordAttackMotion.tick`, `SwordChargeMotion.effectiveLinkAnimationState` |
| `Sword.STATE_HOLDING` | `LinkCollisionMotion.leadingEdgeCollisionMask`, `SwordAttackMotion.blocksMotion`, `SwordAttackMotion.onRelease`, `SwordAttackMotion.releaseHoldWithoutSpin`, `SwordAttackMotion.staticCollisionActive`, `SwordAttackMotion.tick`, `SwordChargeMotion.chargedFlashActive`, `SwordChargeMotion.effectiveLinkAnimationState`, `SwordChargeMotion.enemyCollisionBox` |
| `Sword.MAX_CHARGE` | `SwordAttackMotion.onRelease`, `SwordAttackMotion.tick`, `SwordChargeMotion.chargedFlashActive` |

Result: depth 3 → 0; four chain files (740 lines) → Sword (398) and SwordSpin (159), retaining the existing renderer/palette collaborators. Spin queue/delay/sector/frame/base-direction state is private and independent of Sword. Two independent tests cover release delay, 25 visible frames, invalid-input atomicity and unknown inactive directions. Full suite: 2,895 tests, zero failures/errors, one skipped.

## Compiled inheritance guard

The shared class-file reader now reads super_class and PermittedSubclasses after the constant pool, skipping member/attribute payloads without loading classes. NoImplementationInheritanceTest checks every main-output class; only JDK/LWJGL framework superclasses and explicitly permitted sealed children pass without an exception. A second test checks the reader against compiled sealed, record and enum fixtures.

The two headless driver aliases also had implementation inheritance: State → HeadlessState and Diagnostics → SessionDiagnostics. Their original public names/accessors now implement the corresponding interface contracts and compose ReplayCheckpoint / SessionDiagnosticSnapshot. Subtype use through the contracts is retained; both superclass depths are now zero.

Temporary off-limits exceptions: GPU, RomTileLoader, SfxCpu, EntitySpritePainter (explicit coordinator correction), and EntitySpriteTileSnapshot.FrozenTile (additional immutable Tile subclass found by the compiled audit). All are in render/gpu/audio and assigned to that engineer. No owned package has an exception.

Full suite: 2,897 tests, zero failures/errors, one skipped.

## Whole-owner helper audit

Audit scope includes all owned main packages plus app/gameplay; the requested filename patterns produce these three candidates before cleanup:

| Candidate | Decision / responsibility |
| --- | --- |
| `HeadlessStateValues` | Removed: every method took the complete checkpoint solely to supply its equality/hash/text implementation. Those methods now live with private checkpoint fields in `ReplayCheckpoint`; the public driver State keeps its original State-only equality contract. |
| `ChestItemValues` | Retained: a leaf ROM chest-variant constant table used by reward decoding; it has no methods or owner state argument. |
| `SessionLifecycle` | Retained: the consumer-owned `requestStop()` interface connecting screen input to session shutdown; no implementation or owner state is hidden here. |

The broader new-component audit found no whole-owner slicing helpers. Stateless `EntitySpriteGeometry` / `IntroPaletteMath` perform coordinate/color algorithms on their input values. Registration families build immutable recipes; `LinkPoseTables` holds animation lookup data. Published Link roles implement narrow contracts and retain the components each command uses, rather than taking Link. Small explicit shared state objects contain only their named phase/elevation/motion/probe state.

Cleanup also removed 38 unused package-private component accessors, three unused dependency references, and accessor indirection within state owners. No behavioural branch or test assertion was removed. Full suite remains 2,897 tests, zero failures/errors, one skipped.

## Final shape metrics

Scope: main sources in entity, equipment, state, dialog, ui, cutscene, startup, scene, physics, vfx, app and gameplay. The testing/architecture ownership is test-only and is excluded from main-source metrics. Baseline is the initial merged develop commit `c122c0d8`; no off-limits package is counted. Source bins include blank/comment lines and use non-overlapping inclusive ranges.

| Lines per main file | Before | After |
| --- | ---: | ---: |
| 0–100 | 216 | 220 |
| 101–200 | 71 | 65 |
| 201–300 | 33 | 21 |
| 301–400 | 9 | 13 |
| 401–500 | 0 | 7 |
| >500 | 0 | 0 |

| Metric | Before | After |
| --- | ---: | ---: |
| Java files | 329 | 326 |
| Source lines | 30,952 | 31,921 |
| Largest file | 385 | 484 |
| Class declarations (including nested/anonymous; excluding records/enums/interfaces) | 295 | 289 |
| Record declarations | 86 | 89 |
| Enum declarations | 25 | 26 |
| Interface declarations | 47 | 49 |
| Project-class superclass edges | 35 | 0 |
| Maximum project-class inheritance depth | 6 | 0 |

The line increase is explicit composition, encapsulation and public-contract delegation, not a claim that every metric shrank. The final largest file is IntroSequence (484 lines). The sprite catalog commit message misstated its original facade size as 235 lines; the verified baseline is **56** lines (1,281 across its chain). The following table is authoritative.

| Former chain | Depth before → after | Chain files / lines before | Resulting files / lines | Facade lines before → after |
| --- | --- | ---: | ---: | --- |
| DialogController | 4 → 0 | 5 / 672 | 2 / 566 | 40 → 431 |
| IntroSequence | 6 → 0 | 7 / 1,264 | 10 / 1,750 | 37 → 484 |
| EntitySpriteHandlerCatalog | 6 → 0 | 7 / 1,281 | 8 / 1,604 | 56 → 411 |
| FileMenuController | 3 → 0 | 4 / 606 | 2 / 543 | 38 → 475 |
| LinkStateMachine | 4 → 0 | 5 / 949 | 5 / 1,007 | 76 → 448 |
| LinkAirborne | 2 → 0 | 3 / 508 | 3 / 593 | 162 → 339 |
| LinkAnimator | 3 → 0 | 4 / 818 | 3 / 756 | 161 → 418 |
| LinkMotion | 2 → 0 | 3 / 768 | 3 / 752 | 352 → 389 |
| Sword | 3 → 0 | 4 / 740 | 2 / 557 | 79 → 398 |

Chain totals count the replacements below, excluding unchanged collaborators and the sixteen pre-existing sprite registration-family files. Those families now return immutable recipes instead of calling the catalog. Aggregate debug reset lives in the existing DefaultLinkMotionRole, and assembly-only dependency wiring remains in LinkAssembly.

### Dialog responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `DialogController` | 431 | Reveal, scroll, choice and sound-request state machine. |
| `DialogText` | 135 | Name substitution and width-aware layout. |

### Intro responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `IntroSequence` | 484 | Stage transitions and frame orchestration. |
| `IntroBeachActors` | 279 | Marin and inert-Link actor motion/timers. |
| `IntroWater` | 121 | Wave fractions, offsets and map animation. |
| `IntroSpriteComposer` | 161 | Decoded sprite tables, scratch and ordered RNG use. |
| `IntroFramePublisher` | 401 | Palette fades and retained/published frame snapshots. |
| `IntroPhaseState` | 110 | Explicit stage/substate/fade counters. |
| `IntroVideoMemory` | 79 | Explicit mutable map, scroll and pending video writes. |
| `IntroClock` | 20 | Wrapping intro frame clock. |
| `IntroPaletteMath` | 59 | Pure RGB555 palette arithmetic. |
| `IntroResources` | 36 | Bundled-ROM default construction. |

### Entity sprite catalog responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `EntitySpriteHandlerCatalog` | 411 | Recipe dispatch and public sprite catalog API. |
| `EntitySpriteRegistrationData` | 80 | Immutable indexed family recipes and contextual selection priority. |
| `EntitySpriteRecipe` | 97 | Immutable decoding recipe value. |
| `EntitySpriteDecoder` | 169 | Owned ROM reads and bounded OAM decoding. |
| `EntitySpriteStates` | 339 | State-dependent enemy/companion sprite selection. |
| `BossSpriteComposer` | 183 | Boss body, segment and tail compositions. |
| `EntityEffectSprites` | 217 | Transient effects, projectiles and pickup compositions. |
| `EntitySpriteGeometry` | 108 | Pure signed OAM offsets and pair/rectangle composition. |

### File menu responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `FileMenuController` | 475 | Slot/copy/erase transitions and frame publication. |
| `FileNameEntry` | 68 | Character grid, insertion position and five-byte name buffer. |

### Link motion dispatch responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `LinkStateMachine` | 448 | Ordered per-frame motion dispatch. |
| `LinkDash` | 183 | Boot charge, rebound and pending shake. |
| `LinkRoomPhysics` | 148 | Room identity, physics defaults and scroll completion. |
| `LinkDoorTraversal` | 86 | Revolving-door pose, palette and transition timing. |
| `LinkMotionStatus` | 142 | Explicit mode, recovery and frame-gate state. |

### Link airborne motion responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `LinkAirborne` | 339 | Jump steering, gravity, landing and scripted jump commands. |
| `LinkPitFall` | 219 | Pit probes, slip target, fall duration and warp/damage decision. |
| `LinkElevation` | 35 | Explicit airborne flag, subpixel Z and vertical speed. |

### Link animation responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `LinkAnimator` | 418 | Pose selection, scripted presentations and jump/push counters. |
| `LinkSpriteComposer` | 286 | Body tile composition and equipment draw order. |
| `LinkPoseTables` | 52 | ROM fallback animation lookup tables. |

### Link position and collision responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `LinkMotion` | 389 | Collision probes and ledge orchestration. |
| `LinkPosition` | 324 | XY subpixels/speeds plus room-entry and final-position shadows. |
| `LinkCollisionState` | 39 | Explicit collision mask/probe pixels and door-warp request. |

### Sword swing and spin responsibilities

| Resulting type | Lines | Responsibility |
| --- | ---: | --- |
| `Sword` | 398 | Swing/charge state, equipment API and movement/facing gates. |
| `SwordSpin` | 159 | Queued start, spin sector/frame timing and direction lookups. |

`EntitySpritePainter → EntityOamPainter` was deliberately left untouched, per the coordinator correction; it belongs to render. The temporary guard exceptions are listed above.
