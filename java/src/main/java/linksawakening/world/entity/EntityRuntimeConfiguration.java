package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;

/** Room entity configuration preserving source-shaped handler inputs. */
public final class EntityRuntimeConfiguration {
    private final RoomEntityRuntime runtime;

    EntityRuntimeConfiguration(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    public void setBoomerangSoundCounterForFrame(int counter) {
        if (counter < 0 || counter >= runtime.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        runtime.boomerangSfxCounter = counter;
        runtime.boomerangSoundClockProvided = true;
    }

    public void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        EntitySlotValidation.validateByte(targetAddress, "Torch palette target");
        runtime.torchPaletteTargetAddress = targetAddress;
        runtime.torchPaletteTransitionActive = active;
    }

    public void setLinkVelocityZ(int velocity) {
        runtime.linkState.currentLinkVelocityZ = velocity & 0xFF;
    }

    public void setSpriteSelection(EntitySpriteSelection selection) {
        runtime.spriteSelection = selection;
    }

    public void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        runtime.groundInteraction = groundInteraction == null
                ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.unchanged(entity, 0)
                : groundInteraction;
    }

    public void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        runtime.backgroundInteraction = backgroundInteraction;
    }

    public void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        runtime.objectQuery = objectQuery;
    }

    public void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        runtime.objectIntersectionQuery = objectIntersectionQuery;
    }

    public void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    public void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        runtime.linkState.actionButtonAHeld = actionButtonAHeld;
        runtime.linkState.actionButtonBHeld = actionButtonBHeld;
        runtime.linkState.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
    }

    public void setJoypadHeld(boolean joypadHeld) {
        runtime.linkState.joypadHeld = joypadHeld;
    }

    public void setPressedButtonsMask(int pressedButtonsMask) {
        EntitySlotValidation.validateByte(pressedButtonsMask, "Pressed buttons mask");
        runtime.linkState.linkPressedButtonsMask = pressedButtonsMask;
    }

    public void setDialogActive(boolean dialogActive) {
        runtime.linkState.dialogActive = dialogActive;
    }

    public void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        EntitySlotValidation.validateByte(dialogCooldown, "Dialog cooldown");
        EntitySlotValidation.validateByte(windowY, "Window Y");
        runtime.linkState.inventoryAppearing = inventoryAppearing;
        runtime.linkState.dialogCooldown = dialogCooldown;
        runtime.linkState.windowY = windowY;
    }

    public void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        runtime.linkState.dialogAskSelectionIndex = selectionIndex;
    }

    public void setActiveMusic(boolean activeMusic) {
        runtime.activeMusic = activeMusic;
    }

    public void setBowWowState(int bowWowState) {
        runtime.bowWowState = bowWowState & 0xFF;
    }

    public void setLikeLikeLinkInventory(int itemA, int itemB) {
        EntitySlotValidation.validateByte(itemA, "Link A inventory slot");
        EntitySlotValidation.validateByte(itemB, "Link B inventory slot");
        runtime.linkState.linkItemA = itemA;
        runtime.linkState.linkItemB = itemB;
        runtime.linkState.linkInventoryConfigured = true;
    }

    public void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        runtime.linkState.powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    public void setBombButtonHeld(boolean bombButtonHeld) {
        runtime.linkState.bombButtonHeld = bombButtonHeld;
    }

    public void setLinkAttackStepAnimationCountdown(int countdown) {
        EntitySlotValidation.validateByte(countdown, "Link attack-step animation countdown");
        runtime.linkState.linkAttackStepAnimationCountdown = countdown;
    }

    public void setLinkFinalPositionX(int positionX) {
        EntitySlotValidation.validateByte(positionX, "Link final position X");
        runtime.linkState.currentLinkFinalPositionX = positionX;
        runtime.linkState.currentLinkFinalPositionXConfigured = true;
    }

    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        runtime.linkState.fastLiftTransition = redTunic || pieceOfPower;
    }

    public void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: " + linkC13B);
        }
        runtime.liftedLinkC13B = linkC13B;
    }

    public void setGroundInteractionSideScrolling(boolean sideScrolling) {
        runtime.groundInteractionSideScrolling = sideScrolling;
    }

    public void setEnemyDropResolver(EnemyDropResolver resolver) {
        runtime.enemyDropResolver = resolver;
    }

    public void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        runtime.enemyDropCounters = Objects.requireNonNull(counters, "Drop counters cannot be null");
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        EntitySlotValidation.validateEnemyDropByte(maxHearts, "Maximum hearts");
        EntitySlotValidation.validateEnemyDropByte(health, "Health");
        runtime.enemyDropMaxHearts = maxHearts;
        runtime.enemyDropHealth = health;
        runtime.enemyDropLowHealth = null;
        runtime.enemyDropActivePowerUp = activePowerUp;
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        runtime.enemyDropLowHealth = lowHealth;
    }

    public void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: " + mapId);
        }
        runtime.entityMapId = mapId;
    }

    public void setEntityRoomId(int roomId) {
        EntitySlotValidation.validateByte(roomId, "Entity room id");
        runtime.entityRoomId = roomId;
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.secretSeashellMotion.ensureInitialized(entity.slot(), roomId);
                runtime.entityOptions1Override[entity.slot()] = runtime.secretSeashellMotion.privateState3(
                                        entity.slot())
                                != 0
                        ? runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                        : runtime.SECRET_SEASHELL_REVEALED_OPTIONS1;
            }
        }
    }

    public void setEntityRoomStatus(int roomStatus) {
        EntitySlotValidation.validateByte(roomStatus, "Entity room status");
        runtime.entityRoomStatus = roomStatus;
    }

    public void setDungeonMinibossReady(boolean ready) {
        runtime.dungeonMinibossReady = ready;
    }

    public void setScreenShakeCountdown(int countdown) {
        runtime.slimeEye.setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        EntitySlotValidation.validateByte(collisionX, "Pegasus collision X");
        EntitySlotValidation.validateByte(collisionY, "Pegasus collision Y");
        runtime.secretSeashellScreenShakeActive = screenShakeActive;
        runtime.secretSeashellPegasusCollisionActive = collisionActive;
        runtime.secretSeashellPegasusCollisionCoordinatesKnown = true;
        runtime.secretSeashellPegasusCollisionX = collisionX;
        runtime.secretSeashellPegasusCollisionY = collisionY;
    }

    public void setHouseNpcStoryState(int tarinFlag, int tradeItem, int instrument3Flags, int walrusStatus) {
        EntitySlotValidation.validateByte(tarinFlag, "Tarin flag");
        EntitySlotValidation.validateByte(tradeItem, "Trade item");
        EntitySlotValidation.validateByte(instrument3Flags, "Instrument 3 flags");
        EntitySlotValidation.validateByte(walrusStatus, "Walrus status");
        runtime.houseTarinFlag = tarinFlag;
        runtime.houseTradeItem = tradeItem;
        runtime.houseInstrument3Flags = instrument3Flags;
        runtime.houseWalrusStatus = walrusStatus;
    }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        EntitySlotValidation.validateByte(shieldLevel, "Shield level");
        EntitySlotValidation.validateByte(swordLevel, "Sword level");
        EntitySlotValidation.validateByte(powerBraceletLevel, "Power Bracelet level");
        runtime.chestShieldLevel = shieldLevel;
        runtime.chestSwordLevel = swordLevel;
        runtime.chestPowerBraceletLevel = powerBraceletLevel;
        runtime.chestPlayerLevelsKnown = true;
        if (runtime.spriteHandlers != null) {
            EntitySpriteDefinition pickupDefinition = runtime.spriteHandlers.forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < runtime.slots.length; index++) {
                if (runtime.slots[index].type() == runtime.ENTITY_SWORD_SHIELD_PICKUP) {
                    runtime.slots[index] = withDefinition(
                            runtime.slots[index], pickupDefinition, runtime.slots[index].spriteVariant());
                }
            }
        }
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        EntitySlotValidation.validateByte(magicPowderCount, "Magic Powder count");
        runtime.playerHasToadstool = hasToadstool;
        runtime.playerMagicPowderCount = magicPowderCount;
    }

    public void setTailKeyOwned(boolean owned) {
        runtime.playerHasTailKey = owned;
    }

    public void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        runtime.followingNpcState = state;
        if (state.bowWowFollowing()) runtime.bowWowState = 1;
    }

    public void setColorShellWorld(ColorShellWorld world) {
        runtime.colorShellWorld = world == null ? ColorShellWorld.none() : world;
    }

    public void setSwitchBlockAnimationActive(boolean active) {
        runtime.switchBlockAnimationActive = active;
    }

    public void setOwlStatueDialogId(int globalDialogId) {
        runtime.owlStatueDialogId = globalDialogId;
    }

    public void setOwlDialogResolver(IntUnaryOperator resolver) {
        runtime.owlDialogResolver = Objects.requireNonNull(resolver, "Owl dialog resolver");
    }

    public void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        EntitySlotValidation.validateByte(instrument1Flags, "Instrument 1 flags");
        EntitySlotValidation.validateByte(instrument2Flags, "Instrument 2 flags");
        runtime.instrument1Flags = instrument1Flags;
        runtime.instrument2Flags = instrument2Flags;
    }

    public void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        runtime.owlDefaultMusicResolver = Objects.requireNonNull(resolver, "Owl default music resolver");
    }

    public void setOcarinaPlayback(int countdown, int songFlags, int selectedSong) {
        setOcarinaPlayback(countdown, songFlags, selectedSong, 0, 0);
    }

    public void setOcarinaPlayback(
            int countdown, int songFlags, int selectedSong, int animationCounter, int animationPhase) {
        EntitySlotValidation.validateByte(countdown, "Ocarina playback countdown");
        EntitySlotValidation.validateByte(songFlags, "Ocarina song flags");
        EntitySlotValidation.validateByte(selectedSong, "Selected Ocarina song");
        EntitySlotValidation.validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        runtime.linkState.linkPlayingOcarinaCountdown = countdown;
        runtime.linkState.ocarinaSongFlags = songFlags;
        runtime.linkState.selectedSongIndex = selectedSong;
        runtime.linkState.ocarinaAnimationCounter = animationCounter;
        runtime.linkState.ocarinaAnimationPhase = animationPhase;
    }

    public void setThrownDirection(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.thrownDirection[slot] = value;
    }

    public void setLedgeTransitionTimer(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.ledgeTransitionTimer[slot] = value;
    }

    public void setGoldenLeavesCount(int value) {
        EntitySlotValidation.validateByte(value, "Golden leaves count");
        runtime.entityGoldenLeavesCount = value;
    }

    public void setBooBuddyTriggerCount(int value) {
        EntitySlotValidation.validateByte(value, "Boo Buddy trigger count");
        runtime.booBuddyTriggerCount = value;
    }

    public void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) {
        EntitySlotValidation.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        EntitySlotValidation.validateByte(paletteDataFlags, "Palette data flags");
        EntitySlotValidation.validateByte(defaultMusicTrack, "Default music track");
        runtime.bgPaletteEffectAddress = bgPaletteEffectAddress;
        runtime.paletteDataFlags = paletteDataFlags;
        runtime.defaultMusicTrack = defaultMusicTrack;
    }
}
