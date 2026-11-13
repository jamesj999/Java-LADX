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
    // Absent for ROM-free runtimes; sword pickup presentation then retains its supplied definition.
    private final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    private final EntityBossRewards bossRewards;
    private final EntityLiftThrowController liftThrow;
    private final EntityLinkState linkState;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityPickupController pickups;
    private final EntityPresentationState presentationState;
    private final EntityProjectileController projectiles;
    private final EntityRoomVariables roomVariables;
    private final EntityStoryController story;
    private final RoomEntity[] slots;
    private final SlimeEyeController slimeEye;
    private final EntitySpriteViews spriteViews;

    EntityRuntimeConfiguration(
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBossRewards bossRewards,
            EntityLiftThrowController liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntityPresentationState presentationState,
            EntityProjectileController projectiles,
            EntityRoomVariables roomVariables,
            EntityStoryController story,
            RoomEntity[] slots,
            SlimeEyeController slimeEye,
            EntitySpriteViews spriteViews) {
        this.spriteHandlers = spriteHandlers;
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.slimeEye = java.util.Objects.requireNonNull(slimeEye, "slimeEye");
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
    }

    public void setBoomerangSoundCounterForFrame(int counter) {
        if (counter < 0 || counter >= EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        this.projectiles.state.writeBoomerangSfxCounter(counter);
        this.projectiles.state.writeBoomerangSoundClockProvided(true);
    }

    public void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        EntitySlotValidation.validateByte(targetAddress, "Torch palette target");
        this.projectiles.state.writeTorchPaletteTargetAddress(targetAddress);
        this.projectiles.state.writeTorchPaletteTransitionActive(active);
    }

    public void setLinkVelocityZ(int velocity) {
        this.linkState.currentLinkVelocityZ = velocity & 0xFF;
    }

    public void setSpriteSelection(EntitySpriteSelection selection) {
        this.presentationState.writeSpriteSelection(selection);
    }

    public void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        this.physicsTables.writeGroundInteraction(
                groundInteraction == null
                        ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                                RoomEntityGroundInteraction.Result.unchanged(entity, 0)
                        : groundInteraction);
    }

    public void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        this.physicsTables.writeBackgroundInteraction(backgroundInteraction);
    }

    public void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        this.physicsTables.writeObjectQuery(objectQuery);
    }

    public void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        this.physicsTables.writeObjectIntersectionQuery(objectIntersectionQuery);
    }

    public void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    public void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        this.linkState.actionButtonAHeld = actionButtonAHeld;
        this.linkState.actionButtonBHeld = actionButtonBHeld;
        this.linkState.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
    }

    public void setJoypadHeld(boolean joypadHeld) {
        this.linkState.joypadHeld = joypadHeld;
    }

    public void setPressedButtonsMask(int pressedButtonsMask) {
        EntitySlotValidation.validateByte(pressedButtonsMask, "Pressed buttons mask");
        this.linkState.linkPressedButtonsMask = pressedButtonsMask;
    }

    public void setDialogActive(boolean dialogActive) {
        this.linkState.dialogActive = dialogActive;
    }

    public void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        EntitySlotValidation.validateByte(dialogCooldown, "Dialog cooldown");
        EntitySlotValidation.validateByte(windowY, "Window Y");
        this.linkState.inventoryAppearing = inventoryAppearing;
        this.linkState.dialogCooldown = dialogCooldown;
        this.linkState.windowY = windowY;
    }

    public void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        this.linkState.dialogAskSelectionIndex = selectionIndex;
    }

    public void setActiveMusic(boolean activeMusic) {
        this.presentationState.writeActiveMusic(activeMusic);
    }

    public void setBowWowState(int bowWowState) {
        this.motions.get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(bowWowState & 0xFF);
    }

    public void setLikeLikeLinkInventory(int itemA, int itemB) {
        EntitySlotValidation.validateByte(itemA, "Link A inventory slot");
        EntitySlotValidation.validateByte(itemB, "Link B inventory slot");
        this.linkState.linkItemA = itemA;
        this.linkState.linkItemB = itemB;
        this.linkState.linkInventoryConfigured = true;
    }

    public void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        this.linkState.powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    public void setBombButtonHeld(boolean bombButtonHeld) {
        this.linkState.bombButtonHeld = bombButtonHeld;
    }

    public void setLinkAttackStepAnimationCountdown(int countdown) {
        EntitySlotValidation.validateByte(countdown, "Link attack-step animation countdown");
        this.linkState.linkAttackStepAnimationCountdown = countdown;
    }

    public void setLinkFinalPositionX(int positionX) {
        EntitySlotValidation.validateByte(positionX, "Link final position X");
        this.linkState.currentLinkFinalPositionX = positionX;
        this.linkState.currentLinkFinalPositionXConfigured = true;
    }

    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        this.linkState.fastLiftTransition = redTunic || pieceOfPower;
    }

    public void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: " + linkC13B);
        }
        this.liftThrow.writeLiftedLinkC13B(linkC13B);
    }

    public void setGroundInteractionSideScrolling(boolean sideScrolling) {
        this.physicsTables.writeGroundInteractionSideScrolling(sideScrolling);
    }

    public void setEnemyDropResolver(EnemyDropResolver resolver) {
        this.bossRewards.writeEnemyDropResolver(resolver);
    }

    public void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        this.bossRewards.writeEnemyDropCounters(Objects.requireNonNull(counters, "Drop counters cannot be null"));
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        EntitySlotValidation.validateEnemyDropByte(maxHearts, "Maximum hearts");
        EntitySlotValidation.validateEnemyDropByte(health, "Health");
        this.bossRewards.writeEnemyDropMaxHearts(maxHearts);
        this.bossRewards.writeEnemyDropHealth(health);
        this.bossRewards.writeEnemyDropLowHealth(null);
        this.bossRewards.writeEnemyDropActivePowerUp(activePowerUp);
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        this.bossRewards.writeEnemyDropLowHealth(lowHealth);
    }

    public void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: " + mapId);
        }
        this.roomVariables.writeEntityMapId(mapId);
    }

    public void setEntityRoomId(int roomId) {
        EntitySlotValidation.validateByte(roomId, "Entity room id");
        this.roomVariables.writeEntityRoomId(roomId);
        for (RoomEntity entity : this.slots) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) continue;
            SecretSeashellMotion seashell = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
            seashell.ensureInitialized(entity.slot(), roomId);
            boolean hidden = seashell.privateState3(entity.slot()) != 0;
            this.physicsTables.writeEntityOptions1Override(
                    entity.slot(),
                    hidden
                            ? EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1
                            : EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1);
        }
    }

    public void setEntityRoomStatus(int roomStatus) {
        EntitySlotValidation.validateByte(roomStatus, "Entity room status");
        this.roomVariables.writeEntityRoomStatus(roomStatus);
    }

    public void setDungeonMinibossReady(boolean ready) {
        this.roomVariables.writeDungeonMinibossReady(ready);
    }

    public void setScreenShakeCountdown(int countdown) {
        this.slimeEye.setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        EntitySlotValidation.validateByte(collisionX, "Pegasus collision X");
        EntitySlotValidation.validateByte(collisionY, "Pegasus collision Y");
        this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellScreenShakeActive(screenShakeActive);
        this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionActive(collisionActive);
        this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionCoordinatesKnown(true);
        this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionX(collisionX);
        this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .writeSecretSeashellPegasusCollisionY(collisionY);
    }

    public void setHouseNpcStoryState(int tarinFlag, int tradeItem, int instrument3Flags, int walrusStatus) {
        EntitySlotValidation.validateByte(tarinFlag, "Tarin flag");
        EntitySlotValidation.validateByte(tradeItem, "Trade item");
        EntitySlotValidation.validateByte(instrument3Flags, "Instrument 3 flags");
        EntitySlotValidation.validateByte(walrusStatus, "Walrus status");
        this.story.writeHouseTarinFlag(tarinFlag);
        this.story.writeHouseTradeItem(tradeItem);
        this.story.writeHouseInstrument3Flags(instrument3Flags);
        this.story.writeHouseWalrusStatus(walrusStatus);
    }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        EntitySlotValidation.validateByte(shieldLevel, "Shield level");
        EntitySlotValidation.validateByte(swordLevel, "Sword level");
        EntitySlotValidation.validateByte(powerBraceletLevel, "Power Bracelet level");
        this.pickups.writeChestShieldLevel(shieldLevel);
        this.pickups.writeChestSwordLevel(swordLevel);
        this.pickups.writeChestPowerBraceletLevel(powerBraceletLevel);
        this.pickups.writeChestPlayerLevelsKnown(true);
        if (this.spriteHandlers != null) {
            EntitySpriteDefinition pickupDefinition = this.spriteViews.forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < this.slots.length; index++) {
                if (this.slots[index].type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
                    this.slots[index] =
                            withDefinition(this.slots[index], pickupDefinition, this.slots[index].spriteVariant());
                }
            }
        }
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        EntitySlotValidation.validateByte(magicPowderCount, "Magic Powder count");
        this.story.writePlayerHasToadstool(hasToadstool);
        this.story.writePlayerMagicPowderCount(magicPowderCount);
    }

    public void setTailKeyOwned(boolean owned) {
        this.story.writePlayerHasTailKey(owned);
    }

    public void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        this.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new).writeFollowingNpcState(state);
        if (state.bowWowFollowing())
            this.motions.get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(1);
    }

    public void setColorShellWorld(ColorShellWorld world) {
        this.motions
                .get(ColorShellMotion.class, ColorShellMotion::new)
                .writeColorShellWorld(world == null ? ColorShellWorld.none() : world);
    }

    public void setSwitchBlockAnimationActive(boolean active) {
        this.roomVariables.writeSwitchBlockAnimationActive(active);
    }

    public void setOwlStatueDialogId(int globalDialogId) {
        this.story.owlState().writeOwlStatueDialogId(globalDialogId);
    }

    public void setOwlDialogResolver(IntUnaryOperator resolver) {
        this.story.owlState().writeOwlDialogResolver(Objects.requireNonNull(resolver, "Owl dialog resolver"));
    }

    public void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        EntitySlotValidation.validateByte(instrument1Flags, "Instrument 1 flags");
        EntitySlotValidation.validateByte(instrument2Flags, "Instrument 2 flags");
        this.roomVariables.writeInstrument1Flags(instrument1Flags);
        this.roomVariables.writeInstrument2Flags(instrument2Flags);
    }

    public void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        this.story
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
        this.linkState.linkPlayingOcarinaCountdown = countdown;
        this.linkState.ocarinaSongFlags = songFlags;
        this.linkState.selectedSongIndex = selectedSong;
        this.linkState.ocarinaAnimationCounter = animationCounter;
        this.linkState.ocarinaAnimationPhase = animationPhase;
    }

    public void setThrownDirection(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        this.liftThrow.writeThrownDirection(slot, value);
    }

    public void setLedgeTransitionTimer(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        this.liftThrow.writeLedgeTransitionTimer(slot, value);
    }

    public void setGoldenLeavesCount(int value) {
        EntitySlotValidation.validateByte(value, "Golden leaves count");
        this.roomVariables.writeEntityGoldenLeavesCount(value);
    }

    public void setBooBuddyTriggerCount(int value) {
        EntitySlotValidation.validateByte(value, "Boo Buddy trigger count");
        this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).writeBooBuddyTriggerCount(value);
    }

    public void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) {
        EntitySlotValidation.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        EntitySlotValidation.validateByte(paletteDataFlags, "Palette data flags");
        EntitySlotValidation.validateByte(defaultMusicTrack, "Default music track");
        this.presentationState.writeBgPaletteEffectAddress(bgPaletteEffectAddress);
        this.presentationState.writePaletteDataFlags(paletteDataFlags);
        this.presentationState.writeDefaultMusicTrack(defaultMusicTrack);
    }
}
