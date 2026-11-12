package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.motion.*;

/** Room entity configuration preserving source-shaped handler inputs. */
public final class EntityRuntimeConfiguration {
    private final EntityRuntimeConfigurationServices runtime;

    EntityRuntimeConfiguration(EntityRuntimeConfigurationServices runtime) {
        this.runtime = runtime;
    }

    public void setBoomerangSoundCounterForFrame(int counter) {
        if (counter < 0 || counter >= EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        runtime.projectiles().writeBoomerangSfxCounter(counter);
        runtime.projectiles().writeBoomerangSoundClockProvided(true);
    }

    public void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        EntitySlotValidation.validateByte(targetAddress, "Torch palette target");
        runtime.projectiles().writeTorchPaletteTargetAddress(targetAddress);
        runtime.projectiles().writeTorchPaletteTransitionActive(active);
    }

    public void setLinkVelocityZ(int velocity) {
        runtime.linkState().currentLinkVelocityZ = velocity & 0xFF;
    }

    public void setSpriteSelection(EntitySpriteSelection selection) {
        runtime.presentationState().writeSpriteSelection(selection);
    }

    public void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        runtime.physicsTables()
                .writeGroundInteraction(
                        groundInteraction == null
                                ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                                        RoomEntityGroundInteraction.Result.unchanged(entity, 0)
                                : groundInteraction);
    }

    public void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        runtime.physicsTables().writeBackgroundInteraction(backgroundInteraction);
    }

    public void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        runtime.physicsTables().writeObjectQuery(objectQuery);
    }

    public void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        runtime.physicsTables().writeObjectIntersectionQuery(objectIntersectionQuery);
    }

    public void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    public void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        runtime.linkState().actionButtonAHeld = actionButtonAHeld;
        runtime.linkState().actionButtonBHeld = actionButtonBHeld;
        runtime.linkState().actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
    }

    public void setJoypadHeld(boolean joypadHeld) {
        runtime.linkState().joypadHeld = joypadHeld;
    }

    public void setPressedButtonsMask(int pressedButtonsMask) {
        EntitySlotValidation.validateByte(pressedButtonsMask, "Pressed buttons mask");
        runtime.linkState().linkPressedButtonsMask = pressedButtonsMask;
    }

    public void setDialogActive(boolean dialogActive) {
        runtime.linkState().dialogActive = dialogActive;
    }

    public void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        EntitySlotValidation.validateByte(dialogCooldown, "Dialog cooldown");
        EntitySlotValidation.validateByte(windowY, "Window Y");
        runtime.linkState().inventoryAppearing = inventoryAppearing;
        runtime.linkState().dialogCooldown = dialogCooldown;
        runtime.linkState().windowY = windowY;
    }

    public void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        runtime.linkState().dialogAskSelectionIndex = selectionIndex;
    }

    public void setActiveMusic(boolean activeMusic) {
        runtime.presentationState().writeActiveMusic(activeMusic);
    }

    public void setBowWowState(int bowWowState) {
        runtime.motions().get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(bowWowState & 0xFF);
    }

    public void setLikeLikeLinkInventory(int itemA, int itemB) {
        EntitySlotValidation.validateByte(itemA, "Link A inventory slot");
        EntitySlotValidation.validateByte(itemB, "Link B inventory slot");
        runtime.linkState().linkItemA = itemA;
        runtime.linkState().linkItemB = itemB;
        runtime.linkState().linkInventoryConfigured = true;
    }

    public void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        runtime.linkState().powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    public void setBombButtonHeld(boolean bombButtonHeld) {
        runtime.linkState().bombButtonHeld = bombButtonHeld;
    }

    public void setLinkAttackStepAnimationCountdown(int countdown) {
        EntitySlotValidation.validateByte(countdown, "Link attack-step animation countdown");
        runtime.linkState().linkAttackStepAnimationCountdown = countdown;
    }

    public void setLinkFinalPositionX(int positionX) {
        EntitySlotValidation.validateByte(positionX, "Link final position X");
        runtime.linkState().currentLinkFinalPositionX = positionX;
        runtime.linkState().currentLinkFinalPositionXConfigured = true;
    }

    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        runtime.linkState().fastLiftTransition = redTunic || pieceOfPower;
    }

    public void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: " + linkC13B);
        }
        runtime.liftThrow().writeLiftedLinkC13B(linkC13B);
    }

    public void setGroundInteractionSideScrolling(boolean sideScrolling) {
        runtime.physicsTables().writeGroundInteractionSideScrolling(sideScrolling);
    }

    public void setEnemyDropResolver(EnemyDropResolver resolver) {
        runtime.bossRewards().writeEnemyDropResolver(resolver);
    }

    public void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        runtime.bossRewards().writeEnemyDropCounters(Objects.requireNonNull(counters, "Drop counters cannot be null"));
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        EntitySlotValidation.validateEnemyDropByte(maxHearts, "Maximum hearts");
        EntitySlotValidation.validateEnemyDropByte(health, "Health");
        runtime.bossRewards().writeEnemyDropMaxHearts(maxHearts);
        runtime.bossRewards().writeEnemyDropHealth(health);
        runtime.bossRewards().writeEnemyDropLowHealth(null);
        runtime.bossRewards().writeEnemyDropActivePowerUp(activePowerUp);
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        runtime.bossRewards().writeEnemyDropLowHealth(lowHealth);
    }

    public void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: " + mapId);
        }
        runtime.roomVariables().writeEntityMapId(mapId);
    }

    public void setEntityRoomId(int roomId) {
        EntitySlotValidation.validateByte(roomId, "Entity room id");
        runtime.roomVariables().writeEntityRoomId(roomId);
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) continue;
            SecretSeashellMotion seashell =
                    runtime.motions().get(SecretSeashellMotion.class, SecretSeashellMotion::new);
            seashell.ensureInitialized(entity.slot(), roomId);
            boolean hidden = seashell.privateState3(entity.slot()) != 0;
            runtime.physicsTables()
                    .writeEntityOptions1Override(
                            entity.slot(),
                            hidden
                                    ? EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1
                                    : EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1);
        }
    }

    public void setEntityRoomStatus(int roomStatus) {
        EntitySlotValidation.validateByte(roomStatus, "Entity room status");
        runtime.roomVariables().writeEntityRoomStatus(roomStatus);
    }

    public void setDungeonMinibossReady(boolean ready) {
        runtime.roomVariables().writeDungeonMinibossReady(ready);
    }

    public void setScreenShakeCountdown(int countdown) {
        runtime.slimeEye().setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        EntitySlotValidation.validateByte(collisionX, "Pegasus collision X");
        EntitySlotValidation.validateByte(collisionY, "Pegasus collision Y");
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellScreenShakeActive(screenShakeActive);
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionActive(collisionActive);
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionCoordinatesKnown(true);
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionX(collisionX);
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionY(collisionY);
    }

    public void setHouseNpcStoryState(int tarinFlag, int tradeItem, int instrument3Flags, int walrusStatus) {
        EntitySlotValidation.validateByte(tarinFlag, "Tarin flag");
        EntitySlotValidation.validateByte(tradeItem, "Trade item");
        EntitySlotValidation.validateByte(instrument3Flags, "Instrument 3 flags");
        EntitySlotValidation.validateByte(walrusStatus, "Walrus status");
        runtime.story().writeHouseTarinFlag(tarinFlag);
        runtime.story().writeHouseTradeItem(tradeItem);
        runtime.story().writeHouseInstrument3Flags(instrument3Flags);
        runtime.story().writeHouseWalrusStatus(walrusStatus);
    }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        EntitySlotValidation.validateByte(shieldLevel, "Shield level");
        EntitySlotValidation.validateByte(swordLevel, "Sword level");
        EntitySlotValidation.validateByte(powerBraceletLevel, "Power Bracelet level");
        runtime.pickups().writeChestShieldLevel(shieldLevel);
        runtime.pickups().writeChestSwordLevel(swordLevel);
        runtime.pickups().writeChestPowerBraceletLevel(powerBraceletLevel);
        runtime.pickups().writeChestPlayerLevelsKnown(true);
        if (runtime.spriteHandlers() != null) {
            EntitySpriteDefinition pickupDefinition = runtime.spriteViews().forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < runtime.slots().length; index++) {
                if (runtime.slots()[index].type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
                    runtime.slots()[index] = withDefinition(
                            runtime.slots()[index], pickupDefinition, runtime.slots()[index].spriteVariant());
                }
            }
        }
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        EntitySlotValidation.validateByte(magicPowderCount, "Magic Powder count");
        runtime.story().writePlayerHasToadstool(hasToadstool);
        runtime.story().writePlayerMagicPowderCount(magicPowderCount);
    }

    public void setTailKeyOwned(boolean owned) {
        runtime.story().writePlayerHasTailKey(owned);
    }

    public void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        runtime.motions().get(FollowingNpcMotion.class, FollowingNpcMotion::new).writeFollowingNpcState(state);
        if (state.bowWowFollowing())
            runtime.motions().get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(1);
    }

    public void setColorShellWorld(ColorShellWorld world) {
        runtime.motions()
                .get(ColorShellMotion.class, ColorShellMotion::new)
                .writeColorShellWorld(world == null ? ColorShellWorld.none() : world);
    }

    public void setSwitchBlockAnimationActive(boolean active) {
        runtime.roomVariables().writeSwitchBlockAnimationActive(active);
    }

    public void setOwlStatueDialogId(int globalDialogId) {
        runtime.story().owlState().writeOwlStatueDialogId(globalDialogId);
    }

    public void setOwlDialogResolver(IntUnaryOperator resolver) {
        runtime.story().owlState().writeOwlDialogResolver(Objects.requireNonNull(resolver, "Owl dialog resolver"));
    }

    public void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        EntitySlotValidation.validateByte(instrument1Flags, "Instrument 1 flags");
        EntitySlotValidation.validateByte(instrument2Flags, "Instrument 2 flags");
        runtime.roomVariables().writeInstrument1Flags(instrument1Flags);
        runtime.roomVariables().writeInstrument2Flags(instrument2Flags);
    }

    public void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        runtime.story()
                .owlState()
                .writeOwlDefaultMusicResolver(Objects.requireNonNull(resolver, "Owl default music resolver"));
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
        runtime.linkState().linkPlayingOcarinaCountdown = countdown;
        runtime.linkState().ocarinaSongFlags = songFlags;
        runtime.linkState().selectedSongIndex = selectedSong;
        runtime.linkState().ocarinaAnimationCounter = animationCounter;
        runtime.linkState().ocarinaAnimationPhase = animationPhase;
    }

    public void setThrownDirection(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.liftThrow().writeThrownDirection(slot, value);
    }

    public void setLedgeTransitionTimer(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.liftThrow().writeLedgeTransitionTimer(slot, value);
    }

    public void setGoldenLeavesCount(int value) {
        EntitySlotValidation.validateByte(value, "Golden leaves count");
        runtime.roomVariables().writeEntityGoldenLeavesCount(value);
    }

    public void setBooBuddyTriggerCount(int value) {
        EntitySlotValidation.validateByte(value, "Boo Buddy trigger count");
        runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).writeBooBuddyTriggerCount(value);
    }

    public void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) {
        EntitySlotValidation.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        EntitySlotValidation.validateByte(paletteDataFlags, "Palette data flags");
        EntitySlotValidation.validateByte(defaultMusicTrack, "Default music track");
        runtime.presentationState().writeBgPaletteEffectAddress(bgPaletteEffectAddress);
        runtime.presentationState().writePaletteDataFlags(paletteDataFlags);
        runtime.presentationState().writeDefaultMusicTrack(defaultMusicTrack);
    }
}
