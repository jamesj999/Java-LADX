package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

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
        if (counter < 0 || counter >= EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        runtime.entityState.boomerangSfxCounter = counter;
        runtime.entityState.boomerangSoundClockProvided = true;
    }

    public void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        EntitySlotValidation.validateByte(targetAddress, "Torch palette target");
        runtime.entityState.torchPaletteTargetAddress = targetAddress;
        runtime.entityState.torchPaletteTransitionActive = active;
    }

    public void setLinkVelocityZ(int velocity) {
        runtime.entityState.linkState.currentLinkVelocityZ = velocity & 0xFF;
    }

    public void setSpriteSelection(EntitySpriteSelection selection) {
        runtime.entityState.spriteSelection = selection;
    }

    public void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        runtime.entityState.groundInteraction = groundInteraction == null
                ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.unchanged(entity, 0)
                : groundInteraction;
    }

    public void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        runtime.entityState.backgroundInteraction = backgroundInteraction;
    }

    public void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        runtime.entityState.objectQuery = objectQuery;
    }

    public void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        runtime.entityState.objectIntersectionQuery = objectIntersectionQuery;
    }

    public void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    public void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        runtime.entityState.linkState.actionButtonAHeld = actionButtonAHeld;
        runtime.entityState.linkState.actionButtonBHeld = actionButtonBHeld;
        runtime.entityState.linkState.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
    }

    public void setJoypadHeld(boolean joypadHeld) {
        runtime.entityState.linkState.joypadHeld = joypadHeld;
    }

    public void setPressedButtonsMask(int pressedButtonsMask) {
        EntitySlotValidation.validateByte(pressedButtonsMask, "Pressed buttons mask");
        runtime.entityState.linkState.linkPressedButtonsMask = pressedButtonsMask;
    }

    public void setDialogActive(boolean dialogActive) {
        runtime.entityState.linkState.dialogActive = dialogActive;
    }

    public void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        EntitySlotValidation.validateByte(dialogCooldown, "Dialog cooldown");
        EntitySlotValidation.validateByte(windowY, "Window Y");
        runtime.entityState.linkState.inventoryAppearing = inventoryAppearing;
        runtime.entityState.linkState.dialogCooldown = dialogCooldown;
        runtime.entityState.linkState.windowY = windowY;
    }

    public void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        runtime.entityState.linkState.dialogAskSelectionIndex = selectionIndex;
    }

    public void setActiveMusic(boolean activeMusic) {
        runtime.entityState.activeMusic = activeMusic;
    }

    public void setBowWowState(int bowWowState) {
        runtime.entityState.bowWowState = bowWowState & 0xFF;
    }

    public void setLikeLikeLinkInventory(int itemA, int itemB) {
        EntitySlotValidation.validateByte(itemA, "Link A inventory slot");
        EntitySlotValidation.validateByte(itemB, "Link B inventory slot");
        runtime.entityState.linkState.linkItemA = itemA;
        runtime.entityState.linkState.linkItemB = itemB;
        runtime.entityState.linkState.linkInventoryConfigured = true;
    }

    public void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        runtime.entityState.linkState.powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    public void setBombButtonHeld(boolean bombButtonHeld) {
        runtime.entityState.linkState.bombButtonHeld = bombButtonHeld;
    }

    public void setLinkAttackStepAnimationCountdown(int countdown) {
        EntitySlotValidation.validateByte(countdown, "Link attack-step animation countdown");
        runtime.entityState.linkState.linkAttackStepAnimationCountdown = countdown;
    }

    public void setLinkFinalPositionX(int positionX) {
        EntitySlotValidation.validateByte(positionX, "Link final position X");
        runtime.entityState.linkState.currentLinkFinalPositionX = positionX;
        runtime.entityState.linkState.currentLinkFinalPositionXConfigured = true;
    }

    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        runtime.entityState.linkState.fastLiftTransition = redTunic || pieceOfPower;
    }

    public void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: " + linkC13B);
        }
        runtime.entityState.liftedLinkC13B = linkC13B;
    }

    public void setGroundInteractionSideScrolling(boolean sideScrolling) {
        runtime.entityState.groundInteractionSideScrolling = sideScrolling;
    }

    public void setEnemyDropResolver(EnemyDropResolver resolver) {
        runtime.entityState.enemyDropResolver = resolver;
    }

    public void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        runtime.entityState.enemyDropCounters = Objects.requireNonNull(counters, "Drop counters cannot be null");
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        EntitySlotValidation.validateEnemyDropByte(maxHearts, "Maximum hearts");
        EntitySlotValidation.validateEnemyDropByte(health, "Health");
        runtime.entityState.enemyDropMaxHearts = maxHearts;
        runtime.entityState.enemyDropHealth = health;
        runtime.entityState.enemyDropLowHealth = null;
        runtime.entityState.enemyDropActivePowerUp = activePowerUp;
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        runtime.entityState.enemyDropLowHealth = lowHealth;
    }

    public void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: " + mapId);
        }
        runtime.entityState.entityMapId = mapId;
    }

    public void setEntityRoomId(int roomId) {
        EntitySlotValidation.validateByte(roomId, "Entity room id");
        runtime.entityState.entityRoomId = roomId;
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.entityState.secretSeashellMotion.ensureInitialized(entity.slot(), roomId);
                runtime.entityState.entityOptions1Override[entity.slot()] =
                        runtime.entityState.secretSeashellMotion.privateState3(entity.slot()) != 0
                                ? EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1
                                : EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1;
            }
        }
    }

    public void setEntityRoomStatus(int roomStatus) {
        EntitySlotValidation.validateByte(roomStatus, "Entity room status");
        runtime.entityState.entityRoomStatus = roomStatus;
    }

    public void setDungeonMinibossReady(boolean ready) {
        runtime.entityState.dungeonMinibossReady = ready;
    }

    public void setScreenShakeCountdown(int countdown) {
        runtime.slimeEye.setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        EntitySlotValidation.validateByte(collisionX, "Pegasus collision X");
        EntitySlotValidation.validateByte(collisionY, "Pegasus collision Y");
        runtime.entityState.secretSeashellScreenShakeActive = screenShakeActive;
        runtime.entityState.secretSeashellPegasusCollisionActive = collisionActive;
        runtime.entityState.secretSeashellPegasusCollisionCoordinatesKnown = true;
        runtime.entityState.secretSeashellPegasusCollisionX = collisionX;
        runtime.entityState.secretSeashellPegasusCollisionY = collisionY;
    }

    public void setHouseNpcStoryState(int tarinFlag, int tradeItem, int instrument3Flags, int walrusStatus) {
        EntitySlotValidation.validateByte(tarinFlag, "Tarin flag");
        EntitySlotValidation.validateByte(tradeItem, "Trade item");
        EntitySlotValidation.validateByte(instrument3Flags, "Instrument 3 flags");
        EntitySlotValidation.validateByte(walrusStatus, "Walrus status");
        runtime.entityState.houseTarinFlag = tarinFlag;
        runtime.entityState.houseTradeItem = tradeItem;
        runtime.entityState.houseInstrument3Flags = instrument3Flags;
        runtime.entityState.houseWalrusStatus = walrusStatus;
    }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        EntitySlotValidation.validateByte(shieldLevel, "Shield level");
        EntitySlotValidation.validateByte(swordLevel, "Sword level");
        EntitySlotValidation.validateByte(powerBraceletLevel, "Power Bracelet level");
        runtime.entityState.chestShieldLevel = shieldLevel;
        runtime.entityState.chestSwordLevel = swordLevel;
        runtime.entityState.chestPowerBraceletLevel = powerBraceletLevel;
        runtime.entityState.chestPlayerLevelsKnown = true;
        if (runtime.spriteHandlers != null) {
            EntitySpriteDefinition pickupDefinition = runtime.spriteHandlers.forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < runtime.slots.length; index++) {
                if (runtime.slots[index].type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
                    runtime.slots[index] = withDefinition(
                            runtime.slots[index], pickupDefinition, runtime.slots[index].spriteVariant());
                }
            }
        }
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        EntitySlotValidation.validateByte(magicPowderCount, "Magic Powder count");
        runtime.entityState.playerHasToadstool = hasToadstool;
        runtime.entityState.playerMagicPowderCount = magicPowderCount;
    }

    public void setTailKeyOwned(boolean owned) {
        runtime.entityState.playerHasTailKey = owned;
    }

    public void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        runtime.entityState.followingNpcState = state;
        if (state.bowWowFollowing()) runtime.entityState.bowWowState = 1;
    }

    public void setColorShellWorld(ColorShellWorld world) {
        runtime.entityState.colorShellWorld = world == null ? ColorShellWorld.none() : world;
    }

    public void setSwitchBlockAnimationActive(boolean active) {
        runtime.entityState.switchBlockAnimationActive = active;
    }

    public void setOwlStatueDialogId(int globalDialogId) {
        runtime.entityState.owlStatueDialogId = globalDialogId;
    }

    public void setOwlDialogResolver(IntUnaryOperator resolver) {
        runtime.entityState.owlDialogResolver = Objects.requireNonNull(resolver, "Owl dialog resolver");
    }

    public void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        EntitySlotValidation.validateByte(instrument1Flags, "Instrument 1 flags");
        EntitySlotValidation.validateByte(instrument2Flags, "Instrument 2 flags");
        runtime.entityState.instrument1Flags = instrument1Flags;
        runtime.entityState.instrument2Flags = instrument2Flags;
    }

    public void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        runtime.entityState.owlDefaultMusicResolver = Objects.requireNonNull(resolver, "Owl default music resolver");
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
        runtime.entityState.linkState.linkPlayingOcarinaCountdown = countdown;
        runtime.entityState.linkState.ocarinaSongFlags = songFlags;
        runtime.entityState.linkState.selectedSongIndex = selectedSong;
        runtime.entityState.linkState.ocarinaAnimationCounter = animationCounter;
        runtime.entityState.linkState.ocarinaAnimationPhase = animationPhase;
    }

    public void setThrownDirection(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.thrownDirection[slot] = value;
    }

    public void setLedgeTransitionTimer(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.ledgeTransitionTimer[slot] = value;
    }

    public void setGoldenLeavesCount(int value) {
        EntitySlotValidation.validateByte(value, "Golden leaves count");
        runtime.entityState.entityGoldenLeavesCount = value;
    }

    public void setBooBuddyTriggerCount(int value) {
        EntitySlotValidation.validateByte(value, "Boo Buddy trigger count");
        runtime.entityState.booBuddyTriggerCount = value;
    }

    public void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) {
        EntitySlotValidation.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        EntitySlotValidation.validateByte(paletteDataFlags, "Palette data flags");
        EntitySlotValidation.validateByte(defaultMusicTrack, "Default music track");
        runtime.entityState.bgPaletteEffectAddress = bgPaletteEffectAddress;
        runtime.entityState.paletteDataFlags = paletteDataFlags;
        runtime.entityState.defaultMusicTrack = defaultMusicTrack;
    }
}
